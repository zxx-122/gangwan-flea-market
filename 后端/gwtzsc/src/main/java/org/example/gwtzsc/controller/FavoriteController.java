package org.example.gwtzsc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.gwtzsc.common.Result;
import org.example.gwtzsc.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @PostMapping
    public Result<?> add(HttpServletRequest request, @RequestBody Map<String, Long> params) {
        Long userId = (Long) request.getAttribute("userId");
        Long itemId = params.get("itemId");
        if (itemId == null) return Result.error("商品ID不能为空");
        favoriteService.add(userId, itemId);
        return Result.success();
    }

    @DeleteMapping("/{itemId}")
    public Result<?> remove(HttpServletRequest request, @PathVariable Long itemId) {
        Long userId = (Long) request.getAttribute("userId");
        favoriteService.remove(userId, itemId);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(favoriteService.getList(userId));
    }

    @GetMapping("/check")
    public Result<Boolean> check(HttpServletRequest request, @RequestParam Long itemId) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(favoriteService.check(userId, itemId));
    }
}
