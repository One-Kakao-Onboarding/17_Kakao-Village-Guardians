package com.example.demo.dto.request;

public class SendMessageRequest {
    private String content;
    private Boolean useTransform = false;
    private Boolean useEmotionGuard = false;
    private Boolean isEmoticon = false;
    private Long emoticonId;
    private String profileId;

    public SendMessageRequest() {
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getUseTransform() {
        return useTransform;
    }

    public void setUseTransform(Boolean useTransform) {
        this.useTransform = useTransform;
    }

    public Boolean getUseEmotionGuard() {
        return useEmotionGuard;
    }

    public void setUseEmotionGuard(Boolean useEmotionGuard) {
        this.useEmotionGuard = useEmotionGuard;
    }

    public Boolean getIsEmoticon() {
        return isEmoticon;
    }

    public void setIsEmoticon(Boolean isEmoticon) {
        this.isEmoticon = isEmoticon;
    }

    public Long getEmoticonId() {
        return emoticonId;
    }

    public void setEmoticonId(Long emoticonId) {
        this.emoticonId = emoticonId;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }
}
