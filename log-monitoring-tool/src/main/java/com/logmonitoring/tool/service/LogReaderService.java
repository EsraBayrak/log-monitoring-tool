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

    // 2. Sunucu Tarafli Guclu Grep (Level, Keyword, SessionID, MSISDN, Zaman Araligi awk Destegi)
    public String searchLogsWithGrep(ServerEnvironment env, String fileName, String level,
                                     String keyword, String sessionId, String msisdn, 
                                     String startTime, String endTime, int lineLimit) {

        String fullPath;
        if (fileName != null && !fileName.trim().isEmpty() && !fileName.equals("ALL")) {
            fullPath = getNormalizedDirPath(env.getLogDirectoryPath()) + fileName;
        } else {
            fullPath = getNormalizedDirPath(env.getLogDirectoryPath()) + "*.{out,log}";
        }

        StringBuilder cmd = new StringBuilder();

        // 1. Asama: Zaman Araligi Filtresi (awk ile alfabetik/tarih karsilastirmasi)
        if (startTime != null && !startTime.isBlank() && endTime != null && !endTime.isBlank()) {
            String s = startTime.replace("T", " ");
            String e = endTime.replace("T", " ");
            cmd.append(String.format("awk -v s=\"%s\" -v e=\"%s\" '($1\" \"$2 >= s && $1\" \"$2 <= e)' %s", s, e, fullPath));
        } else if (startTime != null && !startTime.isBlank()) {
            String s = startTime.replace("T", " ");
            cmd.append(String.format("awk -v s=\"%s\" '($1\" \"$2 >= s)' %s", s, fullPath));
        } else {
            cmd.append(String.format("cat %s", fullPath));
        }

        // 2. Asama: Seviye Filtresi (ERROR / WARN / INFO)
        if (level != null && !level.trim().isEmpty()) {
            cmd.append(String.format(" | grep -iE \"\\b%s\\b\"", level.trim()));
        }

        // 3. Asama: Kurumsal Parametreler (SessionID, MSISDN)
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            cmd.append(String.format(" | grep -F \"%s\"", sessionId.trim()));
        }
        if (msisdn != null && !msisdn.trim().isEmpty()) {
            cmd.append(String.format(" | grep -F \"%s\"", msisdn.trim()));
        }

        // 4. Asama: Serbest Kelime Aramasi (Boru hatti / AND mantigi)
        if (keyword != null && !keyword.trim().isEmpty()) {
            for (String term : keyword.trim().split("\\s+")) {
                cmd.append(String.format(" | grep -iF \"%s\"", term));
            }
        }

        // 5. Asama: Guvenlik & Limit
        cmd.append(String.format(" 2>/dev/null | tail -n %d", lineLimit > 0 ? lineLimit : 200));

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