package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "chat_rooms", schema = "hackerton")
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String avatar;

    @Column(name = "is_group", nullable = false)
    private Boolean isGroup = false;

    @Column(name = "formality_level", length = 50)
    private String formalityLevel;

    @Column(length = 50)
    private String relationship;

    @Column(columnDefinition = "TEXT")
    private String keywords;

    public ChatRoom() {
    }

    public ChatRoom(String name, Boolean isGroup) {
        this.name = name;
        this.isGroup = isGroup;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Boolean getIsGroup() {
        return isGroup;
    }

    public void setIsGroup(Boolean isGroup) {
        this.isGroup = isGroup;
    }

    public String getFormalityLevel() {
        return formalityLevel;
    }

    public void setFormalityLevel(String formalityLevel) {
        this.formalityLevel = formalityLevel;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
}
