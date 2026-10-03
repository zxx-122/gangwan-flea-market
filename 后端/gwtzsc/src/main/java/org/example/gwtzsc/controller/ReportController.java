package org.example.gwtzsc.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.example.gwtzsc.common.Result;
import org.example.gwtzsc.dto.PageResult;
import org.example.gwtzsc.entity.Report;
import org.example.gwtzsc.mapper.ReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Autowired private ReportMapper reportMapper;
    @Autowired private org.example.gwtzsc.mapper.UserMapper userMapper;

    private boolean isAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role != null && "ADMIN".equals(String.valueOf(role));
    }

    /** 提交举报（登录用户；游客拦截由 JWT 完成） */
    @PostMapping
    public Result<?> create(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        String targetType = String.valueOf(body.getOrDefault("targetType", "ITEM"));
        if (!"ITEM".equals(targetType) && !"USER".equals(targetType)) return Result.error("举报对象类型无效");
        Object tid = body.get("targetId");
        if (tid == null) return Result.error("缺少举报对象");
        Long targetId;
        try {
            targetId = Long.valueOf(String.valueOf(tid));
        } catch (Exception e) {
            return Result.error("举报对象无效");
        }
        String reason = body.get("reason") == null ? "" : String.valueOf(body.get("reason")).trim();
        if (reason.isEmpty()) return Result.error("请填写举报原因");
        if (reason.length() > 500) return Result.error("举报原因最多500字");

        // 同一用户对同一对象只保留一条待处理举报
        Long dup = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, userId)
                .eq(Report::getTargetType, targetType)
                .eq(Report::getTargetId, targetId)
                .eq(Report::getStatus, "待处理"));
        if (dup > 0) return Result.error("您已举报过该对象，请耐心等待处理");

        Report r = new Report();
        r.setReporterId(userId);
        r.setTargetType(targetType);
        r.setTargetId(targetId);
        r.setReason(reason);
        r.setStatus("待处理");
        reportMapper.insert(r);
        return Result.success();
    }

    /** 举报列表（管理员），支持状态筛选 */
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(HttpServletRequest request,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(required = false) String status) {
        if (!isAdmin(request)) return Result.error("无权限");
        Page<Report> p = new Page<>(page, size);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>()
                .eq(status != null && !status.isBlank(), Report::getStatus, status)
                .orderByDesc(Report::getCreatedAt);
        reportMapper.selectPage(p, wrapper);

        Set<Long> userIds = p.getRecords().stream().map(Report::getReporterId).collect(Collectors.toSet());
        Map<Long, org.example.gwtzsc.entity.User> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(org.example.gwtzsc.entity.User::getId, u -> u));

        List<Map<String, Object>> list = p.getRecords().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("targetType", r.getTargetType());
            m.put("targetId", r.getTargetId());
            m.put("reason", r.getReason());
            m.put("status", r.getStatus());
            m.put("createdAt", r.getCreatedAt());
            org.example.gwtzsc.entity.User u = users.get(r.getReporterId());
            m.put("reporterName", u != null ? (u.getNickname() != null ? u.getNickname() : u.getUsername()) : "未知用户");
            return m;
        }).collect(Collectors.toList());

        return Result.success(org.example.gwtzsc.dto.PageResult.of(p.getTotal(), page, size, list));
    }

    /** 处理举报（管理员标记） */
    @PutMapping("/{id}/handle")
    public Result<?> handle(HttpServletRequest request, @PathVariable Long id) {
        if (!isAdmin(request)) return Result.error("无权限");
        Report r = reportMapper.selectById(id);
        if (r == null) return Result.error("举报不存在");
        r.setStatus("已处理");
        reportMapper.updateById(r);
        return Result.success();
    }

    /** 删除举报记录（管理员） */
    @DeleteMapping("/{id}")
    public Result<?> delete(HttpServletRequest request, @PathVariable Long id) {
        if (!isAdmin(request)) return Result.error("无权限");
        reportMapper.deleteById(id);
        return Result.success();
    }
}
