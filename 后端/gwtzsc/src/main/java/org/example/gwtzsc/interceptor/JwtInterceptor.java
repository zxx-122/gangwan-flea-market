package org.example.gwtzsc.interceptor;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.gwtzsc.common.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Pattern;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final Pattern ITEM_RELATED = Pattern.compile("^/api/item/\\d+/related$");
    /** 商品详情：游客可浏览（下单/收藏/聊天等操作仍需登录） */
    private static final Pattern ITEM_DETAIL = Pattern.compile("^/api/item/\\d+$");
    /** 商品/用户评价、公告列表、求购列表、用户公开主页：游客可看 */
    private static final Pattern PUBLIC_GET = Pattern.compile(
            "^/api/review/item/\\d+$|^/api/review/user/\\d+$|^/api/review/user/\\d+/summary$|^/api/announcement/list$|^/api/want/list$|^/api/user/\\d+/public$|^/api/user/\\d+/items$");

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String method = request.getMethod();
        String uri = request.getRequestURI();
        if (isPublicUri(method, uri)) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token) || !token.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录或Token已过期\",\"data\":null}");
            return false;
        }
        token = token.substring(7);
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"Token无效或已过期\",\"data\":null}");
            return false;
        }
        // 黑名单校验（登出/改密后的旧 token 直接拒绝）
        if (Boolean.TRUE.equals(redisTemplate.hasKey(JwtUtil.BLACKLIST_PREFIX + token))) {
            response.setStatus(401);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"Token已失效，请重新登录\",\"data\":null}");
            return false;
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        request.setAttribute("userId", userId);
        // 角色透传给需要权限判断的接口（如公告管理）
        try {
            request.setAttribute("role", jwtUtil.parseToken(token).get("role"));
        } catch (Exception ignored) {
        }
        return true;
    }

    private boolean isPublicUri(String method, String uri) {
        if ("/api/auth/login".equals(uri) || "/api/auth/register".equals(uri) || "/api/auth/send-sms".equals(uri)
                || "/api/auth/wx-login".equals(uri)) {
            return true;
        }
        if ("/api/category/list".equals(uri)) {
            return true;
        }
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }
        if ("/api/item/list".equals(uri) || "/api/item/search".equals(uri)
                || "/api/item/price-ref".equals(uri)
                || ITEM_RELATED.matcher(uri).matches()
                || ITEM_DETAIL.matcher(uri).matches()
                || PUBLIC_GET.matcher(uri).matches()) {
            return true;
        }
        return uri.startsWith("/api/item/category/");
    }
}