package org.example.gwtzsc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.gwtzsc.common.Result;
import org.example.gwtzsc.dto.MessageRequest;
import org.example.gwtzsc.entity.Message;
import org.example.gwtzsc.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @PostMapping
    public Result<?> send(HttpServletRequest request, @RequestBody @jakarta.validation.Valid MessageRequest req) {
        Long userId = (Long) request.getAttribute("userId");
        messageService.send(userId, req);
        return Result.success();
    }

    @GetMapping("/sessions")
    public Result<List<Map<String, Object>>> sessions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(messageService.getSessions(userId));
    }

    @GetMapping("/chat")
    public Result<List<Message>> chat(
            HttpServletRequest request,
            @RequestParam Long otherUserId,
            @RequestParam(required = false) Long itemId) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(messageService.getChat(userId, otherUserId, itemId));
    }

    @PutMapping("/read")
    public Result<?> markRead(HttpServletRequest request, @RequestBody Map<String, Long> params) {
        Long userId = (Long) request.getAttribute("userId");
        Long otherUserId = params.get("otherUserId");
        messageService.markRead(userId, otherUserId);
        return Result.success();
    }
}
