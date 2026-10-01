package org.example.gwtzsc.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import org.mindrot.jbcrypt.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.gwtzsc.common.JwtUtil;
import org.example.gwtzsc.dto.*;
import org.example.gwtzsc.entity.Item;
import org.example.gwtzsc.entity.Order;
import org.example.gwtzsc.entity.User;
import org.example.gwtzsc.mapper.ItemMapper;
import org.example.gwtzsc.mapper.OrderMapper;
import org.example.gwtzsc.mapper.UserMapper;
import org.example.gwtzsc.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    @Autowired private UserMapper userMapper;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private RedisTemplate<String, Object> redisTemplate;
    @Autowired private OrderMapper orderMapper;
    @Autowired private ItemMapper itemMapper;
    @Autowired private org.example.gwtzsc.service.FundFlowService fundFlowService;

    private static final String SMS_PREFIX = "sms:code:";

    @Override
    public String sendSmsCode(String phone) {
        if (!phone.matches("^1\\d{10}$")) throw new RuntimeException("手机号格式不正确");
        // 60 秒内不允许重发
        String limitKey = SMS_PREFIX + "limit:" + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            Long ttl = redisTemplate.getExpire(limitKey, TimeUnit.SECONDS);
            throw new RuntimeException("发送过于频繁，请 " + Math.max(ttl, 1L) + " 秒后再试");
        }
        String code = RandomUtil.randomNumbers(6);
        redisTemplate.opsForValue().set(SMS_PREFIX + phone, code, 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(limitKey, "1", 60, TimeUnit.SECONDS);
        // 开发环境：验证码直接打印到控制台
        System.out.println("======================================");
        System.out.println("[短信验证码] 手机号: " + phone + "  验证码: " + code);
        System.out.println("======================================");
        return code;
    }

    @Override
    @Transactional
    public User register(RegisterRequest req) {
        String username = req.getUsername();
        String password = req.getPassword();

        if (StrUtil.isBlank(username)) throw new RuntimeException("账号不能为空");
        if (StrUtil.isBlank(password)) throw new RuntimeException("密码不能为空");
        if (username.length() < 3 || username.length() > 20) throw new RuntimeException("账号长度需3-20位");
        if (password.length() < 6) throw new RuntimeException("密码至少6位");

        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count > 0) throw new RuntimeException("该账号已被注册");

        if (StrUtil.isNotBlank(req.getPhone())) {
            Long phoneCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()));
            if (phoneCount > 0) throw new RuntimeException("该手机号已被注册");
            // 可选校验：传了 smsCode 则校验短信验证码，不传则兼容旧前端
            if (StrUtil.isNotBlank(req.getSmsCode())) {
                Object cached = redisTemplate.opsForValue().get(SMS_PREFIX + req.getPhone());
                if (cached == null) throw new RuntimeException("验证码已过期，请重新获取");
                if (!cached.toString().equals(req.getSmsCode())) throw new RuntimeException("验证码错误");
                redisTemplate.delete(SMS_PREFIX + req.getPhone());
            }
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setNickname(StrUtil.isNotBlank(req.getNickname()) ? req.getNickname() : username);
        user.setPhone(req.getPhone());
        user.setRole("USER");
        user.setStatus(1);
        // 演示环境：新用户赠送体验余额，便于体验完整交易闭环
        user.setBalance(new BigDecimal("1000"));
        userMapper.insert(user);

        // 赠送余额记一条流水
        fundFlowService.record(user.getId(), "RECHARGE", user.getBalance(), user.getBalance(),
                "新用户注册赠送体验金", null);

        System.out.println("===== 注册成功：" + username + " =====");
        return user;
    }

    @Override
    public Map<String, Object> login(LoginRequest req) {
        // 手机号 + 验证码登录
        if (StrUtil.isNotBlank(req.getPhone())) {
            if (StrUtil.isBlank(req.getSmsCode())) throw new RuntimeException("请输入验证码");
            Object cached = redisTemplate.opsForValue().get(SMS_PREFIX + req.getPhone());
            if (cached == null) throw new RuntimeException("验证码已过期，请重新获取");
            if (!cached.toString().equals(req.getSmsCode())) throw new RuntimeException("验证码错误");

            User user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()));
            if (user == null) throw new RuntimeException("该手机号尚未注册");
            if (user.getStatus() == 0) throw new RuntimeException("账号已被禁用");
            return buildLoginResult(user);
        }

        // 账号密码登录
        String username = req.getUsername();
        String password = req.getPassword();
        if (StrUtil.isBlank(username)) throw new RuntimeException("请输入账号");
        if (StrUtil.isBlank(password)) throw new RuntimeException("请输入密码");

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) throw new RuntimeException("账号不存在");
        if (user.getStatus() == 0) throw new RuntimeException("账号已被禁用");
        if (!BCrypt.checkpw(password, user.getPassword())) throw new RuntimeException("密码错误");

        return buildLoginResult(user);
    }

    private Map<String, Object> buildLoginResult(User user) {
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        user.setPassword(null);
        result.put("user", user);
        return result;
    }

    @Override
    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user != null) user.setPassword(null);
        return user;
    }

    @Override
    public User getCurrentUser(Long userId) {
        return getById(userId);
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UserProfileRequest req) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        if (StrUtil.isNotBlank(req.getNickname())) user.setNickname(req.getNickname());
        if (StrUtil.isNotBlank(req.getPhone())) {
            Long c = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()).ne(User::getId, userId));
            if (c > 0) throw new RuntimeException("该手机号已被绑定");
            user.setPhone(req.getPhone());
        }
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, PasswordRequest req) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        if (!BCrypt.checkpw(req.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        if (req.getNewPassword() == null || req.getNewPassword().length() < 6) {
            throw new RuntimeException("新密码至少6位");
        }
        user.setPassword(BCrypt.hashpw(req.getNewPassword(), BCrypt.gensalt()));
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void updateAvatar(Long userId, String avatarUrl) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        user.setAvatar(avatarUrl);
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public Map<String, Object> withdraw(Long userId, WithdrawRequest req) {
        BigDecimal amount = req.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("提现金额必须大于0");
        }
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        if (user.getStatus() == 0) throw new RuntimeException("账号已被禁用");
        if (user.getBalance().compareTo(amount) < 0) throw new RuntimeException("余额不足");

        user.setBalance(user.getBalance().subtract(amount));
        userMapper.updateById(user);
        fundFlowService.record(userId, "WITHDRAW", amount, user.getBalance(), "余额提现", null);

        Map<String, Object> result = new HashMap<>();
        result.put("balance", user.getBalance());
        return result;
    }

    @Override
    public List<org.example.gwtzsc.entity.FundFlow> getFundFlows(Long userId) {
        return fundFlowService.listByUser(userId);
    }

    @Override
    public Map<String, Object> getSellerStats(Long userId) {
        Long sold = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getSellerId, userId)
                        .eq(Order::getStatus, "已完成"));
        Long onSale = itemMapper.selectCount(
                new LambdaQueryWrapper<Item>()
                        .eq(Item::getUserId, userId)
                        .eq(Item::getStatus, "在售"));
        Map<String, Object> m = new HashMap<>();
        m.put("soldCount", sold);
        m.put("onSaleCount", onSale);
        return m;
    }
}