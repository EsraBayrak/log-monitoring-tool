package com.logmonitoring.tool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logmonitoring.tool.dto.ServerEnvironmentRequestDto;
import com.logmonitoring.tool.model.ServerEnvironment;
import com.logmonitoring.tool.repository.ServerEnvironmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import com.logmonitoring.tool.repository.AuditLogRepository;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EnvironmentControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ServerEnvironmentRepository environmentRepository;

    @Mock
    private AuditLogRepository auditLogRepository; 

    @InjectMocks
    private EnvironmentController environmentController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(environmentController).build();
    }

    @Test
    @DisplayName("GET /api/environments - Kayıtlı sunucuları şifre maskeli olarak dönmeli (200 OK)")
    void shouldReturnMaskedEnvironments() throws Exception {
        ServerEnvironment env = new ServerEnvironment();
        env.setId(1L);
        env.setName("Test Server");
        env.setHost("10.0.0.1");
        env.setPort(22);
        env.setUsername("admin");
        env.setPassword("gizliSifre");

        when(environmentRepository.findAll()).thenReturn(List.of(env));

        mockMvc.perform(get("/api/environments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Server"))
                .andExpect(jsonPath("$[0].password").value("******"));
    }

    @Test
    @DisplayName("POST /api/environments - Geçerli DTO ile yeni sunucu kaydetmeli (201 Created)")
    void shouldCreateEnvironmentWhenValidDto() throws Exception {
        ServerEnvironmentRequestDto dto = new ServerEnvironmentRequestDto();
        dto.setName("Prod Server");
        dto.setHost("192.168.1.50");
        dto.setPort(22);
        dto.setUsername("root");

        ServerEnvironment savedEnv = new ServerEnvironment();
        savedEnv.setId(10L);
        savedEnv.setName("Prod Server");
        savedEnv.setHost("192.168.1.50");
        savedEnv.setPort(22);

        when(environmentRepository.save(any(ServerEnvironment.class))).thenReturn(savedEnv);

        mockMvc.perform(post("/api/environments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.name").value("Prod Server"));
    }

    @Test
    @DisplayName("DELETE /api/environments/{id} - Var olan sunucuyu silmeli (204 No Content)")
    void shouldDeleteEnvironment() throws Exception {
        when(environmentRepository.existsById(1L)).thenReturn(true);
        doNothing().when(environmentRepository).deleteById(1L);

        mockMvc.perform(delete("/api/environments/1"))
                .andExpect(status().isNoContent());

        verify(environmentRepository, times(1)).deleteById(1L);
    }
}