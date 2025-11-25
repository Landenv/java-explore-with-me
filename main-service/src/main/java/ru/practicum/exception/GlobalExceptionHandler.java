package ru.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException exception) {
        log.error("Validation error: {}", exception.getMessage());

        List<String> errors = new ArrayList<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> {
            errors.add(String.format("Field: %s, Error: %s, Value: %s",
                    error.getField(), error.getDefaultMessage(), error.getRejectedValue()));
        });

        ErrorResponse apiError = ErrorResponse.builder()
                .errors(errors)
                .message("Validation error")
                .reason("Incorrectly made request.")
                .status(HttpStatus.BAD_REQUEST.name())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException exception) {
        log.error("Not found error: {}", exception.getMessage());

        ErrorResponse apiError = ErrorResponse.builder()
                .message(exception.getMessage())
                .reason("The required object was not found.")
                .status(HttpStatus.NOT_FOUND.name())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(ConflictException exception) {
        log.error("Conflict error: {}", exception.getMessage());

        ErrorResponse apiError = ErrorResponse.builder()
                .message(exception.getMessage())
                .reason("For the requested operation the conditions are not met.")
                .status(HttpStatus.CONFLICT.name())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(apiError, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException exception) {
        log.error("Forbidden error: {}", exception.getMessage());

        ErrorResponse apiError = ErrorResponse.builder()
                .message(exception.getMessage())
                .reason("For the requested operation the conditions are not met.")
                .status(HttpStatus.FORBIDDEN.name())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(apiError, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException exception) {
        log.error("Validation error: {}", exception.getMessage());

        ErrorResponse apiError = ErrorResponse.builder()
                .message(exception.getMessage())
                .reason("Incorrectly made request.")
                .status(HttpStatus.BAD_REQUEST.name())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.error("Illegal argument error: {}", exception.getMessage());

        ErrorResponse apiError = ErrorResponse.builder()
                .message(exception.getMessage())
                .reason("Incorrectly made request.")
                .status(HttpStatus.BAD_REQUEST.name())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        log.error("HTTP message not readable: {}", exception.getMessage());

        ErrorResponse apiError = ErrorResponse.builder()
                .message("Missing or invalid request body")
                .reason("Incorrectly made request.")
                .status(HttpStatus.BAD_REQUEST.name())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException exception) {
        log.error("Method argument type mismatch: {}", exception.getMessage());

        String errorMessage = String.format("Parameter '%s' should be of type %s",
                exception.getName(), exception.getRequiredType().getSimpleName());

        ErrorResponse apiError = ErrorResponse.builder()
                .message(errorMessage)
                .reason("Incorrectly made request.")
                .status(HttpStatus.BAD_REQUEST.name())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ErrorResponse> handleNumberFormatException(NumberFormatException exception) {
        log.error("Number format error: {}", exception.getMessage());

        ErrorResponse apiError = ErrorResponse.builder()
                .message("Invalid number format in path variable")
                .reason("Incorrectly made request.")
                .status(HttpStatus.BAD_REQUEST.name())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        log.error("Missing request parameter: {}", ex.getMessage());

        String errorMessage = String.format("Required parameter '%s' is missing", ex.getParameterName());

        ErrorResponse apiError = ErrorResponse.builder()
                .message(errorMessage)
                .reason("Incorrectly made request.")
                .status(HttpStatus.BAD_REQUEST.name())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception exception) {
        log.error("Internal server error: {}", exception.getMessage(), exception);

        ErrorResponse apiError = ErrorResponse.builder()
                .message(exception.getMessage())
                .reason("Internal server error.")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.name())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}