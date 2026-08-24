package com.logmonitoring.tool.dto;

import java.util.List;

public class LogStatsDto {
    private long totalLines;
    private long errorCount;
    private long warnCount;
    private long infoCount;
    private List<String> recentErrors;

    public LogStatsDto() {}

    public LogStatsDto(long totalLines, long errorCount, long warnCount, long infoCount, List<String> recentErrors) {
        this.totalLines = totalLines;
        this.errorCount = errorCount;
        this.warnCount = warnCount;
        this.infoCount = infoCount;
        this.recentErrors = recentErrors;
    }

    public long getTotalLines() { return totalLines; }
    public void setTotalLines(long totalLines) { this.totalLines = totalLines; }

    public long getErrorCount() { return errorCount; }
    public void setErrorCount(long errorCount) { this.errorCount = errorCount; }

    public long getWarnCount() { return warnCount; }
    public void setWarnCount(long warnCount) { this.warnCount = warnCount; }

    public long getInfoCount() { return infoCount; }
    public void setInfoCount(long infoCount) { this.infoCount = infoCount; }

    public List<String> getRecentErrors() { return recentErrors; }
    public void setRecentErrors(List<String> recentErrors) { this.recentErrors = recentErrors; }
}