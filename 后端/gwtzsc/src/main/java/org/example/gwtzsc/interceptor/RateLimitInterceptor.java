package org.example.gwtzsc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.regex.Pattern;

/**
 * 基于 Redis 的接口限流：防登录爆破、短信轰炸、接口滥用。
 * 规则来自 application.yml 的 rate-limit 配置（次数/窗口秒）。
 * Redis 不可用时自动放行（fail-open），不影响正常业务。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Pattern UPLOAD_PATH = Pattern.compile("^/api/upload(/.*)?$");
    private static final String SMS_PATH = "/api/auth/send-sms";
    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${rate-limit.auth-login:10/60}")
    private String rlLogin;
    @Value("${rate-limit.auth-register:5/60}")
    private String rlRegister;
    @Value("${rate-limit.auth-sms:5/3600}")
    private String rlSms;
    @Value("${rate-limit.upload:30/60}")
    private String rlUpload;
    @Value("${rate-limit.global:300/60}")
    private String rlGlobal;

    private record Rule(long limit, long windowSec) {}

    private Rule parse(String conf) {
        try {
            String[] parts = conf.split("/");
            return new Rule(Long.parseLong(parts[0].trim()), Long.parseLong(parts[1].trim()));
        } catch (Exception e) {
            return new Rule(1000, 60);
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String uri = request.getRequestURI();
        String ip = clientIp(request);
        long now = System.currentTimeMillis();

        // 1. 全局限流
        if (!allow("rl:global:" + ip, parse(rlGlobal), now)) return tooMany(response);
        // 2. 敏感接口单独限流
        if (LOGIN_PATH.equals(uri) && !allow("rl:login:" + ip, parse(rlLogin), now)) return tooMany(response);
        if (REGISTER_PATH.equals(uri) && !allow("rl:register:" + ip, parse(rlRegister), now)) return tooMany(response);
        if (SMS_PATH.equals(uri) && !allow("rl:sms:" + ip, parse(rlSms), now)) return tooMany(response);
        if (UPLOAD_PATH.matcher(uri).matches() && !allow("rl:upload:" + ip, parse(rlUpload), now)) return tooMany(response);
        return true;
    }

    /** Redis INCR + 首次设置过期；执行失败（Redis 不可用）放行 */
    private boolean allow(String key, Rule rule, long now) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(rule.windowSec()));
            }
            return count == null || count <= rule.limit();
        } catch (Exception e) {
            return true; // fail-open
        }
    }

    private boolean tooMany(HttpServletResponse response) throws Exception {
        response.setStatus(429);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write("{\"code\":429,\"msg\":\"请求过于频繁，请稍后再试\",\"data\":null}");
        return false;
    }

    /** 客户端真实 IP：优先 X-Forwarded-First（Nginx 已设置 X-Real-IP） */
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return request.getRemoteAddr();
    }
}
