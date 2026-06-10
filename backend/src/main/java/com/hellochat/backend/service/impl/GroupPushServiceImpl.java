package com.hellochat.backend.service.impl;

import com.hellochat.backend.config.HighConcurrencyProperties;
import com.hellochat.backend.dto.GroupMessageResponse;
import com.hellochat.backend.dto.GroupResponse;
import com.hellochat.backend.service.GroupPushService;
import com.hellochat.backend.service.event.GroupPushEvent;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class GroupPushServiceImpl implements GroupPushService {

    private final KafkaTemplate<String, GroupPushEvent> kafkaTemplate;
    private final HighConcurrencyProperties properties;

    public GroupPushServiceImpl(
        KafkaTemplate<String, GroupPushEvent> kafkaTemplate,
        HighConcurrencyProperties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Override
    public void pushNewMessage(Long groupId, GroupMessageResponse message) {
        GroupPushEvent event = new GroupPushEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setType(GroupPushEvent.TYPE_NEW_MESSAGE);
        event.setGroupId(groupId);
        event.setActorUserId(message == null ? null : message.getSenderId());
        event.setMessage(message);
        publish(groupId, event);
    }

    @Override
    public void pushMessageUpdated(Long groupId, Long messageId, String eventType) {
        GroupPushEvent event = new GroupPushEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setType(GroupPushEvent.TYPE_MESSAGE_UPDATED);
        event.setGroupId(groupId);
        event.setMessageId(messageId);
        event.setEventType(eventType);
        publish(groupId, event);
    }

    @Override
    public void pushGroupUpdated(Long groupId, String eventType, GroupResponse group) {
        GroupPushEvent event = new GroupPushEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setType(GroupPushEvent.TYPE_GROUP_UPDATED);
        event.setGroupId(groupId);
        event.setEventType(eventType);
        event.setGroup(group);
        publish(groupId, event);
    }

    private void publish(Long groupId, GroupPushEvent event) {
        kafkaTemplate.send(properties.getGroupPushTopic(), String.valueOf(groupId), event);
    }
}
