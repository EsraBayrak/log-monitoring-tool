package com.logmonitoring.tool.controller;

import com.logmonitoring.tool.model.ServerEnvironment;
import com.logmonitoring.tool.repository.ServerEnvironmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
@Tag(name = "Environment Controller", description = "Sunucu ortamları yönetim uç noktaları (CRUD)")
public class EnvironmentController {

    private final ServerEnvironmentRepository environmentRepository;

    public EnvironmentController(ServerEnvironmentRepository environmentRepository) {
        this.environmentRepository = environmentRepository;
    }

    @Operation(summary = "Tüm kayıtlı sunucuları listeler")
    @GetMapping("/environments")
    public List<ServerEnvironment> getAllEnvironments() {
        return environmentRepository.findAll();
    }

    @Operation(summary = "Yeni bir sunucu ortamı tanımlar")
    @PostMapping("/environments")
    public ServerEnvironment createEnvironment(@RequestBody ServerEnvironment environment) {
        return environmentRepository.save(environment);
    }

    @Operation(summary = "Mevcut bir sunucu ortamını günceller")
    @PutMapping("/environments/{id}")
    public ResponseEntity<ServerEnvironment> updateEnvironment(@PathVariable Long id, @RequestBody ServerEnvironment updatedEnv) {
        ServerEnvironment existing = environmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ID: " + id + " olan sunucu bulunamadı."));

        existing.setName(updatedEnv.getName());
        existing.setHost(updatedEnv.getHost());
        existing.setPort(updatedEnv.getPort());
        existing.setUsername(updatedEnv.getUsername());
        if (updatedEnv.getPassword() != null && !updatedEnv.getPassword().isBlank()) {
            existing.setPassword(updatedEnv.getPassword());
        }
        existing.setLogDirectoryPath(updatedEnv.getLogDirectoryPath());
        existing.setLogFilePath(updatedEnv.getLogFilePath());

        ServerEnvironment saved = environmentRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Kayıtlı bir sunucu ortamını siler")
    @DeleteMapping("/environments/{id}")
    public ResponseEntity<Void> deleteEnvironment(@PathVariable Long id) {
        if (!environmentRepository.existsById(id)) {
            throw new NoSuchElementException("ID: " + id + " olan sunucu bulunamadı.");
        }
        environmentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}