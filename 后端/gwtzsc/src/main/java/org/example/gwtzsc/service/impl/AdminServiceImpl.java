package org.example.gwtzsc.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.gwtzsc.dto.AdminStats;
import org.example.gwtzsc.dto.PageResult;
import org.example.gwtzsc.entity.Item;
import org.example.gwtzsc.entity.Order;
import org.example.gwtzsc.entity.User;
import org.example.gwtzsc.mapper.ItemMapper;
import org.example.gwtzsc.mapper.OrderMapper;
import org.example.gwtzsc.mapper.UserMapper;
import org.example.gwtzsc.service.AdminService;
import org.example.gwtzsc.service.ItemImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired private UserMapper userMapper;
    @Autowired private ItemMapper itemMapper;
    @Autowired private OrderMapper orderMapper;
    @Autowired private ItemImageService itemImageService;
    @Autowired private org.example.gwtzsc.service.FundFlowService fundFlowService;

    @Override
    public AdminStats getStats() {
        AdminStats stats = new AdminStats();
        stats.setUserCount(userMapper.selectCount(null));
        stats.setItemCount(itemMapper.selectCount(null));
        stats.setOrderCount(orderMapper.selectCount(null));

        BigDecimal totalCommission = orderMapper.selectList(
            new LambdaQueryWrapper<Order>().eq(Order::getStatus, "已完成")
        ).stream().map(o -> o.getCommission() != null ? o.getCommission() : BigDecimal.ZERO)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalCommission(totalCommission);

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        BigDecimal todayCommission = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, "已完成")
                .ge(Order::getCreatedAt, todayStart)
        ).stream().map(o -> o.getCommission() != null ? o.getCommission() : BigDecimal.ZERO)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTodayCommission(todayCommission);

        long todayOrders = orderMapper.selectCount(
            new LambdaQueryWrapper<Order>().ge(Order::getCreatedAt, todayStart));
        stats.setTodayOrders(todayOrders);

        return stats;
    }

    @Override
    public PageResult<Map<String, Object>> getUsers(int page, int size, String keyword) {
        Page<User> userPage = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(User::getUsername, keyword).or().like(User::getNickname, keyword);
        }
        wrapper.orderByDesc(User::getCreatedAt);
        userMapper.selectPage(userPage, wrapper);

        List<Map<String, Object>> list = userPage.getRecords().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            m.put("phone", u.getPhone());
            m.put("role", u.getRole());
            m.put("balance", u.getBalance());
            m.put("status", u.getStatus());
            m.put("createdAt", u.getCreatedAt());
            return m;
        }).collect(Collectors.toList());

        return PageResult.of(userPage.getTotal(), page, size, list);
    }

    @Override
    @Transactional
    public void updateUserStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) throw new RuntimeException("用户不存在");
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void setUserBalance(Long id, BigDecimal balance) {
        if (balance == null) throw new RuntimeException("余额不能为空");
        if (balance.compareTo(BigDecimal.ZERO) < 0) throw new RuntimeException("余额不能为负数");
        User user = userMapper.selectById(id);
        if (user == null) throw new RuntimeException("用户不存在");
        BigDecimal oldBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        BigDecimal diff = balance.subtract(oldBalance);
        user.setBalance(balance.setScale(2, java.math.RoundingMode.HALF_UP));
        userMapper.updateById(user);
        fundFlowService.record(user.getId(), diff.compareTo(BigDecimal.ZERO) >= 0 ? "RECHARGE" : "WITHDRAW",
                diff, user.getBalance(), "管理员调整余额", null);
    }

    @Override
    public PageResult<Map<String, Object>> getItems(int page, int size, String keyword, String status) {
        Page<Item> itemPage = new Page<>(page, size);
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(Item::getStatus, status);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(Item::getTitle, keyword);
        }
        wrapper.orderByDesc(Item::getCreatedAt);
        itemMapper.selectPage(itemPage, wrapper);

        List<Item> records = itemPage.getRecords();
        // 批量查询图片与卖家，避免逐行查询（N+1）
        Map<Long, List<String>> imagesByItem = itemImageService.getImagesByItemIds(
                records.stream().map(Item::getId).collect(Collectors.toList()));
        Map<Long, String> namesByUser = displayNames(records.stream().map(Item::getUserId).collect(Collectors.toSet()));

        List<Map<String, Object>> list = records.stream().map(item -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", item.getId());
            m.put("userId", item.getUserId());
            m.put("title", item.getTitle());
            m.put("price", item.getPrice());
            m.put("status", item.getStatus());
            m.put("condition", item.getCondition());
            m.put("views", item.getViews());
            m.put("createdAt", item.getCreatedAt());
            List<String> images = imagesByItem.getOrDefault(item.getId(), Collections.emptyList());
            m.put("mainImage", images.isEmpty() ? null : images.get(0));
            m.put("sellerName", namesByUser.getOrDefault(item.getUserId(), ""));
            return m;
        }).collect(Collectors.toList());

        return PageResult.of(itemPage.getTotal(), page, size, list);
    }

    @Override
    @Transactional
    public void offlineItem(Long id) {
        Item item = itemMapper.selectById(id);
        if (item == null) throw new RuntimeException("商品不存在");
        item.setStatus("下架");
        itemMapper.updateById(item);
    }

    @Override
    @Transactional
    public void relistItem(Long id) {
        Item item = itemMapper.selectById(id);
        if (item == null) throw new RuntimeException("商品不存在");
        if (!"下架".equals(item.getStatus())) throw new RuntimeException("仅下架商品可重新上架");
        item.setStatus("在售");
        itemMapper.updateById(item);
    }

    @Override
    public PageResult<Map<String, Object>> getOrders(int page, int size, String status, String keyword) {
        Page<Order> orderPage = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Order::getOrderNo, keyword)
                    .or().like(Order::getReceiverName, keyword)
                    .or().like(Order::getReceiverPhone, keyword));
        }
        orderMapper.selectPage(orderPage, wrapper);

        List<Order> orders = orderPage.getRecords();
        // 批量查询商品与买卖双方用户，避免逐行查询（N+1）
        Set<Long> userIds = new HashSet<>();
        orders.forEach(o -> { userIds.add(o.getSellerId()); userIds.add(o.getBuyerId()); });
        Map<Long, String> namesByUser = displayNames(userIds);
        Map<Long, Item> itemsById = orders.isEmpty() ? Collections.emptyMap()
                : itemMapper.selectBatchIds(orders.stream().map(Order::getItemId).collect(Collectors.toSet()))
                    .stream().collect(Collectors.toMap(Item::getId, i -> i));

        List<Map<String, Object>> list = orders.stream().map(order -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", order.getId());
            m.put("orderNo", order.getOrderNo());
            m.put("itemId", order.getItemId());
            m.put("sellerId", order.getSellerId());
            m.put("buyerId", order.getBuyerId());
            m.put("price", order.getPrice());
            m.put("commission", order.getCommission());
            m.put("sellerIncome", order.getSellerIncome());
            m.put("status", order.getStatus());
            m.put("receiverName", order.getReceiverName());
            m.put("receiverPhone", order.getReceiverPhone());
            m.put("receiverAddress", order.getReceiverAddress());
            m.put("courierCompany", order.getCourierCompany());
            m.put("trackingNo", order.getTrackingNo());
            m.put("createdAt", order.getCreatedAt());

            Item item = itemsById.get(order.getItemId());
            if (item != null) m.put("itemTitle", item.getTitle());

            m.put("sellerName", namesByUser.getOrDefault(order.getSellerId(), ""));
            m.put("buyerName", namesByUser.getOrDefault(order.getBuyerId(), ""));

            return m;
        }).collect(Collectors.toList());

        return PageResult.of(orderPage.getTotal(), page, size, list);
    }

    /** 批量取用户昵称/用户名显示名 */
    private Map<Long, String> displayNames(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId,
                        u -> u.getNickname() != null ? u.getNickname() : u.getUsername()));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new RuntimeException("用户不存在");
        if ("ADMIN".equals(user.getRole())) throw new RuntimeException("不能删除管理员账号");
        userMapper.deleteById(id);
    }

    @Override
    public List<Map<String, Object>> getRevenue() {
        List<Order> orders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, "已完成")
                .orderByDesc(Order::getCreatedAt));

        // group by date
        Map<LocalDate, BigDecimal> dailyMap = new LinkedHashMap<>();
        for (Order o : orders) {
            LocalDate date = o.getCreatedAt().toLocalDate();
            dailyMap.merge(date, o.getCommission() != null ? o.getCommission() : BigDecimal.ZERO, BigDecimal::add);
        }

        return dailyMap.entrySet().stream()
            .sorted(Map.Entry.<LocalDate, BigDecimal>comparingByKey().reversed())
            .map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("date", e.getKey().toString());
                m.put("commission", e.getValue());
                return m;
            }).collect(Collectors.toList());
    }
}

