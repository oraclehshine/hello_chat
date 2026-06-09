package com.hellochat.backend.sentinel;

public final class SentinelResourceNames {

    public static final String AUTH_SEND_CAPTCHA = "auth:sendCaptcha";
    public static final String AUTH_SEND_CAPTCHA_GET = "auth:sendCaptcha:get";
    public static final String AUTH_LOGIN = "auth:login";
    public static final String CHAT_SEND_MESSAGE = "chat:sendMessage";
    public static final String GROUP_SEND_MESSAGE = "group:sendMessage";
    public static final String GROUP_SEND_MENTION_ALL = "group:sendMentionAllMessage";
    public static final String FILE_UPLOAD = "file:upload";

    private SentinelResourceNames() {
    }
}
