package com.hellochat.backend.dto;

public class GroupNoticeReadResponse {

    private final long readCount;
    private final long memberCount;

    public GroupNoticeReadResponse(long readCount, long memberCount) {
        this.readCount = readCount;
        this.memberCount = memberCount;
    }

    public long getReadCount() {
        return readCount;
    }

    public long getMemberCount() {
        return memberCount;
    }
}
