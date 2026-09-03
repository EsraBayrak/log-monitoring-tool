package com.logmonitoring.tool.service;

import com.logmonitoring.tool.model.ServerEnvironment;
import com.logmonitoring.tool.repository.ServerEnvironmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogServiceIntegrationTest {

    @Mock
    private ServerEnvironmentRepository environmentRepository;

    @Mock
    private Executor logStreamExecutor;

    @InjectMocks
    private LogService logService;

    @Test
    @DisplayName("Çoklu Akış: 5 eşzamanlı SSE akışı başlatıldığında tüm Emitter nesneleri başarıyla üretilmeli")
    void shouldHandleConcurrentLogStreamsWithoutBlocking() throws InterruptedException, ExecutionException {
        Long envId = 1L;
        ServerEnvironment env = new ServerEnvironment();
        env.setId(envId);
        env.setHost("192.168.1.100");
        env.setLogFilePath("/var/log/app.log");

        when(environmentRepository.findById(envId)).thenReturn(Optional.of(env));

        // Asenkron executor mock'u: task'ı hemen çalıştırsın
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            new Thread(task).start();
            return null;
        }).when(logStreamExecutor).execute(any(Runnable.class));

        int clientCount = 5;
        ExecutorService clientPool = Executors.newFixedThreadPool(clientCount);
        List<Callable<SseEmitter>> tasks = new ArrayList<>();

        for (int i = 0; i < clientCount; i++) {
            tasks.add(() -> logService.streamLiveLogs(envId));
        }

        List<Future<SseEmitter>> results = clientPool.invokeAll(tasks);

        assertEquals(clientCount, results.size());
        for (Future<SseEmitter> future : results) {
            SseEmitter emitter = future.get();
            assertNotNull(emitter, "Her istemci için geçerli bir SseEmitter örneği üretilmeli");
        }

        clientPool.shutdown();
    }

    @Test
    @DisplayName("Bağlantı Hatası Dayanıklılığı: Geçersiz sunucuda SSE akışı zarifçe (graceful) sonlanmalı")
    void shouldHandleConnectionFailureGracefully() {
        Long nonExistingId = 999L;
        when(environmentRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        SseEmitter emitter = logService.streamLiveLogs(nonExistingId);

        assertNotNull(emitter);
        // Sunucu bulunamadığında exception fırlatmak yerine emitter üretilip kontrollü kapatılmalı
        verify(environmentRepository, times(1)).findById(nonExistingId);
    }
}