package org.example.gwtzsc.service;

import org.example.gwtzsc.dto.MessageRequest;
import org.example.gwtzsc.entity.Message;

import java.util.List;
import java.util.Map;

public interface MessageService {
    void send(Long fromUserId, MessageRequest req);
    List<Map<String, Object>> getSessions(Long userId);
    List<Message> getChat(Long userId, Long otherUserId, Long itemId);
    void markRead(Long userId, Long otherUserId);
}
