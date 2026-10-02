package org.example.gwtzsc.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.example.gwtzsc.common.Result;
import org.example.gwtzsc.entity.Announcement;
import org.example.gwtzsc.mapper.AnnouncementMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcement")
public class AnnouncementController {

    @Autowired private AnnouncementMapper announcementMapper;

    /** 公告列表（公开；admin=true 时返回全部，否则只返回启用中的） */
    @GetMapping("/list")
    public Result<List<Announcement>> list(@RequestParam(required = false) String admin,
                                           HttpServletRequest request) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getCreatedAt);
        if (!"true".equals(admin)) {
            wrapper.eq(Announcement::getEnabled, 1);
        }
        return Result.success(announcementMapper.selectList(wrapper));
    }

    private boolean isAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role != null && "ADMIN".equals(String.valueOf(role));
    }

    @PostMapping
    public Result<?> create(HttpServletRequest request, @RequestBody Announcement a) {
        if (!isAdmin(request)) return Result.error("无权限");
        if (a.getTitle() == null || a.getTitle().isBlank()) return Result.error("标题不能为空");
        if (a.getEnabled() == null) a.setEnabled(1);
        announcementMapper.insert(a);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<?> update(HttpServletRequest request, @PathVariable Long id, @RequestBody Announcement a) {
        if (!isAdmin(request)) return Result.error("无权限");
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) return Result.error("公告不存在");
        if (a.getTitle() != null) existing.setTitle(a.getTitle());
        if (a.getContent() != null) existing.setContent(a.getContent());
        if (a.getEnabled() != null) existing.setEnabled(a.getEnabled());
        announcementMapper.updateById(existing);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(HttpServletRequest request, @PathVariable Long id) {
        if (!isAdmin(request)) return Result.error("无权限");
        announcementMapper.deleteById(id);
        return Result.success();
    }
}
