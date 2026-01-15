package com.example.demo.dto.response;

public class MessageOnlyResponse {
    private String message;

    public MessageOnlyResponse() {
    }

    public MessageOnlyResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
