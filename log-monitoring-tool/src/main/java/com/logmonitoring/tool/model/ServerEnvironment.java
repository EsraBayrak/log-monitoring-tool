package com.logmonitoring.tool.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "server_environments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerEnvironment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;           // Örn: Test 1, Test 2
    private String host;           // Örn: 10.248.67.233
    private Integer port = 22;     // Varsayılan SSH portu
    private String username;
    private String password;
    private String logFilePath;    // Örn: /appdata/.../oim_m1/logs/oim_m1.out
    private String logDirectoryPath; // Örn: /appdata/.../oim_m1/logs/
    private String configFilePath; // Örn: /appdata/.../oim_m1/logs/logback.xml
}