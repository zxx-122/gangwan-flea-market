package org.example.gwtzsc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.gwtzsc.entity.Favorite;
import org.example.gwtzsc.entity.Item;
import org.example.gwtzsc.mapper.FavoriteMapper;
import org.example.gwtzsc.mapper.ItemMapper;
import org.example.gwtzsc.service.FavoriteService;
import org.example.gwtzsc.service.ItemImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ItemImageService itemImageService;

    @Override
    @Transactional
    public void add(Long userId, Long itemId) {
        Long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getItemId, itemId));
        if (count > 0) throw new RuntimeException("已收藏该商品");

        Favorite fav = new Favorite();
        fav.setUserId(userId);
        fav.setItemId(itemId);
        favoriteMapper.insert(fav);
    }

    @Override
    @Transactional
    public void remove(Long userId, Long itemId) {
        favoriteMapper.delete(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getItemId, itemId));
    }

    @Override
    public List<Map<String, Object>> getList(Long userId) {
        List<Favorite> favorites = favoriteMapper.selectList(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreatedAt));

        return favorites.stream().map(fav -> {
            Item item = itemMapper.selectById(fav.getItemId());
            if (item == null) return null;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.getId());
            map.put("title", item.getTitle());
            map.put("price", item.getPrice());
            map.put("status", item.getStatus());
            map.put("condition", item.getCondition());
            map.put("createdAt", item.getCreatedAt());
            List<String> images = itemImageService.getImages(item.getId());
            map.put("images", images);
            map.put("mainImage", images.isEmpty() ? null : images.get(0));
            return map;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public boolean check(Long userId, Long itemId) {
        Long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getItemId, itemId));
        return count > 0;
    }
}
