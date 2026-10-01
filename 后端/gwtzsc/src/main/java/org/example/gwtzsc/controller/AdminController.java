package org.example.gwtzsc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.gwtzsc.common.Result;
import org.example.gwtzsc.dto.AdminStats;
import org.example.gwtzsc.dto.PageResult;
import org.example.gwtzsc.service.AdminService;
import org.example.gwtzsc.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired private AdminService adminService;
    @Autowired private UserService userService;

    private void checkAdmin(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var user = userService.getById(userId);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("无管理员权限");
        }
    }

    @GetMapping("/stats")
    public Result<AdminStats> stats(HttpServletRequest request) {
        checkAdmin(request);
        return Result.success(adminService.getStats());
    }

    @GetMapping("/users")
    public Result<PageResult<Map<String, Object>>> users(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        checkAdmin(request);
        return Result.success(adminService.getUsers(page, size, keyword));
    }

    @PutMapping("/users/{id}/balance")
    public Result<?> setUserBalance(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> params) {
        checkAdmin(request);
        adminService.setUserBalance(id, params.get("balance"));
        return Result.success();
    }

    @PutMapping("/users/{id}/status")
    public Result<?> updateUserStatus(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody Map<String, Integer> params) {
        checkAdmin(request);
        Integer status = params.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return Result.error("状态值无效");
        }
        adminService.updateUserStatus(id, status);
        return Result.success();
    }

    @GetMapping("/items")
    public Result<PageResult<Map<String, Object>>> items(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        checkAdmin(request);
        return Result.success(adminService.getItems(page, size, keyword, status));
    }

    @PutMapping("/items/{id}/offline")
    public Result<?> offlineItem(HttpServletRequest request, @PathVariable Long id) {
        checkAdmin(request);
        adminService.offlineItem(id);
        return Result.success();
    }

    @PutMapping("/items/{id}/relist")
    public Result<?> relistItem(HttpServletRequest request, @PathVariable Long id) {
        checkAdmin(request);
        adminService.relistItem(id);
        return Result.success();
    }

    @GetMapping("/orders")
    public Result<PageResult<Map<String, Object>>> orders(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        checkAdmin(request);
        return Result.success(adminService.getOrders(page, size, status, keyword));
    }

    @DeleteMapping("/users/{id}")
    public Result<?> deleteUser(HttpServletRequest request, @PathVariable Long id) {
        checkAdmin(request);
        adminService.deleteUser(id);
        return Result.success();
    }

    @GetMapping("/revenue")
    public Result<List<Map<String, Object>>> revenue(HttpServletRequest request) {
        checkAdmin(request);
        return Result.success(adminService.getRevenue());
    }
}
