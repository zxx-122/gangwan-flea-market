package org.example.gwtzsc.service;

import org.example.gwtzsc.dto.*;
import org.example.gwtzsc.entity.FundFlow;
import org.example.gwtzsc.entity.User;
import java.util.List;
import java.util.Map;

public interface UserService {
    /** 发送验证码，返回本次生成的验证码（演示环境直接回传给前端弹窗展示） */
    String sendSmsCode(String phone);
    User register(RegisterRequest req);
    Map<String, Object> login(LoginRequest req);
    User getById(Long id);
    User getCurrentUser(Long userId);
    void updateProfile(Long userId, UserProfileRequest req);
    void updatePassword(Long userId, PasswordRequest req);
    void updateAvatar(Long userId, String avatarUrl);

    /** 余额提现，返回最新余额 */
    Map<String, Object> withdraw(Long userId, WithdrawRequest req);

    /** 构建统一登录返回（token + 脱敏用户信息） */
    Map<String, Object> buildLoginResult(User user);

    /** 直接增加余额并记录充值流水（调用方需已设置好新余额） */
    void addBalance(org.example.gwtzsc.entity.User user, java.math.BigDecimal amount, String remark);

    /** 当前用户资金流水（按时间倒序） */
    List<FundFlow> getFundFlows(Long userId);

    // 卖家数据（详情页展示）
    Map<String, Object> getSellerStats(Long userId);
}