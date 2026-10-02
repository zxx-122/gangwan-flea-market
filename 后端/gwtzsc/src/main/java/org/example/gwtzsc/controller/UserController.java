package org.example.gwtzsc.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.example.gwtzsc.common.Result;
import org.example.gwtzsc.dto.PasswordRequest;
import org.example.gwtzsc.dto.UserProfileRequest;
import org.example.gwtzsc.dto.WithdrawRequest;
import org.example.gwtzsc.service.ItemService;
import org.example.gwtzsc.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private org.example.gwtzsc.common.JwtUtil jwtUtil;

    @Autowired
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Value("${upload.path}")
    private String uploadPath;

    @PutMapping("/profile")
    public Result<?> updateProfile(HttpServletRequest request, @RequestBody @jakarta.validation.Valid UserProfileRequest req) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updateProfile(userId, req);
        return Result.success();
    }

    @PutMapping("/password")
    public Result<?> updatePassword(HttpServletRequest request, @RequestBody @jakarta.validation.Valid PasswordRequest req) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updatePassword(userId, req);
        // 改密成功后将当前 token 加入黑名单，强制重新登录
        blacklistToken(request);
        return Result.success();
    }

    @PostMapping("/withdraw")
    public Result<?> withdraw(HttpServletRequest request, @RequestBody @jakarta.validation.Valid WithdrawRequest req) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(userService.withdraw(userId, req));
    }

    /** 余额充值（演示环境模拟到账，不接真实支付） */
    @PostMapping("/recharge")
    public Result<?> recharge(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        java.math.BigDecimal amount;
        try {
            amount = new java.math.BigDecimal(String.valueOf(body.get("amount")));
        } catch (Exception e) {
            return Result.error("金额格式不正确");
        }
        if (amount.compareTo(java.math.BigDecimal.ZERO) <= 0) return Result.error("充值金额必须大于0");
        if (amount.compareTo(new java.math.BigDecimal("99999")) > 0) return Result.error("单笔充值不能超过99999");

        org.example.gwtzsc.entity.User user = userService.getById(userId);
        if (user == null) return Result.error("用户不存在");
        if (user.getStatus() != null && user.getStatus() == 0) return Result.error("账号已被禁用");

        user.setBalance(user.getBalance().add(amount));
        userService.addBalance(user, amount, "余额充值");
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("balance", user.getBalance());
        return Result.success(result);
    }

    @GetMapping("/fund-flows")
    public Result<?> fundFlows(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(userService.getFundFlows(userId));
    }

    /** 将当前请求携带的 token 加入 Redis 黑名单（TTL 取剩余有效期） */
    private void blacklistToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) return;
        token = token.substring(7);
        long ttl = jwtUtil.getRemainingMillis(token);
        if (ttl > 0) {
            redisTemplate.opsForValue().set(
                    org.example.gwtzsc.common.JwtUtil.BLACKLIST_PREFIX + token, 1,
                    ttl, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    @GetMapping("/items")
    public Result<?> getMyItems(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(itemService.searchUserItems(userId, keyword, page, size));
    }

    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("仅支持图片文件");
        }
        String originalName = file.getOriginalFilename();
        String ext = FileUtil.extName(originalName);
        if (!isAllowedImageExt(ext)) {
            return Result.error("仅支持 jpg/png/gif/webp 格式");
        }
        String newName = IdUtil.fastSimpleUUID() + "." + ext;
        File dest = new File(uploadPath, newName);
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            return Result.error("上传失败");
        }
        String url = "/uploads/" + newName;
        Long userId = (Long) request.getAttribute("userId");
        userService.updateAvatar(userId, url);
        return Result.success(Map.of("url", url));
    }

    private boolean isAllowedImageExt(String ext) {
        if (ext == null) return false;
        ext = ext.toLowerCase();
        return "jpg".equals(ext) || "jpeg".equals(ext) ||
               "png".equals(ext) || "gif".equals(ext) || "webp".equals(ext);
    }
}
