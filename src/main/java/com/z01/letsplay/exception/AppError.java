package com.z01.letsplay.exception;

import org.springframework.http.HttpStatus;

public enum AppError {
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later"),

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "The request contains invalid data"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "The request body is missing or is not valid JSON"),
    PASSWORD_TOO_LONG(HttpStatus.BAD_REQUEST, "Password must not exceed 72 bytes when UTF-8 encoded"),

    MISSING_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "Authentication required: log in first"),
    INVALID_JWT(HttpStatus.UNAUTHORIZED, "The session is invalid or has expired. Please log in again"),
    INVALID_EMAIL_OR_PASSWORD(HttpStatus.UNAUTHORIZED, "Invalid email or password"),

    PERMISSION_DENIED(HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "You do not have access to this resource"),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "An account with this email already exists"),

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found"),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found");

    private final HttpStatus status;
    private final String message;

    AppError(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }

    public AppException asException() {
        return new AppException(this);
    }
}
