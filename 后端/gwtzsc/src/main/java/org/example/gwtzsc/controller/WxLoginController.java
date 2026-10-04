package org.example.gwtzsc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.gwtzsc.common.Result;
import org.example.gwtzsc.entity.User;
import org.example.gwtzsc.mapper.UserMapper;
import org.example.gwtzsc.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class WxLoginController {

    @Autowired private UserService userService;
    @Autowired private UserMapper userMapper;
    @Autowired private org.example.gwtzsc.service.FundFlowService fundFlowService;

    @Value("${wx.appid:}")
    private String wxAppid;

    @Value("${wx.secret:}")
    private String wxSecret;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    /**
     * 微信小程序一键登录：
     * 小程序端 wx.login() 拿 code → 本接口调 jscode2session 换 openid
     * → 按 openid 查找/自动创建用户 → 签发本系统 JWT。
     */
    @PostMapping("/wx-login")
    public Result<?> wxLogin(@RequestBody Map<String, String> body) {
        String code = body.getOrDefault("code", "").trim();
        if (code.isEmpty()) return Result.error("缺少微信登录 code");
        if (wxAppid == null || wxAppid.isBlank() || wxSecret == null || wxSecret.isBlank()) {
            return Result.error("微信登录暂未配置，请使用账号密码登录");
        }

        // 1. 调用微信 jscode2session
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + wxAppid
                + "&secret=" + wxSecret + "&js_code=" + code + "&grant_type=authorization_code";
        String resp;
        try {
            resp = httpClient.send(HttpRequest.newBuilder(URI.create(url))
                            .timeout(Duration.ofSeconds(10)).GET().build(),
                    HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) {
            return Result.error("微信服务连接失败，请稍后再试");
        }

        // 2. 解析结果
        String openid;
        try {
            com.fasterxml.jackson.databind.JsonNode node =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp);
            if (node.has("errcode") && node.get("errcode").asInt() != 0) {
                return Result.error("微信登录失败：" + node.path("errmsg").asText(""));
            }
            openid = node.path("openid").asText(null);
            if (openid == null || openid.isBlank()) return Result.error("微信登录失败：未获取到 openid");
        } catch (Exception e) {
            return Result.error("微信登录响应解析失败");
        }

        // 3. 按 openid 查找/创建用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getWxOpenid, openid));
        boolean isNew = false;
        if (user == null) {
            isNew = true;
            user = new User();
            user.setUsername("wx_" + openid.substring(0, Math.min(12, openid.length())));
            user.setPassword(org.mindrot.jbcrypt.BCrypt.hashpw("wx-" + openid, org.mindrot.jbcrypt.BCrypt.gensalt()));
            String nickname = body.getOrDefault("nickname", "").trim();
            user.setNickname(nickname.isEmpty() ? "微信用户" + openid.substring(openid.length() - 4) : nickname);
            user.setWxOpenid(openid);
            user.setRole("USER");
            user.setStatus(1);
            user.setBalance(new BigDecimal("1000"));
            userMapper.insert(user);
            fundFlowService.record(user.getId(), "RECHARGE", user.getBalance(), user.getBalance(),
                    "新用户注册赠送体验金", null);
        } else if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.error("账号已被禁用");
        }

        // 4. 签发 JWT（复用统一登录返回结构）
        Map<String, Object> result = userService.buildLoginResult(user);
        if (isNew) {
            Map<String, Object> data = new java.util.HashMap<>(result);
            data.put("isNewUser", true);
            return Result.success(data);
        }
        return Result.success(result);
    }
}
