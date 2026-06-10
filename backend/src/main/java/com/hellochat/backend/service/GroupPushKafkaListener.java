package com.hellochat.backend.service;

import com.hellochat.backend.cache.KafkaEventDedupService;
import com.hellochat.backend.service.event.GroupPushEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class GroupPushKafkaListener {

    private final LocalGroupPushDispatcher localGroupPushDispatcher;
    private final KafkaEventDedupService kafkaEventDedupService;

    public GroupPushKafkaListener(
        LocalGroupPushDispatcher localGroupPushDispatcher,
        KafkaEventDedupService kafkaEventDedupService
    ) {
        this.localGroupPushDispatcher = localGroupPushDispatcher;
        this.kafkaEventDedupService = kafkaEventDedupService;
    }

    @KafkaListener(
        topics = "${hello-chat.concurrency.group-push-topic}",
        groupId = "${spring.application.name}-group-push"
    )
    public void consume(GroupPushEvent event) {
        if (event == null || event.getType() == null) {
            return;
        }
        if (!kafkaEventDedupService.shouldProcess("group-push", event.getEventId())) {
            return;
        }
        switch (event.getType()) {
            case GroupPushEvent.TYPE_NEW_MESSAGE ->
                localGroupPushDispatcher.pushNewMessage(event.getGroupId(), event.getActorUserId(), event.getMessage());
            case GroupPushEvent.TYPE_MESSAGE_UPDATED ->
                localGroupPushDispatcher.pushMessageUpdated(event.getGroupId(), event.getMessageId(), event.getEventType());
            case GroupPushEvent.TYPE_GROUP_UPDATED ->
                localGroupPushDispatcher.pushGroupUpdated(event.getGroupId(), event.getEventType(), event.getGroup());
            default -> {
            }
        }
    }
}
