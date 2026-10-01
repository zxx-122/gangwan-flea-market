package org.example.gwtzsc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.gwtzsc.common.Result;
import org.example.gwtzsc.entity.Category;
import org.example.gwtzsc.service.CategoryService;
import org.example.gwtzsc.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private UserService userService;

    private void checkAdmin(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var user = userService.getById(userId);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("无管理员权限");
        }
    }

    @GetMapping("/list")
    public Result<List<Category>> list() {
        return Result.success(categoryService.getList());
    }

    @PostMapping
    public Result<?> create(HttpServletRequest request, @RequestBody Category category) {
        checkAdmin(request);
        categoryService.create(category);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<?> update(HttpServletRequest request, @PathVariable Integer id, @RequestBody Category category) {
        checkAdmin(request);
        category.setId(id);
        categoryService.update(category);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(HttpServletRequest request, @PathVariable Integer id) {
        checkAdmin(request);
        categoryService.delete(id);
        return Result.success();
    }
}