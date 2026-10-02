package org.example.gwtzsc.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.example.gwtzsc.common.Result;
import org.example.gwtzsc.entity.Order;
import org.example.gwtzsc.entity.Review;
import org.example.gwtzsc.entity.User;
import org.example.gwtzsc.mapper.OrderMapper;
import org.example.gwtzsc.mapper.ReviewMapper;
import org.example.gwtzsc.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    @Autowired private ReviewMapper reviewMapper;
    @Autowired private OrderMapper orderMapper;
    @Autowired private UserMapper userMapper;

    /** 买家对已完成订单评价（一单一评） */
    @PostMapping
    public Result<?> create(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        Long orderId = Long.valueOf(String.valueOf(body.get("orderId")));
        Integer rating = body.get("rating") == null ? 5 : Integer.valueOf(String.valueOf(body.get("rating")));
        String content = body.get("content") == null ? "" : String.valueOf(body.get("content")).trim();

        if (rating < 1 || rating > 5) return Result.error("评分需在1-5星之间");
        if (content.length() > 500) return Result.error("评价内容最多500字");

        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.error("订单不存在");
        if (!"已完成".equals(order.getStatus())) return Result.error("仅已完成订单可评价");
        if (!order.getBuyerId().equals(userId)) return Result.error("仅买家可评价");

        Long count = reviewMapper.selectCount(new LambdaQueryWrapper<Review>().eq(Review::getOrderId, orderId));
        if (count > 0) return Result.error("该订单已评价过");

        Review review = new Review();
        review.setOrderId(orderId);
        review.setItemId(order.getItemId());
        review.setFromUserId(userId);
        review.setToUserId(order.getSellerId());
        review.setRating(rating);
        review.setContent(content);
        reviewMapper.insert(review);
        return Result.success();
    }

    /** 某订单的评价（用于判断是否已评） */
    @GetMapping("/order/{orderId}")
    public Result<?> byOrder(@PathVariable Long orderId) {
        Review review = reviewMapper.selectOne(new LambdaQueryWrapper<Review>().eq(Review::getOrderId, orderId));
        return Result.success(review);
    }

    /** 商品的评价列表（公开） */
    @GetMapping("/item/{itemId}")
    public Result<?> byItem(@PathVariable Long itemId) {
        return Result.success(buildList(
                reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                        .eq(Review::getItemId, itemId)
                        .orderByDesc(Review::getCreatedAt))));
    }

    /** 用户收到的评价列表（公开） */
    @GetMapping("/user/{userId}")
    public Result<?> byUser(@PathVariable Long userId) {
        return Result.success(buildList(
                reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                        .eq(Review::getToUserId, userId)
                        .orderByDesc(Review::getCreatedAt))));
    }

    /** 用户收到的平均分与数量（公开，用于详情页卖家资料） */
    @GetMapping("/user/{userId}/summary")
    public Result<?> userSummary(@PathVariable Long userId) {
        List<Review> list = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getToUserId, userId));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("count", list.size());
        m.put("avgRating", list.isEmpty() ? null :
                Math.round(list.stream().mapToInt(Review::getRating).average().orElse(0) * 10) / 10.0);
        return Result.success(m);
    }

    private List<Map<String, Object>> buildList(List<Review> list) {
        Set<Long> userIds = new HashSet<>();
        list.forEach(r -> userIds.add(r.getFromUserId()));
        Map<Long, User> users = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        return list.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("orderId", r.getOrderId());
            m.put("itemId", r.getItemId());
            m.put("rating", r.getRating());
            m.put("content", r.getContent());
            m.put("createdAt", r.getCreatedAt());
            User u = users.get(r.getFromUserId());
            Map<String, Object> um = new LinkedHashMap<>();
            um.put("id", r.getFromUserId());
            um.put("nickname", u != null ? u.getNickname() : "匿名");
            um.put("avatar", u != null ? u.getAvatar() : null);
            m.put("user", um);
            return m;
        }).collect(Collectors.toList());
    }
}
