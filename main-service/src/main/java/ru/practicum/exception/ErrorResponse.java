package ru.practicum.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ErrorResponse {
    private List<String> errors;
    private String message;
    private String reason;
    private String status;
    private LocalDateTime timestamp;
}