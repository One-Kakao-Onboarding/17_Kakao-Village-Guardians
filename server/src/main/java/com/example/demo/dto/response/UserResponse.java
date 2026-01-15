package com.example.demo.dto.response;

public class UserResponse {
    private Long id;
    private String ldap;
    private String name;
    private String avatar;

    public UserResponse() {
    }

    public UserResponse(Long id, String ldap, String name, String avatar) {
        this.id = id;
        this.ldap = ldap;
        this.name = name;
        this.avatar = avatar;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLdap() {
        return ldap;
    }

    public void setLdap(String ldap) {
        this.ldap = ldap;
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
}
