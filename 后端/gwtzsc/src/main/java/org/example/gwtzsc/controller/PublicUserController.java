package org.example.gwtzsc.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.example.gwtzsc.common.Result;
import org.example.gwtzsc.entity.Item;
import org.example.gwtzsc.entity.User;
import org.example.gwtzsc.mapper.ItemMapper;
import org.example.gwtzsc.mapper.OrderMapper;
import org.example.gwtzsc.mapper.UserMapper;
import org.example.gwtzsc.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 公开用户主页：任何人可查看某用户的在售商品与信用概况。
 */
@RestController
@RequestMapping("/api/user")
public class PublicUserController {

    @Autowired private UserMapper userMapper;
    @Autowired private ItemMapper itemMapper;
    @Autowired private OrderMapper orderMapper;
    @Autowired private ItemService itemService;

    /** 用户公开资料 + 信用概况 */
    @GetMapping("/{id}/public")
    public Result<?> publicProfile(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) return Result.error("用户不存在");

        Long sold = orderMapper.selectCount(new LambdaQueryWrapper<org.example.gwtzsc.entity.Order>()
                .eq(org.example.gwtzsc.entity.Order::getSellerId, id)
                .eq(org.example.gwtzsc.entity.Order::getStatus, "已完成"));
        Long onSale = itemMapper.selectCount(new LambdaQueryWrapper<Item>()
                .eq(Item::getUserId, id)
                .eq(Item::getStatus, "在售"));

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("nickname", user.getNickname());
        m.put("avatar", user.getAvatar());
        m.put("createdAt", user.getCreatedAt());
        m.put("soldCount", sold);
        m.put("onSaleCount", onSale);
        return Result.success(m);
    }

    /** 某用户的在售商品（公开；只展示在售，不暴露已售/下架） */
    @GetMapping("/{id}/items")
    public Result<?> publicItems(@PathVariable Long id,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "12") int size) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Item> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        itemMapper.selectPage(p, new LambdaQueryWrapper<Item>()
                .eq(Item::getUserId, id)
                .eq(Item::getStatus, "在售")
                .orderByDesc(Item::getCreatedAt));
        return Result.success(org.example.gwtzsc.dto.PageResult.of(p.getTotal(), page, size,
                itemService.decoratePublicList(p.getRecords())));
    }
}
