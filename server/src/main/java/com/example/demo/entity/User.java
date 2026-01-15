package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users", schema = "hackerton", indexes = {
    @Index(name = "idx_users_ldap", columnList = "ldap")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String ldap;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String avatar;

    public User() {
    }

    public User(String ldap, String name) {
        this.ldap = ldap;
        this.name = name;
    }

    public User(String ldap, String name, String avatar) {
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
