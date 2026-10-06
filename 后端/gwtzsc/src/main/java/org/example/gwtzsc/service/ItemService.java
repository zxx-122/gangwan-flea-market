package org.example.gwtzsc.service;

import org.example.gwtzsc.dto.ItemRequest;
import org.example.gwtzsc.dto.PageResult;
import org.example.gwtzsc.entity.Item;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ItemService {
    PageResult<Map<String, Object>> list(int page, int size, Integer categoryId, String keyword,
                                         BigDecimal minPrice, BigDecimal maxPrice, String condition, String sort);
    Map<String, Object> getDetail(Long id);
    Item create(Long userId, ItemRequest req);
    Item update(Long userId, Long id, ItemRequest req);
    void delete(Long userId, Long id);
    void relist(Long userId, Long id);
    PageResult<Map<String, Object>> search(String keyword, Integer categoryId, BigDecimal minPrice,
                                           BigDecimal maxPrice, String condition, String sort, int page, int size);
    PageResult<Map<String, Object>> byCategory(Integer categoryId, int page, int size);
    List<Map<String, Object>> getUserItems(Long userId);
    PageResult<Map<String, Object>> searchUserItems(Long userId, String keyword, int page, int size);
    List<Map<String, Object>> getRelated(Long id, int limit);
    Map<String, Object> getPriceRef(Integer categoryId);

    /** 公开用户主页用：商品卡片脱敏列表（只含展示字段） */
    java.util.List<Map<String, Object>> decoratePublicList(List<Item> items);
}