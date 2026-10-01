package org.example.gwtzsc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.gwtzsc.dto.MessageRequest;
import org.example.gwtzsc.entity.Message;
import org.example.gwtzsc.entity.User;
import org.example.gwtzsc.mapper.MessageMapper;
import org.example.gwtzsc.mapper.UserMapper;
import org.example.gwtzsc.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String SESSION_CACHE_PREFIX = "session:";

    @Override
    @Transactional
    public void send(Long fromUserId, MessageRequest req) {
        if (req.getToUserId() == null || fromUserId.equals(req.getToUserId())) {
            throw new RuntimeException("无效的接收者");
        }
        if (req.getContent() == null || req.getContent().trim().isEmpty()) {
            throw new RuntimeException("消息内容不能为空");
        }
        Message message = new Message();
        message.setItemId(req.getItemId());
        message.setFromUserId(fromUserId);
        message.setToUserId(req.getToUserId());
        message.setContent(req.getContent().trim());
        message.setIsRead(0);
        messageMapper.insert(message);
        // 清除双方会话列表缓存
        redisTemplate.delete(SESSION_CACHE_PREFIX + fromUserId);
        redisTemplate.delete(SESSION_CACHE_PREFIX + req.getToUserId());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getSessions(Long userId) {
        String cacheKey = SESSION_CACHE_PREFIX + userId;
        List<Map<String, Object>> cached = (List<Map<String, Object>>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return cached;
        // 获取与当前用户相关的所有消息
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getFromUserId, userId)
                        .or()
                        .eq(Message::getToUserId, userId)
                        .orderByDesc(Message::getCreatedAt));

        // 按对方用户分组
        Map<Long, List<Message>> grouped = messages.stream()
                .collect(Collectors.groupingBy(m ->
                        m.getFromUserId().equals(userId) ? m.getToUserId() : m.getFromUserId()));

        // 批量查询对方用户信息，避免逐会话查询（N+1）
        Map<Long, User> usersById = grouped.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(grouped.keySet()).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));

        List<Map<String, Object>> sessions = new ArrayList<>();
        for (Map.Entry<Long, List<Message>> entry : grouped.entrySet()) {
            Long otherUserId = entry.getKey();
            List<Message> msgs = entry.getValue();
            Message last = msgs.get(0);

            long unread = msgs.stream()
                    .filter(m -> m.getToUserId().equals(userId) && m.getIsRead() == 0)
                    .count();

            User other = usersById.get(otherUserId);
            Map<String, Object> session = new LinkedHashMap<>();
            session.put("userId", otherUserId);
            session.put("username", other != null ? other.getUsername() : "未知用户");
            session.put("nickname", other != null ? other.getNickname() : null);
            session.put("avatar", other != null ? other.getAvatar() : null);
            session.put("lastContent", last.getContent());
            session.put("lastTime", last.getCreatedAt());
            session.put("unread", unread);
            sessions.add(session);
        }

        // 按最后消息时间排序（LocalDateTime implements Comparable）
        sessions.sort((a, b) -> {
            Comparable<Object> ca = (Comparable<Object>) a.get("lastTime");
            Comparable<Object> cb = (Comparable<Object>) b.get("lastTime");
            if (ca == null && cb == null) return 0;
            if (ca == null) return 1;
            if (cb == null) return -1;
            return cb.compareTo(ca);
        });

        redisTemplate.opsForValue().set(cacheKey, sessions, 30, TimeUnit.MINUTES);
        return sessions;
    }

    @Override
    public List<Message> getChat(Long userId, Long otherUserId, Long itemId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .and(w -> w.eq(Message::getFromUserId, userId).eq(Message::getToUserId, otherUserId)
                        .or().eq(Message::getFromUserId, otherUserId).eq(Message::getToUserId, userId))
                .orderByAsc(Message::getCreatedAt);

        if (itemId != null) {
            wrapper.eq(Message::getItemId, itemId);
        }

        return messageMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long otherUserId) {
        Message message = new Message();
        message.setIsRead(1);
        messageMapper.update(message,
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getFromUserId, otherUserId)
                        .eq(Message::getToUserId, userId)
                        .eq(Message::getIsRead, 0));
        // 已读数变化，清除本人会话列表缓存
        redisTemplate.delete(SESSION_CACHE_PREFIX + userId);
    }
}
