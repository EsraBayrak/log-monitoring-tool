package com.logmonitoring.tool.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "AUDIT_LOGS")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action; // CREATE, UPDATE, DELETE
    private String entityName;
    private Long entityId;
    private String clientIp;
    private LocalDateTime timestamp;
    private String details;

    public AuditLog() {}

    public AuditLog(String action, String entityName, Long entityId, String clientIp, String details) {
        this.action = action;
        this.entityName = entityName;
        this.entityId = entityId;
        this.clientIp = clientIp;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getAction() { return action; }
    public String getEntityName() { return entityName; }
    public Long getEntityId() { return entityId; }
    public String getClientIp() { return clientIp; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getDetails() { return details; }
}