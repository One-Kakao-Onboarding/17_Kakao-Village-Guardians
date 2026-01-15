package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "profiles", schema = "hackerton", indexes = {
    @Index(name = "idx_profiles_user", columnList = "user_id")
})
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String avatar;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "default_persona", length = 100)
    private String defaultPersona;

    @Column(name = "linked_chat_room_ids", columnDefinition = "TEXT")
    private String linkedChatRoomIds;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    public Profile() {
    }

    public Profile(User user, String name) {
        this.user = user;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public String getLinkedChatRoomIds() {
        return linkedChatRoomIds;
    }

    public void setLinkedChatRoomIds(String linkedChatRoomIds) {
        this.linkedChatRoomIds = linkedChatRoomIds;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}
