package com.hellochat.backend.dto;

public class TypingStatusRequest {

    private boolean typing;

    public boolean isTyping() {
        return typing;
    }

    public void setTyping(boolean typing) {
        this.typing = typing;
    }
}
