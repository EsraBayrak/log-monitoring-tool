package com.logmonitoring.tool.controller;

import com.logmonitoring.tool.dto.LogStatsDto;
import com.logmonitoring.tool.service.LogService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;


@RestController
@RequestMapping("/api")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping("/logs")
    public String getTailLogs(@RequestParam Long envId, @RequestParam(defaultValue = "100") int lines) {
        return logService.fetchTailLogs(envId, lines);
    }

    @GetMapping("/files")
    public List<String> listFiles(@RequestParam Long envId, @RequestParam(defaultValue = "ALL") String extension) {
        return logService.listFilesInDirectory(envId, extension);
    }

    @GetMapping("/file-content")
    public String getFileContent(@RequestParam Long envId, @RequestParam String fileName) {
        return logService.fetchFileContent(envId, fileName);
    }

    @GetMapping("/search-grep")
    public String searchGrep(
            @RequestParam Long envId,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String msisdn,
            @RequestParam(defaultValue = "100") int lineLimit) {
        return logService.searchLogsWithGrep(envId, fileName, level, keyword, sessionId, msisdn, lineLimit);
    }

    @GetMapping("/stats")
    public LogStatsDto getLogStats(@RequestParam Long envId, @RequestParam(defaultValue = "200") int lines) {
        return logService.analyzeLogStats(envId, lines);
    }

    @GetMapping("/health")
    public boolean checkHealth(@RequestParam Long envId) {
        return logService.checkServerHealth(envId);
    }
   
   // @Hidden
    //@Operation(summary = "Gerçek zamanlı canlı log akışı sağlar (SSE Stream)")
    @GetMapping(value = "/logs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs(@RequestParam Long envId) {
        return logService.streamLiveLogs(envId);
    } 

}