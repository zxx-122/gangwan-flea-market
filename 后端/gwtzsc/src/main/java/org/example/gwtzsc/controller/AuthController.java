package org.example.gwtzsc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.gwtzsc.common.Result;
import org.example.gwtzsc.dto.LoginRequest;
import org.example.gwtzsc.dto.RegisterRequest;
import org.example.gwtzsc.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private UserService userService;
    @Autowired private org.example.gwtzsc.common.JwtUtil jwtUtil;
    @Autowired private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/send-sms")
    public Result<Map<String, Object>> sendSms(@RequestBody Map<String, String> params) {
        String phone = params.get("phone");
        if (phone == null || phone.isBlank()) return Result.error("请输入手机号");
        String code = userService.sendSmsCode(phone);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("smsCode", code);
        data.put("expireSeconds", 300);
        return Result.success(data);
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody @jakarta.validation.Valid RegisterRequest req) {
        userService.register(req);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @jakarta.validation.Valid LoginRequest req) {
        return Result.success(userService.login(req));
    }

    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            long ttl = jwtUtil.getRemainingMillis(token);
            if (ttl > 0) {
                redisTemplate.opsForValue().set(
                        org.example.gwtzsc.common.JwtUtil.BLACKLIST_PREFIX + token, 1,
                        ttl, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        }
        return Result.success();
    }

    @GetMapping("/info")
    public Result<?> getInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(userService.getCurrentUser(userId));
    }
}
