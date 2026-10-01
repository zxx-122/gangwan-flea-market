package org.example.gwtzsc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.gwtzsc.common.Result;
import org.example.gwtzsc.dto.OrderRequest;
import org.example.gwtzsc.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public Result<?> create(HttpServletRequest request, @RequestBody @jakarta.validation.Valid OrderRequest req) {
        Long userId = (Long) request.getAttribute("userId");
        orderService.create(userId, req);
        return Result.success();
    }

    @GetMapping("/buyer")
    public Result<List<Map<String, Object>>> buyerOrders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(orderService.getBuyerOrders(userId));
    }

    @GetMapping("/seller")
    public Result<List<Map<String, Object>>> sellerOrders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(orderService.getSellerOrders(userId));
    }

    @PutMapping("/{id}/ship")
    public Result<?> ship(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        orderService.ship(userId, id, body.get("courierCompany"), body.get("trackingNo"));
        return Result.success();
    }

    @PutMapping("/{id}/confirm")
    public Result<?> confirm(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        orderService.confirm(userId, id);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    public Result<?> cancel(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        orderService.cancel(userId, id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<?> detail(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        org.example.gwtzsc.entity.Order order = orderService.getById(id);
        if (order == null) return Result.error("订单不存在");
        // 仅订单买卖双方可见，防止越权查看他人收货信息
        if (!userId.equals(order.getBuyerId()) && !userId.equals(order.getSellerId())) {
            return Result.error("无权查看此订单");
        }
        return Result.success(order);
    }
}
