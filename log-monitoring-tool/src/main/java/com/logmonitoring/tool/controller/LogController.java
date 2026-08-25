package com.logmonitoring.tool.controller;

import com.logmonitoring.tool.dto.LogStatsDto;
import com.logmonitoring.tool.model.ServerEnvironment;
import com.logmonitoring.tool.repository.ServerEnvironmentRepository;
import com.logmonitoring.tool.service.LogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class LogController {

    private final ServerEnvironmentRepository environmentRepository;
    private final LogService logService;

    public LogController(ServerEnvironmentRepository environmentRepository, LogService logService) {
        this.environmentRepository = environmentRepository;
        this.logService = logService;
    }

    @GetMapping("/environments")
    public ResponseEntity<List<ServerEnvironment>> getAllEnvironments() {
        return ResponseEntity.ok(environmentRepository.findAll());
    }

    @PostMapping("/environments")
    public ResponseEntity<ServerEnvironment> addEnvironment(@RequestBody ServerEnvironment environment) {
        return ResponseEntity.ok(environmentRepository.save(environment));
    }

    @GetMapping("/logs")
    public ResponseEntity<String> getTailLogs(
            @RequestParam Long envId,
            @RequestParam(defaultValue = "100") int lines) {
        String result = logService.fetchTailLogs(envId, lines);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/files")
    public ResponseEntity<List<String>> listFiles(
            @RequestParam Long envId,
            @RequestParam(defaultValue = "ALL") String extension) {
        List<String> files = logService.listFilesInDirectory(envId, extension);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/file-content")
    public ResponseEntity<String> getFileContent(
            @RequestParam Long envId,
            @RequestParam String fileName) {
        String content = logService.fetchFileContent(envId, fileName);
        return ResponseEntity.ok(content);
    }

    @GetMapping("/search-grep")
    public ResponseEntity<String> searchGrep(
            @RequestParam Long envId,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "200") int lineLimit) {
        String results = logService.searchLogsWithGrep(envId, fileName, level, keyword, lineLimit);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/stats")
    public ResponseEntity<LogStatsDto> getLogStats(
            @RequestParam Long envId,
            @RequestParam(defaultValue = "200") int lines) {
        LogStatsDto stats = logService.analyzeLogStats(envId, lines);
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Boolean> checkHealth(@RequestParam Long envId) {
        boolean isAlive = logService.checkServerHealth(envId);
        return ResponseEntity.ok(isAlive);
    }
}