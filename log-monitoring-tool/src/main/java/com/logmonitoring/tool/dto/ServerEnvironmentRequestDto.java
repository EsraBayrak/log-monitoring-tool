package com.logmonitoring.tool.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ServerEnvironmentRequestDto {

    @NotBlank(message = "Sunucu adı boş bırakılamaz.")
    private String name;

    @NotBlank(message = "Host / IP adresi boş bırakılamaz.")
    @Pattern(
        regexp = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$|^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$",
        message = "Geçerli bir IP adresi veya Hostname giriniz."
    )
    private String host;

    @Min(value = 1, message = "Port 1'den küçük olamaz.")
    @Max(value = 65535, message = "Port 65535'ten büyük olamaz.")
    private int port = 22;

    private String username;
    private String password;
    private String logDirectoryPath;
    private String logFilePath;

    public ServerEnvironmentRequestDto() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getLogDirectoryPath() { return logDirectoryPath; }
    public void setLogDirectoryPath(String logDirectoryPath) { this.logDirectoryPath = logDirectoryPath; }
    public String getLogFilePath() { return logFilePath; }
    public void setLogFilePath(String logFilePath) { this.logFilePath = logFilePath; }
}