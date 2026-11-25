package com.z01.letsplay.model.DTO;

public record AppErrorResponse(int status, String error, String message) {

    // Hand-built so that errors raised inside the security filter chain (before Spring MVC
    // and Jackson are involved) have exactly the same shape as the ones from GlobalExceptionHandler.
    public String json() {
        return "{\"status\":" + status + ",\"error\":\"" + escape(error) + "\",\"message\":\"" + escape(message)
                + "\"}";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
