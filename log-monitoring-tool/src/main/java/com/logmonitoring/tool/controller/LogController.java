package com.logmonitoring.tool.controller;

import com.logmonitoring.tool.model.ServerEnvironment;
import com.logmonitoring.tool.repository.ServerEnvironmentRepository;
import com.logmonitoring.tool.service.LogReaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LogController {

    private final ServerEnvironmentRepository envRepository;
    private final LogReaderService logReaderService;

    @GetMapping("/environments")
    public List<ServerEnvironment> getEnvironments() {
        return envRepository.findAll();
    }

    @PostMapping("/environments")
    public ServerEnvironment addEnvironment(@RequestBody ServerEnvironment env) {
        return envRepository.save(env);
    }

    @GetMapping("/logs")
    public ResponseEntity<String> getLogs(@RequestParam Long envId, @RequestParam(defaultValue = "100") int lines) {
        return envRepository.findById(envId)
                .map(env -> ResponseEntity.ok(logReaderService.readLogFile(env, lines)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search-grep")
    public ResponseEntity<String> searchGrep(
            @RequestParam Long envId,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "200") int lineLimit) {
        return envRepository.findById(envId)
                .map(env -> ResponseEntity.ok(logReaderService.searchLogsWithGrep(env, fileName, level, keyword, lineLimit)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/file-content")
    public ResponseEntity<String> getFileContent(@RequestParam Long envId, @RequestParam String fileName) {
        return envRepository.findById(envId)
                .map(env -> ResponseEntity.ok(logReaderService.readSpecificFile(env, fileName)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/files")
    public ResponseEntity<List<String>> listFiles(
            @RequestParam Long envId,
            @RequestParam(defaultValue = "ALL") String extension) {
        return envRepository.findById(envId)
                .map(env -> ResponseEntity.ok(logReaderService.listFilesInDirectory(env, extension)))
                .orElse(ResponseEntity.notFound().build());
    }
}