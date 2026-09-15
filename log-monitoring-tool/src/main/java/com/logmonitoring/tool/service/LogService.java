package com.logmonitoring.tool.service;

import com.jcraft.jsch.*;
import com.logmonitoring.tool.dto.LogStatsDto;
import com.logmonitoring.tool.model.ServerEnvironment;
import com.logmonitoring.tool.repository.ServerEnvironmentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Executor;
import org.springframework.cache.annotation.Cacheable;

@Service
public class LogService {

    private final ServerEnvironmentRepository environmentRepository;
    private final Executor logStreamExecutor;

    public LogService(ServerEnvironmentRepository environmentRepository, 
                      @Qualifier("logStreamExecutor") Executor logStreamExecutor) {
        this.environmentRepository = environmentRepository;
        this.logStreamExecutor = logStreamExecutor;
    }

    public String fetchTailLogs(Long envId, int lines) {
        ServerEnvironment env = environmentRepository.findById(envId).orElse(null);
        if (env == null) {
            return "[HATA] Sunucu tanımı bulunamadı (ID: " + envId + ")";
        }

        String command = "tail -n " + lines + " " + env.getLogFilePath();
        return executeSshCommand(env, command);
    }

    @Cacheable(value = "sftpFilesCache", key = "{#envId, #extensionFilter}")
    public List<String> listFilesInDirectory(Long envId, String extensionFilter) {
        List<String> fileList = new ArrayList<>();
        ServerEnvironment env = environmentRepository.findById(envId).orElse(null);
        if (env == null || env.getLogDirectoryPath() == null || env.getLogDirectoryPath().isBlank()) {
            return fileList;
        }

        Session session = null;
        ChannelSftp channelSftp = null;
        try {
            session = createSshSession(env);
            session.connect(10000);

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect(10000);

            Vector<ChannelSftp.LsEntry> entries = channelSftp.ls(env.getLogDirectoryPath());
            for (ChannelSftp.LsEntry entry : entries) {
                String filename = entry.getFilename();
                if (!filename.equals(".") && !filename.equals("..") && !entry.getAttrs().isDir()) {
                    if (extensionFilter == null || extensionFilter.equalsIgnoreCase("ALL") || filename.endsWith(extensionFilter)) {
                        fileList.add(filename);
                    }
                }
            }
        } catch (Exception e) {
            fileList.add("[SFTP HATA] " + e.getMessage());
        } finally {
            if (channelSftp != null && channelSftp.isConnected()) channelSftp.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
        return fileList;
    }

    @Cacheable(value = "sftpFileContentCache", key = "{#envId, #fileName}")
    public String fetchFileContent(Long envId, String fileName) {
        ServerEnvironment env = environmentRepository.findById(envId).orElse(null);
        if (env == null) {
            return "[HATA] Sunucu tanımı bulunamadı.";
        }

        String targetPath = env.getLogDirectoryPath() + "/" + fileName;
        String command = "tail -n 300 " + targetPath;
        return executeSshCommand(env, command);
    }
    

    private String sanitizeInput(String input) {
    if (input == null) return "";
    
    return input.replaceAll("[;&|`$><!]", "").trim();
}
   public String searchLogsWithGrep(Long envId, String fileName, String level, String keyword, 
                                     String sessionId, String msisdn, String startTime, String endTime, int lineLimit) {
        // Girdileri temizle (Command Injection Koruması)
        String safeFileName = sanitizeInput(fileName);
        String safeLevel = sanitizeInput(level);
        String safeKeyword = sanitizeInput(keyword);
        String safeSessionId = sanitizeInput(sessionId);
        String safeMsisdn = sanitizeInput(msisdn);
        String safeStartTime = sanitizeInput(startTime);
        String safeEndTime = sanitizeInput(endTime);

        ServerEnvironment env = environmentRepository.findById(envId).orElse(null);
        if (env == null) {
            return "[HATA] Sunucu tanımı bulunamadı.";
        }

        String searchPath;
        if (!safeFileName.isBlank() && !safeFileName.equalsIgnoreCase("ALL")) {
            searchPath = env.getLogDirectoryPath() + "/" + safeFileName;
        } else {
            searchPath = env.getLogDirectoryPath() + "/*.{out,log}";
        }

        StringBuilder cmd = new StringBuilder();

        // 1. Aşama: Zaman Aralığı Filtresi (awk ile alfabetik/tarih karşılaştırması)
        if (!safeStartTime.isBlank() && !safeEndTime.isBlank()) {
            String s = safeStartTime.replace("T", " ");
            String e = safeEndTime.replace("T", " ");
            cmd.append(String.format("awk -v s=\"%s\" -v e=\"%s\" '($1\" \"$2 >= s && $1\" \"$2 <= e)' %s", s, e, searchPath));
        } else if (!safeStartTime.isBlank()) {
            String s = safeStartTime.replace("T", " ");
            cmd.append(String.format("awk -v s=\"%s\" '($1\" \"$2 >= s)' %s", s, searchPath));
        } else {
            cmd.append(String.format("cat %s", searchPath));
        }

        // 2. Aşama: Seviye Filtresi (ERROR / WARN / INFO)
        if (!safeLevel.isBlank()) {
            cmd.append(String.format(" | grep -iE \"\\b%s\\b\"", safeLevel));
        }

        // 3. Aşama: Kurumsal Parametreler (SessionID, MSISDN)
        if (!safeSessionId.isBlank()) {
            cmd.append(String.format(" | grep -F \"%s\"", safeSessionId));
        }
        if (!safeMsisdn.isBlank()) {
            cmd.append(String.format(" | grep -F \"%s\"", safeMsisdn));
        }

        // 4. Aşama: Serbest Kelime Araması (AND mantığıyla piped)
        if (!safeKeyword.isBlank()) {
            for (String term : safeKeyword.split("\\s+")) {
                if (!term.isBlank()) {
                    cmd.append(String.format(" | grep -iF \"%s\"", term));
                }
            }
        }

        // 5. Aşama: Güvenlik, Hata Bastırma ve Limit
        cmd.append(String.format(" 2>/dev/null | tail -n %d", lineLimit > 0 ? lineLimit : 200));

        return executeSshCommand(env, cmd.toString());
    }

    public LogStatsDto analyzeLogStats(Long envId, int lines) {
        String rawLogs = fetchTailLogs(envId, lines);
        if (rawLogs == null || rawLogs.isBlank() || rawLogs.startsWith("[HATA]")) {
            return new LogStatsDto(0, 0, 0, 0, List.of("Log verisi alınamadı veya sunucu erişilemez."));
        }

        String[] logLines = rawLogs.split("\n");
        long errors = 0;
        long warns = 0;
        long infos = 0;
        List<String> recentErrors = new ArrayList<>();

        for (String line : logLines) {
            String upper = line.toUpperCase();
            if (upper.contains("ERROR") || upper.contains("EXCEPTION") || upper.contains("FATAL") || upper.contains("[HATA]")) {
                errors++;
                if (recentErrors.size() < 5) {
                    recentErrors.add(line.trim());
                }
            } else if (upper.contains("WARN")) {
                warns++;
            } else if (upper.contains("INFO")) {
                infos++;
            }
        }

        return new LogStatsDto(logLines.length, errors, warns, infos, recentErrors);
    }

    public boolean checkServerHealth(Long envId) {
        ServerEnvironment env = environmentRepository.findById(envId).orElse(null);
        if (env == null || env.getHost() == null) {
            return false;
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(env.getHost(), env.getPort()), 3000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public SseEmitter streamLiveLogs(Long envId) {
        SseEmitter emitter = new SseEmitter(1800000L); // 30 dakikalık canlı yayın kanalı
        ServerEnvironment env = environmentRepository.findById(envId).orElse(null);

        if (env == null) {
            try {
                emitter.send(SseEmitter.event().name("error").data("[HATA] Sunucu tanımı bulunamadı (ID: " + envId + ")"));
                emitter.complete();
            } catch (IOException ignored) {}
            return emitter;
        }

        logStreamExecutor.execute(() -> {
            Session session = null;
            ChannelExec channel = null;
            BufferedReader reader = null;
            try {
                session = createSshSession(env);
                session.connect(8000);

                channel = (ChannelExec) session.openChannel("exec");
                channel.setCommand("tail -f -n 25 " + env.getLogFilePath());
                channel.setInputStream(null);
                
                InputStream in = channel.getInputStream();
                channel.connect(8000);

                reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line;

                while ((line = reader.readLine()) != null) {
                    try {
                        emitter.send(SseEmitter.event().name("log").data(line));
                    } catch (Exception clientDisconnected) {
                        break; 
                    }
                }
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("[BAĞLANTI HATASI]: " + e.getMessage()));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            } finally {
                try {
                    if (reader != null) reader.close();
                    if (channel != null && channel.isConnected()) channel.disconnect();
                    if (session != null && session.isConnected()) session.disconnect();
                } catch (Exception ignored) {}
            }
        });

        emitter.onCompletion(() -> {});
        emitter.onTimeout(emitter::complete);
        emitter.onError((e) -> emitter.complete());

        return emitter;
    }

    private String executeSshCommand(ServerEnvironment env, String command) {
        Session session = null;
        ChannelExec channel = null;
        StringBuilder output = new StringBuilder();

        try {
            session = createSshSession(env);
            session.connect(10000);

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);
            channel.setErrStream(System.err);

            InputStream in = channel.getInputStream();
            channel.connect(10000);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

        } catch (Exception e) {
            return "[HATA - SSH Bağlantı / Komut Hatası]: " + e.getMessage();
        } finally {
            if (channel != null && channel.isConnected()) channel.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }

        return output.length() > 0 ? output.toString() : "[Bilgi]: Eşleşen herhangi bir kayıt veya log çıktısı bulunamadı.";
    }

    private Session createSshSession(ServerEnvironment env) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(env.getUsername(), env.getHost(), env.getPort());
        session.setPassword(env.getPassword());
        session.setConfig("StrictHostKeyChecking", "no");
        return session;
    }
   // 1. Context grep komutunu üreten yardımcı metot
    public String buildContextSearchCommand(String logFilePath, String searchTerm, int contextLines) {
        String grepCmd = logFilePath.endsWith(".gz") ? "zgrep" : "grep";
        // -n: Satır numarası, -C: Önceki ve sonraki X satır context, -m 5: İlk 5 eşleşme bloğu
        return String.format("%s -n -C %d -m 5 \"%s\" %s", grepCmd, contextLines, searchTerm, logFilePath);
    }

    // 2. Controller'dan çağrılacak asıl context arama metodu
    public String searchLogsWithContext(Long envId, String fileName, String searchTerm, int contextLines) {
        ServerEnvironment env = environmentRepository.findById(envId).orElse(null);
        if (env == null) {
            return "[HATA] Sunucu tanımı bulunamadı (ID: " + envId + ")";
        }

        String safeSearchTerm = sanitizeInput(searchTerm);
        if (safeSearchTerm.isBlank()) {
            return "[HATA] Aranacak kelime / SessionID / MSISDN boş olamaz.";
        }

        String safeFileName = sanitizeInput(fileName);
        String targetPath;
        if (!safeFileName.isBlank() && !safeFileName.equalsIgnoreCase("ALL")) {
            targetPath = env.getLogDirectoryPath() + "/" + safeFileName;
        } else {
            targetPath = env.getLogFilePath(); // Dosya seçilmediyse varsayılan log dosyası
        }

        String command = buildContextSearchCommand(targetPath, safeSearchTerm, contextLines);
        return executeSshCommand(env, command);
    }
}