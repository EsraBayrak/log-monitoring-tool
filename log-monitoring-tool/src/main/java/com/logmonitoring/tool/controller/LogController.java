package com.logmonitoring.tool.controller;

import com.logmonitoring.tool.dto.LogStatsDto;
import com.logmonitoring.tool.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Log Controller", description = "SSH Log İzleme, Grep Filtreleme ve SSE Akış Uç Noktaları")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @Operation(summary = "Uzak sunucudan son N satır logu çeker (tail -n)")
    @GetMapping("/logs")
    public String getTailLogs(@RequestParam Long envId, @RequestParam(defaultValue = "100") int lines) {
        return logService.fetchTailLogs(envId, lines);
    }

    @Operation(summary = "Log dizinindeki dosyaları listeler (SFTP)")
    @GetMapping("/files")
    public List<String> listFiles(@RequestParam Long envId, @RequestParam(defaultValue = "ALL") String extension) {
        return logService.listFilesInDirectory(envId, extension);
    }

    @Operation(summary = "Seçilen spesifik bir log dosyasını okur (SFTP/Tail)")
    @GetMapping("/file-content")
    public String getFileContent(@RequestParam Long envId, @RequestParam String fileName) {
        return logService.fetchFileContent(envId, fileName);
    }

    @Operation(summary = "Linux Grep ile sunucu üzerinde doğrudan filtreleme ve arama yapar")
    @GetMapping("/search-grep")
    public String searchGrep(
            @RequestParam Long envId,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "100") int lineLimit) {
        return logService.searchLogsWithGrep(envId, fileName, level, keyword, lineLimit);
    }

    @Operation(summary = "Logları analiz ederek Hata/Uyarı/Bilgi metriklerini döner")
    @GetMapping("/stats")
    public LogStatsDto getLogStats(@RequestParam Long envId, @RequestParam(defaultValue = "200") int lines) {
        return logService.analyzeLogStats(envId, lines);
    }

    @Operation(summary = "Sunucunun SSH portuna erişim durumunu test eder (Health Check)")
    @GetMapping("/health")
    public boolean checkHealth(@RequestParam Long envId) {
        return logService.checkServerHealth(envId);
    }

    @Operation(summary = "Gerçek zamanlı canlı log akışı sağlar (SSE Stream tail -f)")
    @GetMapping(value = "/logs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs(@RequestParam Long envId) {
        return logService.streamLiveLogs(envId);
    }
}