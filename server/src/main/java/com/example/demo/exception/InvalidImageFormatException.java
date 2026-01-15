package com.example.demo.exception;

import java.util.List;

public class InvalidImageFormatException extends RuntimeException {
    private final List<String> supportedFormats;

    public InvalidImageFormatException(String message, List<String> supportedFormats) {
        super(message);
        this.supportedFormats = supportedFormats;
    }

    public List<String> getSupportedFormats() {
        return supportedFormats;
    }
}
