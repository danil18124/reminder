package com.example.reminder.model.dto;

import java.util.List;
import java.util.Map;

public class ApiErrorResponse<T> {
    private final String errorCode;
    private final String message;
    private final Map<String, T> details;

    public ApiErrorResponse(String errorCode, String message, Map<String, T> details) {
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
    }

    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
    public Map<String, T> getDetails() { return details; }
}



