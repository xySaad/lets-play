package com.z01.letsplay.exception;

public class AppException extends RuntimeException {
    private final AppError error;

    public AppException(AppError error) {
        super(error.message());
        this.error = error;
    }

    public AppException(AppError error, String message) {
        super(message);
        this.error = error;
    }

    public AppError getError() {
        return error;
    }
}
