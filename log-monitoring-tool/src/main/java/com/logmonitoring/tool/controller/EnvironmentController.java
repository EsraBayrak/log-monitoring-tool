package com.logmonitoring.tool.controller;

import com.logmonitoring.tool.dto.ServerEnvironmentRequestDto;
import com.logmonitoring.tool.dto.ServerEnvironmentResponseDto;
import com.logmonitoring.tool.model.AuditLog;
import com.logmonitoring.tool.model.ServerEnvironment;
import com.logmonitoring.tool.repository.AuditLogRepository;
import com.logmonitoring.tool.repository.ServerEnvironmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Tag(name = "Environment Controller", description = "Sunucu ortamları ve Audit log yönetimi")
public class EnvironmentController {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentController.class);

    private final ServerEnvironmentRepository environmentRepository;
    private final AuditLogRepository auditLogRepository;

    public EnvironmentController(ServerEnvironmentRepository environmentRepository, 
                                 AuditLogRepository auditLogRepository) {
        this.environmentRepository = environmentRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Operation(summary = "Tüm kayıtlı sunucuları şifreleri maskelenmiş olarak listeler")
    @GetMapping("/environments")
    @Cacheable(value = "environmentsCache")
    public List<ServerEnvironmentResponseDto> getAllEnvironments() {
        return environmentRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Yeni bir sunucu ortamı tanımlar")
    @PostMapping("/environments")
    @CacheEvict(value = "environmentsCache", allEntries = true)
    public ResponseEntity<ServerEnvironmentResponseDto> createEnvironment(
            @Valid @RequestBody ServerEnvironmentRequestDto dto,
            HttpServletRequest request) {
        
        ServerEnvironment env = new ServerEnvironment();
        env.setName(dto.getName());
        env.setHost(dto.getHost());
        env.setPort(dto.getPort());
        env.setUsername(dto.getUsername());
        env.setPassword(dto.getPassword());
        env.setLogDirectoryPath(dto.getLogDirectoryPath());
        env.setLogFilePath(dto.getLogFilePath());

        ServerEnvironment saved = environmentRepository.save(env);
        
        auditLogRepository.save(new AuditLog("CREATE", "ServerEnvironment", saved.getId(), request.getRemoteAddr(), "Sunucu eklendi: " + saved.getName()));
        log.info("Yeni sunucu kaydı oluşturuldu: ID={}, Name={}", saved.getId(), saved.getName());

        return new ResponseEntity<>(mapToResponseDto(saved), HttpStatus.CREATED);
    }

    @Operation(summary = "Mevcut bir sunucu ortamını günceller")
    @PutMapping("/environments/{id}")
    @CacheEvict(value = "environmentsCache", allEntries = true)
    public ResponseEntity<ServerEnvironmentResponseDto> updateEnvironment(
            @PathVariable Long id, 
            @Valid @RequestBody ServerEnvironmentRequestDto dto,
            HttpServletRequest request) {

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

        auditLogRepository.save(new AuditLog("UPDATE", "ServerEnvironment", saved.getId(), request.getRemoteAddr(), "Sunucu güncellendi: " + saved.getName()));
        log.info("Sunucu güncellendi: ID={}", saved.getId());

        return ResponseEntity.ok(mapToResponseDto(saved));
    }

    @Operation(summary = "Kayıtlı bir sunucu ortamını siler")
    @DeleteMapping("/environments/{id}")
    @CacheEvict(value = "environmentsCache", allEntries = true)
    public ResponseEntity<Void> deleteEnvironment(@PathVariable Long id, HttpServletRequest request) {
        if (!environmentRepository.existsById(id)) {
            throw new NoSuchElementException("ID: " + id + " olan sunucu bulunamadı.");
        }
        environmentRepository.deleteById(id);

        auditLogRepository.save(new AuditLog("DELETE", "ServerEnvironment", id, request.getRemoteAddr(), "Sunucu silindi: ID=" + id));
        log.info("Sunucu silindi: ID={}", id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Son 50 işlem denetim kaydını (Audit Logs) listeler")
    @GetMapping("/audit-logs")
    public List<AuditLog> getAuditLogs() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc();
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