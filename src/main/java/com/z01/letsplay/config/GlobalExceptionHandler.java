package com.z01.letsplay.config;

import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.z01.letsplay.exception.AppError;
import com.z01.letsplay.exception.AppException;
import com.z01.letsplay.model.DTO.AppErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<AppErrorResponse> handleAppException(AppException ex) {
        AppError error = ex.getError();
        return respond(error.status(), error.name(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AppErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));
        if (message.isEmpty())
            message = AppError.VALIDATION_FAILED.message();
        return respond(HttpStatus.BAD_REQUEST, AppError.VALIDATION_FAILED.name(), message);
    }

    // missing body, broken JSON, or a JSON object/array where a string is expected
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AppErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        AppError error = AppError.MALFORMED_REQUEST;
        return respond(error.status(), error.name(), error.message());
    }

    // unique index violated
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<AppErrorResponse> handleDuplicateKey(DuplicateKeyException ex) {
        AppError error = AppError.EMAIL_ALREADY_EXISTS;
        return respond(error.status(), error.name(), error.message());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
        // Spring MVC's own exceptions instead of answering 500
        if (ex instanceof org.springframework.web.ErrorResponse er) {
            HttpStatusCode code = er.getStatusCode();
            String name = code instanceof HttpStatus status ? status.name() : "HTTP_" + code.value();
            String detail = er.getBody().getDetail();
            String message = detail != null ? detail
                    : code instanceof HttpStatus status ? status.getReasonPhrase() : name;
            return ResponseEntity.status(code)
                    .headers(er.getHeaders())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AppErrorResponse(code.value(), name, message));
        }

        log.error("Unhandled exception", ex);
        AppError error = AppError.INTERNAL_ERROR;
        return respond(error.status(), error.name(), error.message());
    }

    private static ResponseEntity<AppErrorResponse> respond(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AppErrorResponse(status.value(), error, message));
    }
}
