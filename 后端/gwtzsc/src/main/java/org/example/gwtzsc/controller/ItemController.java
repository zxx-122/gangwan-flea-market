package org.example.gwtzsc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.gwtzsc.common.Result;
import org.example.gwtzsc.dto.ItemRequest;
import org.example.gwtzsc.dto.PageResult;
import org.example.gwtzsc.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/item")
public class ItemController {

    @Autowired
    private ItemService itemService;

    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String sort) {
        return Result.success(itemService.list(page, size, categoryId, keyword, minPrice, maxPrice, condition, sort));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(itemService.getDetail(id));
    }

    @GetMapping("/{id}/related")
    public Result<List<Map<String, Object>>> related(@PathVariable Long id,
                                                     @RequestParam(defaultValue = "8") int limit) {
        return Result.success(itemService.getRelated(id, Math.min(limit, 20)));
    }

    @GetMapping("/price-ref")
    public Result<Map<String, Object>> priceRef(@RequestParam(required = false) Integer categoryId) {
        return Result.success(itemService.getPriceRef(categoryId));
    }

    @PostMapping
    public Result<?> create(HttpServletRequest request, @RequestBody @jakarta.validation.Valid ItemRequest req) {
        Long userId = (Long) request.getAttribute("userId");
        itemService.create(userId, req);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<?> update(HttpServletRequest request, @PathVariable Long id, @RequestBody @jakarta.validation.Valid ItemRequest req) {
        Long userId = (Long) request.getAttribute("userId");
        itemService.update(userId, id, req);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        itemService.delete(userId, id);
        return Result.success();
    }

    @PutMapping("/{id}/relist")
    public Result<?> relist(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        itemService.relist(userId, id);
        return Result.success();
    }

    @GetMapping("/search")
    public Result<PageResult<Map<String, Object>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(itemService.search(keyword, categoryId, minPrice, maxPrice, condition, sort, page, size));
    }

    @GetMapping("/category/{id}")
    public Result<PageResult<Map<String, Object>>> byCategory(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(itemService.byCategory(id, page, size));
    }
}
