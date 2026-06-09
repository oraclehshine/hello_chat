package com.hellochat.backend.sentinel;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hello-chat.sentinel")
public class SentinelRuleProperties {

    private boolean localRulesEnabled = true;
    private double authCaptchaQps = 2.0d;
    private double authLoginQps = 5.0d;
    private double chatSendMessageQps = 25.0d;
    private double groupSendMessageQps = 40.0d;
    private double groupMentionAllQps = 3.0d;
    private double fileUploadQps = 5.0d;

    public boolean isLocalRulesEnabled() {
        return localRulesEnabled;
    }

    public void setLocalRulesEnabled(boolean localRulesEnabled) {
        this.localRulesEnabled = localRulesEnabled;
    }

    public double getAuthCaptchaQps() {
        return authCaptchaQps;
    }

    public void setAuthCaptchaQps(double authCaptchaQps) {
        this.authCaptchaQps = authCaptchaQps;
    }

    public double getAuthLoginQps() {
        return authLoginQps;
    }

    public void setAuthLoginQps(double authLoginQps) {
        this.authLoginQps = authLoginQps;
    }

    public double getChatSendMessageQps() {
        return chatSendMessageQps;
    }

    public void setChatSendMessageQps(double chatSendMessageQps) {
        this.chatSendMessageQps = chatSendMessageQps;
    }

    public double getGroupSendMessageQps() {
        return groupSendMessageQps;
    }

    public void setGroupSendMessageQps(double groupSendMessageQps) {
        this.groupSendMessageQps = groupSendMessageQps;
    }

    public double getGroupMentionAllQps() {
        return groupMentionAllQps;
    }

    public void setGroupMentionAllQps(double groupMentionAllQps) {
        this.groupMentionAllQps = groupMentionAllQps;
    }

    public double getFileUploadQps() {
        return fileUploadQps;
    }

    public void setFileUploadQps(double fileUploadQps) {
        this.fileUploadQps = fileUploadQps;
    }
}
