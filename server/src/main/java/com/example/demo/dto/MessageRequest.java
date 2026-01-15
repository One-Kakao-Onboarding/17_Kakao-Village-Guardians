package com.example.demo.dto;

public class MessageRequest {
    private String user;
    private String content;

    public MessageRequest() {
    }

    public MessageRequest(String user, String content) {
        this.user = user;
        this.content = content;
    }

    public String getUsername() {
        return user;
    }

    public void setUsername(String user) {
        this.user = user;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
