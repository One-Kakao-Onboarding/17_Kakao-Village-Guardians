package com.example.demo.dto.request;

import java.util.List;

public class ProfileRequest {
    private String name;
    private String avatar;
    private String description;
    private String defaultPersona;
    private List<String> assignedFriends;

    public ProfileRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultPersona() {
        return defaultPersona;
    }

    public void setDefaultPersona(String defaultPersona) {
        this.defaultPersona = defaultPersona;
    }

    public List<String> getAssignedFriends() {
        return assignedFriends;
    }

    public void setAssignedFriends(List<String> assignedFriends) {
        this.assignedFriends = assignedFriends;
    }
}
