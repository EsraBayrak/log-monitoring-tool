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

    // Tüm ortamları listele
    @GetMapping("/environments")
    public List<ServerEnvironment> getEnvironments() {
        return envRepository.findAll();
    }

    // Yeni ortam ekle
    @PostMapping("/environments")
    public ServerEnvironment addEnvironment(@RequestBody ServerEnvironment env) {
        return envRepository.save(env);
    }

    // Log oku
    @GetMapping("/logs")
    public ResponseEntity<String> getLogs(@RequestParam Long envId, @RequestParam(defaultValue = "100") int lines) {
        return envRepository.findById(envId)
                .map(env -> ResponseEntity.ok(logReaderService.readLogFile(env, lines)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Konfigürasyon dosyası oku
    @GetMapping("/config")
    public ResponseEntity<String> getConfig(@RequestParam Long envId) {
        return envRepository.findById(envId)
                .map(env -> ResponseEntity.ok(logReaderService.readConfigFile(env)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Dizin içindeki dosyaları listele
    @GetMapping("/files")
    public ResponseEntity<List<String>> listFiles(@RequestParam Long envId) {
        return envRepository.findById(envId)
                .map(env -> ResponseEntity.ok(logReaderService.listFilesInDirectory(env)))
                .orElse(ResponseEntity.notFound().build());
    }
    // Seçilen dosyayı oku
    @GetMapping("/file-content")
    public ResponseEntity<String> getFileContent(@RequestParam Long envId, @RequestParam String fileName) {
        return envRepository.findById(envId)
                .map(env -> ResponseEntity.ok(logReaderService.readSpecificFile(env, fileName)))
                .orElse(ResponseEntity.notFound().build());
    }
    
}