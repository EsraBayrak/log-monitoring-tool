package com.logmonitoring.tool.service;

import com.jcraft.jsch.*;
import com.logmonitoring.tool.model.ServerEnvironment;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

@Service
public class LogReaderService {

    // 1. Canlı / Son Logları Oku (tail -n)
    public String readLogFile(ServerEnvironment env, int lineCount) {
        String command = "tail -n " + lineCount + " " + env.getLogFilePath();
        return executeSshCommand(env, command);
    }

    // 2. Sunucu Taraflı Güçlü Grep (Level ve Metin Arama)
    // Örn: grep -inE "ERROR|WARN" /dizin/dosya.out | tail -n 200
    public String searchLogsWithGrep(ServerEnvironment env, String fileName, String level, String keyword, int lineLimit) {
        StringBuilder cmd = new StringBuilder("grep -in");

        String fullPath;
        if (fileName != null && !fileName.trim().isEmpty() && !fileName.equals("ALL")) {
            fullPath = getNormalizedDirPath(env.getLogDirectoryPath()) + fileName;
        } else {
            // Belirtilen dizindeki tüm .log ve .out dosyalarında ara
            fullPath = getNormalizedDirPath(env.getLogDirectoryPath()) + "*.{out,log}";
        }

        // Filtre deseni oluştur
        if (level != null && !level.trim().isEmpty() && keyword != null && !keyword.trim().isEmpty()) {
            // Hem seviye hem kelime varsa
            cmd.append("E \"(").append(level).append(").*(").append(keyword).append(")\" ");
        } else if (level != null && !level.trim().isEmpty()) {
            cmd.append("E \"").append(level).append("\" ");
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            cmd.append(" \"").append(keyword).append("\" ");
        } else {
            // Filtre yoksa sadece tail yap
            return readSpecificFile(env, fileName != null ? fileName : "oim_m1.out");
        }

        cmd.append(fullPath).append(" 2>/dev/null | tail -n ").append(lineLimit > 0 ? lineLimit : 200);

        return executeSshCommand(env, cmd.toString());
    }

    // 3. SFTP ile Dosya İçeriğini Oku
    public String readSpecificFile(ServerEnvironment env, String fileName) {
        Session session = null;
        ChannelSftp sftpChannel = null;
        try {
            session = createSession(env);
            session.connect(10000);

            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect(5000);

            String fullPath = getNormalizedDirPath(env.getLogDirectoryPath()) + fileName;
            InputStream inputStream = sftpChannel.get(fullPath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        } catch (Exception e) {
            return "[HATA] Dosya SFTP ile okunamadı (" + fileName + "): " + e.getMessage();
        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) sftpChannel.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
    }

    // 4. SFTP ile Uzantıya Göre (.out, .log, .xml) Dosyaları Listele
    public List<String> listFilesInDirectory(ServerEnvironment env, String extensionFilter) {
        List<String> fileNames = new ArrayList<>();
        Session session = null;
        ChannelSftp sftpChannel = null;
        try {
            session = createSession(env);
            session.connect(10000);

            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect(5000);

            String dirPath = env.getLogDirectoryPath() != null ? env.getLogDirectoryPath() : "/";
            Vector<ChannelSftp.LsEntry> entries = sftpChannel.ls(dirPath);

            for (ChannelSftp.LsEntry entry : entries) {
                if (!entry.getAttrs().isDir()) {
                    String name = entry.getFilename();
                    if (extensionFilter == null || extensionFilter.equalsIgnoreCase("ALL") 
                            || name.endsWith(extensionFilter) || name.contains(extensionFilter)) {
                        fileNames.add(name);
                    }
                }
            }
        } catch (Exception e) {
            fileNames.add("[Hata] Dosyalar listelenemedi: " + e.getMessage());
        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) sftpChannel.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
        return fileNames;
    }

    private String executeSshCommand(ServerEnvironment env, String command) {
        Session session = null;
        ChannelExec channel = null;
        try {
            session = createSession(env);
            session.connect(10000);

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channel.setOutputStream(outputStream);
            channel.connect(5000);

            while (!channel.isClosed()) {
                Thread.sleep(100);
            }

            String result = outputStream.toString(StandardCharsets.UTF_8);
            return result.isEmpty() ? "Belirtilen kriterlere uygun log kaydı bulunamadı." : result;

        } catch (Exception e) {
            return "[HATA] SSH Komut çalıştırma başarısız (" + command + "): " + e.getMessage();
        } finally {
            if (channel != null && channel.isConnected()) channel.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
    }

    private String getNormalizedDirPath(String path) {
        if (path == null || path.isEmpty()) return "/";
        return path.endsWith("/") ? path : path + "/";
    }

    private Session createSession(ServerEnvironment env) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(env.getUsername(), env.getHost(), env.getPort() != null ? env.getPort() : 22);
        session.setPassword(env.getPassword());

        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        return session;
    }
}