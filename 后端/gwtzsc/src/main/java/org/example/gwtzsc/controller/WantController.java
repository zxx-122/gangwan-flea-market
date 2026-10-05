package org.example.gwtzsc.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.example.gwtzsc.common.Result;
import org.example.gwtzsc.dto.PageResult;
import org.example.gwtzsc.entity.Want;
import org.example.gwtzsc.mapper.WantMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/want")
public class WantController {

    @Autowired private WantMapper wantMapper;
    @Autowired private org.example.gwtzsc.mapper.UserMapper userMapper;
    @Autowired private org.example.gwtzsc.mapper.CategoryMapper categoryMapper;

    private Map<String, Object> decorate(Want w) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", w.getId());
        m.put("userId", w.getUserId());
        m.put("title", w.getTitle());
        m.put("description", w.getDescription());
        m.put("priceMax", w.getPriceMax());
        m.put("categoryId", w.getCategoryId());
        if (w.getCategoryId() != null) {
            var cat = categoryMapper.selectById(w.getCategoryId());
            m.put("categoryName", cat != null ? cat.getName() : "");
        } else {
            m.put("categoryName", "");
        }
        m.put("status", w.getStatus());
        m.put("createdAt", w.getCreatedAt());
        return m;
    }

    /** 求购列表（公开，只展示求购中） */
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(required = false) Integer categoryId,
                                                        @RequestParam(required = false) String keyword) {
        Page<Want> p = new Page<>(page, size);
        LambdaQueryWrapper<Want> wrapper = new LambdaQueryWrapper<Want>()
                .eq(Want::getStatus, "求购中")
                .orderByDesc(Want::getCreatedAt);
        if (categoryId != null) wrapper.eq(Want::getCategoryId, categoryId);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Want::getTitle, keyword).or().like(Want::getDescription, keyword));
        }
        wantMapper.selectPage(p, wrapper);

        Set<Long> userIds = p.getRecords().stream().map(Want::getUserId).collect(Collectors.toSet());
        Map<Long, org.example.gwtzsc.entity.User> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(org.example.gwtzsc.entity.User::getId, u -> u));

        List<Map<String, Object>> list = p.getRecords().stream().map(w -> {
            Map<String, Object> m = decorate(w);
            org.example.gwtzsc.entity.User u = users.get(w.getUserId());
            m.put("publisherName", u != null ? (u.getNickname() != null ? u.getNickname() : u.getUsername()) : "同学");
            m.put("publisherAvatar", u != null ? u.getAvatar() : null);
            return m;
        }).collect(Collectors.toList());

        return Result.success(PageResult.of(p.getTotal(), page, size, list));
    }

    /** 我的求购（登录） */
    @GetMapping("/mine")
    public Result<?> mine(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Want> list = wantMapper.selectList(new LambdaQueryWrapper<Want>()
                .eq(Want::getUserId, userId)
                .orderByDesc(Want::getCreatedAt));
        return Result.success(list.stream().map(this::decorate).collect(Collectors.toList()));
    }

    /** 发布求购 */
    @PostMapping
    public Result<?> create(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        String title = body.get("title") == null ? "" : String.valueOf(body.get("title")).trim();
        if (title.isEmpty()) return Result.error("请填写求购标题");
        if (title.length() > 100) return Result.error("标题最多100字");
        String desc = body.get("description") == null ? "" : String.valueOf(body.get("description")).trim();
        if (desc.length() > 500) return Result.error("描述最多500字");

        Want w = new Want();
        w.setUserId(userId);
        w.setTitle(title);
        w.setDescription(desc);
        if (body.get("priceMax") != null && !String.valueOf(body.get("priceMax")).isBlank()) {
            try {
                w.setPriceMax(new java.math.BigDecimal(String.valueOf(body.get("priceMax"))));
            } catch (Exception e) {
                return Result.error("期望价格格式不正确");
            }
        }
        if (body.get("categoryId") != null && !String.valueOf(body.get("categoryId")).isBlank()) {
            w.setCategoryId(Integer.valueOf(String.valueOf(body.get("categoryId"))));
        }
        w.setStatus("求购中");
        wantMapper.insert(w);
        return Result.success();
    }

    /** 编辑求购（本人） */
    @PutMapping("/{id}")
    public Result<?> update(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        Want w = wantMapper.selectById(id);
        if (w == null) return Result.error("求购不存在");
        if (!w.getUserId().equals(userId)) return Result.error("无权修改");
        if (body.get("title") != null) w.setTitle(String.valueOf(body.get("title")).trim());
        if (body.get("description") != null) w.setDescription(String.valueOf(body.get("description")).trim());
        if (body.get("priceMax") != null) {
            try { w.setPriceMax(new java.math.BigDecimal(String.valueOf(body.get("priceMax")))); } catch (Exception ignored) {}
        }
        wantMapper.updateById(w);
        return Result.success();
    }

    /** 求购状态变更（本人：下架/重新求购/标记成交） */
    @PutMapping("/{id}/status")
    public Result<?> setStatus(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        Want w = wantMapper.selectById(id);
        if (w == null) return Result.error("求购不存在");
        if (!w.getUserId().equals(userId)) return Result.error("无权操作");
        String status = body.getOrDefault("status", "");
        if (!List.of("求购中", "已成交", "已下架").contains(status)) return Result.error("状态无效");
        w.setStatus(status);
        wantMapper.updateById(w);
        return Result.success();
    }
}
