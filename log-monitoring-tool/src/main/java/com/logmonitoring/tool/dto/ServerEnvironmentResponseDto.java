package com.logmonitoring.tool.dto;

public class ServerEnvironmentResponseDto {

    private Long id;
    private String name;
    private String host;
    private int port;
    private String username;
    private String password; // Maskeli: "******"
    private String logDirectoryPath;
    private String logFilePath;

    public ServerEnvironmentResponseDto() {}

    public ServerEnvironmentResponseDto(Long id, String name, String host, int port, String username, String logDirectoryPath, String logFilePath) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = "******";
        this.logDirectoryPath = logDirectoryPath;
        this.logFilePath = logFilePath;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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