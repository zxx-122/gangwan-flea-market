package org.example.gwtzsc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.gwtzsc.entity.ItemImage;
import org.example.gwtzsc.mapper.ItemImageMapper;
import org.example.gwtzsc.service.ItemImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ItemImageServiceImpl implements ItemImageService {

    @Autowired
    private ItemImageMapper itemImageMapper;

    @Override
    @Transactional
    public void saveImages(Long itemId, List<String> urls) {
        if (urls == null || urls.isEmpty()) return;
        for (int i = 0; i < urls.size(); i++) {
            ItemImage image = new ItemImage();
            image.setItemId(itemId);
            image.setUrl(urls.get(i));
            image.setSort(i);
            itemImageMapper.insert(image);
        }
    }

    @Override
    public List<String> getImages(Long itemId) {
        List<ItemImage> list = itemImageMapper.selectList(
                new LambdaQueryWrapper<ItemImage>()
                        .eq(ItemImage::getItemId, itemId)
                        .orderByAsc(ItemImage::getSort));
        return list.stream().map(ItemImage::getUrl).collect(Collectors.toList());
    }

    @Override
    public Map<Long, List<String>> getImagesByItemIds(Collection<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) return Collections.emptyMap();
        List<ItemImage> list = itemImageMapper.selectList(
                new LambdaQueryWrapper<ItemImage>()
                        .in(ItemImage::getItemId, itemIds)
                        .orderByAsc(ItemImage::getSort));
        Map<Long, List<String>> result = new HashMap<>();
        for (ItemImage img : list) {
            result.computeIfAbsent(img.getItemId(), k -> new ArrayList<>()).add(img.getUrl());
        }
        return result;
    }

    @Override
    @Transactional
    public void deleteByItemId(Long itemId) {
        itemImageMapper.delete(new LambdaQueryWrapper<ItemImage>().eq(ItemImage::getItemId, itemId));
    }
}
