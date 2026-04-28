package com.openhour.backend.dto;

public class CancellationResponse {
    private boolean success;
    private String message;

    public CancellationResponse() {}

    public CancellationResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
