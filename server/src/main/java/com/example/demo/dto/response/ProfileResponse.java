package com.example.demo.dto.response;

import java.util.List;

public class ProfileResponse {
    private Long id;
    private Long userId;
    private String name;
    private String avatar;
    private String description;
    private String defaultPersona;
    private List<String> assignedFriends;
    private Boolean isDefault;

    public ProfileResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}
