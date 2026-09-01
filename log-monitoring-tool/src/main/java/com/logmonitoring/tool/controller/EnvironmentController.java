package com.logmonitoring.tool.controller;

import com.logmonitoring.tool.dto.ServerEnvironmentRequestDto;
import com.logmonitoring.tool.dto.ServerEnvironmentResponseDto;
import com.logmonitoring.tool.model.ServerEnvironment;
import com.logmonitoring.tool.repository.ServerEnvironmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Tag(name = "Environment Controller", description = "Sunucu ortamları yönetim uç noktaları (CRUD & Security)")
public class EnvironmentController {

    private final ServerEnvironmentRepository environmentRepository;

    public EnvironmentController(ServerEnvironmentRepository environmentRepository) {
        this.environmentRepository = environmentRepository;
    }

    @Operation(summary = "Tüm kayıtlı sunucuları şifreleri maskelenmiş olarak listeler")
    @GetMapping("/environments")
    public List<ServerEnvironmentResponseDto> getAllEnvironments() {
        return environmentRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Yeni bir sunucu ortamı tanımlar (Girdi Doğrulamalı)")
    @PostMapping("/environments")
    public ResponseEntity<ServerEnvironmentResponseDto> createEnvironment(@Valid @RequestBody ServerEnvironmentRequestDto dto) {
        ServerEnvironment env = new ServerEnvironment();
        env.setName(dto.getName());
        env.setHost(dto.getHost());
        env.setPort(dto.getPort());
        env.setUsername(dto.getUsername());
        env.setPassword(dto.getPassword());
        env.setLogDirectoryPath(dto.getLogDirectoryPath());
        env.setLogFilePath(dto.getLogFilePath());

        ServerEnvironment saved = environmentRepository.save(env);
        return new ResponseEntity<>(mapToResponseDto(saved), HttpStatus.CREATED);
    }

    @Operation(summary = "Mevcut bir sunucu ortamını günceller")
    @PutMapping("/environments/{id}")
    public ResponseEntity<ServerEnvironmentResponseDto> updateEnvironment(
            @PathVariable Long id, 
            @Valid @RequestBody ServerEnvironmentRequestDto dto) {
        
        ServerEnvironment existing = environmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ID: " + id + " olan sunucu bulunamadı."));

        existing.setName(dto.getName());
        existing.setHost(dto.getHost());
        existing.setPort(dto.getPort());
        existing.setUsername(dto.getUsername());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            existing.setPassword(dto.getPassword());
        }
        existing.setLogDirectoryPath(dto.getLogDirectoryPath());
        existing.setLogFilePath(dto.getLogFilePath());

        ServerEnvironment saved = environmentRepository.save(existing);
        return ResponseEntity.ok(mapToResponseDto(saved));
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

    private ServerEnvironmentResponseDto mapToResponseDto(ServerEnvironment env) {
        return new ServerEnvironmentResponseDto(
                env.getId(),
                env.getName(),
                env.getHost(),
                env.getPort(),
                env.getUsername(),
                env.getLogDirectoryPath(),
                env.getLogFilePath()
        );
    }
}