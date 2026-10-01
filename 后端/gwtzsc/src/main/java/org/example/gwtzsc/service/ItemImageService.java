package org.example.gwtzsc.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ItemImageService {
    void saveImages(Long itemId, List<String> urls);
    List<String> getImages(Long itemId);
    /** 批量查询多个商品的图片，返回 itemId -> 按 sort 排序的图片 URL 列表 */
    Map<Long, List<String>> getImagesByItemIds(Collection<Long> itemIds);
    void deleteByItemId(Long itemId);
}
