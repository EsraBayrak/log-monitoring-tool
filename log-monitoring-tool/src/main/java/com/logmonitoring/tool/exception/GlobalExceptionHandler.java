package com.logmonitoring.tool.exception;

import com.jcraft.jsch.JSchException;
import com.logmonitoring.tool.dto.ApiErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JSchException.class)
    public ResponseEntity<ApiErrorDto> handleJSchException(JSchException ex, HttpServletRequest request) {
        ApiErrorDto error = new ApiErrorDto(
                HttpStatus.BAD_GATEWAY.value(),
                "SSH_CONNECTION_ERROR",
                "Uzak sunucuya SSH bağlantısı kurulamadı: " + ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiErrorDto> handleNotFoundException(NoSuchElementException ex, HttpServletRequest request) {
        ApiErrorDto error = new ApiErrorDto(
                HttpStatus.NOT_FOUND.value(),
                "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDto> handleGeneralException(Exception ex, HttpServletRequest request) {
        ApiErrorDto error = new ApiErrorDto(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "Sunucuda beklenmedik bir hata oluştu: " + ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}