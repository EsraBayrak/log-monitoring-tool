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

    // SSH ile bağlanıp tail -n komutuyla log okur
    public String readLogFile(ServerEnvironment env, int lineCount) {
        Session session = null;
        ChannelExec channel = null;
        try {
            session = createSession(env);
            session.connect(10000);

            channel = (ChannelExec) session.openChannel("exec");
            String command = "tail -n " + lineCount + " " + env.getLogFilePath();
            channel.setCommand(command);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channel.setOutputStream(outputStream);
            channel.connect(5000);

            while (!channel.isClosed()) {
                Thread.sleep(100);
            }

            String result = outputStream.toString(StandardCharsets.UTF_8);
            return result.isEmpty() ? "Log dosyasında içerik bulunamadı veya dosya boş." : result;

        } catch (Exception e) {
            return "[HATA] SSH bağlantısı veya log okuma başarısız: " + e.getMessage();
        } finally {
            if (channel != null && channel.isConnected()) channel.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
    }

    // SFTP ile konfigürasyon dosyasını metin olarak okur
    public String readConfigFile(ServerEnvironment env) {
        Session session = null;
        ChannelSftp sftpChannel = null;
        try {
            session = createSession(env);
            session.connect(10000);

            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect(5000);

            InputStream inputStream = sftpChannel.get(env.getConfigFilePath());
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            return content.toString();

        } catch (Exception e) {
            return "[HATA] SFTP ile konfigürasyon dosyası okunamadı: " + e.getMessage();
        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) sftpChannel.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
    }

    // SFTP ile dizindeki dosyaları listeler (File Explorer mantığı)
    public List<String> listFilesInDirectory(ServerEnvironment env) {
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
                    fileNames.add(entry.getFilename());
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
    // Seçilen spesifik bir dosyayı SFTP üzerinden okur
    public String readSpecificFile(ServerEnvironment env, String fileName) {
        Session session = null;
        ChannelSftp sftpChannel = null;
        try {
            session = createSession(env);
            session.connect(10000);

            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect(5000);

            String fullPath = env.getLogDirectoryPath();
            if (!fullPath.endsWith("/")) {
                fullPath += "/";
            }
            fullPath += fileName;

            InputStream inputStream = sftpChannel.get(fullPath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            return content.toString();
        } catch (Exception e) {
            return "[HATA] Dosya okunamadı (" + fileName + "): " + e.getMessage();
        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) sftpChannel.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
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