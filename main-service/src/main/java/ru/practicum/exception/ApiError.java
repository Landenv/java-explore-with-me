package ru.practicum.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    @Builder.Default
    private List<String> errors = List.of();
    private String message;
    private String reason;
    private String status;
    private String timestamp;
}