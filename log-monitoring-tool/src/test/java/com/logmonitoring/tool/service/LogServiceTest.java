package com.logmonitoring.tool.service;

import com.logmonitoring.tool.dto.LogStatsDto;
import com.logmonitoring.tool.model.ServerEnvironment;
import com.logmonitoring.tool.repository.ServerEnvironmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogServiceTest {

    @Mock
    private ServerEnvironmentRepository environmentRepository;

    @Mock
    private Executor logStreamExecutor;

    @Spy
    @InjectMocks
    private LogService logService;

    @Test
    @DisplayName("analyzeLogStats - Gelen log satırlarını ERROR, WARN ve INFO olarak doğru saymalı")
    void shouldCorrectlyAnalyzeLogStats() {
        Long envId = 1L;
        String mockRawLogs = "INFO [Server] Application started\n" +
                             "WARN [DB] Slow query detected\n" +
                             "ERROR [Auth] NullPointerException in login\n" +
                             "FATAL [System] OutOfMemoryError detected";

        doReturn(mockRawLogs).when(logService).fetchTailLogs(envId, 100);

        LogStatsDto stats = logService.analyzeLogStats(envId, 100);

        assertNotNull(stats);
        assertEquals(4, stats.getTotalLines());
        assertEquals(2, stats.getErrorCount()); // ERROR + FATAL
        assertEquals(1, stats.getWarnCount());
        assertEquals(1, stats.getInfoCount());
        assertEquals(2, stats.getRecentErrors().size());
    }

    @Test
    @DisplayName("searchLogsWithGrep - Sunucu bulunamadığında uygun hata mesajı dönmeli")
    void shouldReturnErrorMessageWhenServerNotFound() {
        Long invalidId = 999L;
        when(environmentRepository.findById(invalidId)).thenReturn(Optional.empty());

        String result = logService.searchLogsWithGrep(invalidId, "test.log", "ERROR", "NullPointer", 50);

        assertEquals("[HATA] Sunucu tanımı bulunamadı.", result);
    }
}