package com.logmonitoring.tool.exception;

import com.logmonitoring.tool.dto.ApiErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDto> handleGeneralException(Exception ex, HttpServletRequest request) {
        ApiErrorDto error = new ApiErrorDto(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                ex.getMessage() != null ? ex.getMessage() : "Sunucuda beklenmedik bir hata oluştu",
                request.getRequestURI()
        );

        HttpHeaders headers = new HttpHeaders();
        // SSE akışı sırasında patlamayı önlemek için Content-Type'ı kesin olarak JSON yapıyoruz:
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(error, headers, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}