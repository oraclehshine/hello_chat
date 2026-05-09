package com.hellochat.backend.service;

import com.hellochat.backend.dto.GroupMessageResponse;
import com.hellochat.backend.dto.GroupResponse;

public interface GroupPushService {

    void pushNewMessage(Long groupId, GroupMessageResponse message);

    void pushMessageUpdated(Long groupId, Long messageId, String eventType);

    void pushGroupUpdated(Long groupId, String eventType, GroupResponse group);
}
