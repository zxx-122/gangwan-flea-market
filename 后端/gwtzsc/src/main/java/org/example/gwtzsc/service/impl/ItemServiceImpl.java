package org.example.gwtzsc.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.gwtzsc.dto.ItemRequest;
import org.example.gwtzsc.dto.PageResult;
import org.example.gwtzsc.entity.Item;
import org.example.gwtzsc.entity.User;
import org.example.gwtzsc.mapper.ItemMapper;
import org.example.gwtzsc.mapper.UserMapper;
import org.example.gwtzsc.service.ItemImageService;
import org.example.gwtzsc.service.ItemService;
import org.example.gwtzsc.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ItemImageService itemImageService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private org.example.gwtzsc.mapper.CategoryMapper categoryMapper;

    @Autowired
    private UserService userService;

    private static final String ITEM_CACHE_PREFIX = "item:";
    private static final String HOME_CACHE_PREFIX = "home:items";

    @Override
    @SuppressWarnings("unchecked")
    public PageResult<Map<String, Object>> list(int page, int size, Integer categoryId, String keyword,
                                                BigDecimal minPrice, BigDecimal maxPrice, String condition, String sort) {
        String cacheKey = buildHomeCacheKey(page, size, categoryId, keyword, minPrice, maxPrice, condition, sort);
        PageResult<Map<String, Object>> cached = (PageResult<Map<String, Object>>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return cached;

        Page<Item> itemPage = new Page<>(page, size);
        LambdaQueryWrapper<Item> wrapper = buildListWrapper(keyword, categoryId, minPrice, maxPrice, condition);
        applySort(wrapper, sort);
        itemMapper.selectPage(itemPage, wrapper);
        PageResult<Map<String, Object>> result = PageResult.of(itemPage.getTotal(), page, size, buildItemList(itemPage.getRecords()));
        redisTemplate.opsForValue().set(cacheKey, result, 5, TimeUnit.MINUTES);
        return result;
    }

    private String buildHomeCacheKey(int page, int size, Integer categoryId, String keyword,
                                     BigDecimal minPrice, BigDecimal maxPrice, String condition, String sort) {
        return HOME_CACHE_PREFIX + ":" + page + ":" + size + ":"
                + nvl(categoryId) + ":" + nvl(keyword) + ":" + nvl(minPrice) + ":"
                + nvl(maxPrice) + ":" + nvl(condition) + ":" + nvl(sort);
    }

    private String nvl(Object o) {
        return o == null ? "-" : o.toString();
    }

    /** 清除首页商品列表缓存（home:items 前缀的所有 key） */
    public void clearHomeItemCache() {
        Set<String> keys = redisTemplate.keys(HOME_CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private LambdaQueryWrapper<Item> buildListWrapper(String keyword, Integer categoryId,
                                                      BigDecimal minPrice, BigDecimal maxPrice, String condition) {
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<Item>()
                .eq(Item::getStatus, "在售");
        if (categoryId != null) wrapper.eq(Item::getCategoryId, categoryId);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Item::getTitle, keyword).or().like(Item::getDescription, keyword));
        }
        if (minPrice != null) wrapper.ge(Item::getPrice, minPrice);
        if (maxPrice != null) wrapper.le(Item::getPrice, maxPrice);
        if (StrUtil.isNotBlank(condition)) wrapper.eq(Item::getCondition, condition);
        return wrapper;
    }

    private void applySort(LambdaQueryWrapper<Item> wrapper, String sort) {
        if (sort == null) sort = "new";
        switch (sort) {
            case "hot" -> wrapper.orderByDesc(Item::getViews).orderByDesc(Item::getCreatedAt);
            case "price_asc" -> wrapper.orderByAsc(Item::getPrice);
            case "price_desc" -> wrapper.orderByDesc(Item::getPrice);
            default -> wrapper.orderByDesc(Item::getCreatedAt);
        }
    }

    @Override
    public Map<String, Object> getDetail(Long id) {
        String cacheKey = ITEM_CACHE_PREFIX + id;
        @SuppressWarnings("unchecked")
        Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            Item item = itemMapper.selectById(id);
            if (item != null) {
                int newViews = (item.getViews() == null ? 0 : item.getViews()) + 1;
                item.setViews(newViews);
                itemMapper.updateById(item);
                cached.put("views", newViews);
            }
            return cached;
        }

        Item item = itemMapper.selectById(id);
        if (item == null) throw new RuntimeException("商品不存在");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("userId", item.getUserId());
        result.put("categoryId", item.getCategoryId());
        if (item.getCategoryId() != null) {
            org.example.gwtzsc.entity.Category cat = categoryMapper.selectById(item.getCategoryId());
            result.put("categoryName", cat != null ? cat.getName() : "");
        } else {
            result.put("categoryName", "");
        }
        result.put("title", item.getTitle());
        result.put("description", item.getDescription());
        result.put("price", item.getPrice());
        result.put("condition", item.getCondition());
        result.put("tags", item.getTags());
        result.put("isOriginal", item.getIsOriginal());
        result.put("isFreeShip", item.getIsFreeShip());
        result.put("status", item.getStatus());
        result.put("views", item.getViews());
        result.put("stock", item.getStock() == null ? 1 : item.getStock());
        result.put("createdAt", item.getCreatedAt());
        result.put("updatedAt", item.getUpdatedAt());

        List<String> imageUrls = itemImageService.getImages(id);
        result.put("images", imageUrls.stream().map(url -> {
            Map<String, String> img = new LinkedHashMap<>();
            img.put("url", url);
            return img;
        }).collect(Collectors.toList()));

        // 卖家信息 + 卖家数据
        User seller = userMapper.selectById(item.getUserId());
        if (seller != null) {
            Map<String, Object> sellerInfo = new LinkedHashMap<>();
            sellerInfo.put("id", seller.getId());
            sellerInfo.put("username", seller.getUsername());
            sellerInfo.put("nickname", seller.getNickname());
            sellerInfo.put("avatar", seller.getAvatar());
            Map<String, Object> stats = userService.getSellerStats(seller.getId());
            long sold = ((Number) stats.getOrDefault("soldCount", 0)).longValue();
            double rating = sold > 0 ? 100.0 : 90.0;
            stats.put("rating", Math.round(rating * 10) / 10.0);
            stats.put("replyRate", 100.0);
            sellerInfo.put("stats", stats);
            result.put("seller", sellerInfo);
        }

        int newViews = (item.getViews() == null ? 0 : item.getViews()) + 1;
        item.setViews(newViews);
        itemMapper.updateById(item);
        result.put("views", newViews);

        redisTemplate.opsForValue().set(cacheKey, result, 10, TimeUnit.MINUTES);
        return result;
    }

    @Override
    @Transactional
    public Item create(Long userId, ItemRequest req) {
        if (StrUtil.isBlank(req.getTitle())) throw new RuntimeException("商品标题不能为空");
        if (req.getTitle().length() > 100) throw new RuntimeException("标题最多100字");
        if (req.getPrice() == null || req.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("价格必须大于0");
        }
        if (req.getPrice().compareTo(new BigDecimal("999999")) > 0) {
            throw new RuntimeException("价格超出上限");
        }
        Item item = new Item();
        item.setUserId(userId);
        item.setCategoryId(req.getCategoryId());
        item.setTitle(req.getTitle());
        item.setDescription(req.getDescription());
        item.setPrice(req.getPrice());
        item.setCondition(req.getCondition());
        item.setTags(req.getTags());
        item.setIsOriginal(req.getIsOriginal() == null ? 1 : req.getIsOriginal());
        item.setIsFreeShip(req.getIsFreeShip() == null ? 0 : req.getIsFreeShip());
        int stock = req.getStock() == null ? 1 : req.getStock();
        if (stock < 1) throw new RuntimeException("库存至少为1");
        if (stock > 9999) throw new RuntimeException("库存最大9999");
        item.setStock(stock);
        item.setStatus("在售");
        item.setViews(0);
        itemMapper.insert(item);

        if (req.getImages() != null && !req.getImages().isEmpty()) {
            itemImageService.saveImages(item.getId(), req.getImages());
        }
        clearHomeItemCache();
        return item;
    }

    @Override
    @Transactional
    public Item update(Long userId, Long id, ItemRequest req) {
        Item item = itemMapper.selectById(id);
        if (item == null) throw new RuntimeException("商品不存在");
        if (!item.getUserId().equals(userId)) throw new RuntimeException("无权修改此商品");
        if ("已售".equals(item.getStatus())) throw new RuntimeException("已售商品无法修改");

        if (StrUtil.isNotBlank(req.getTitle())) item.setTitle(req.getTitle());
        if (req.getDescription() != null) item.setDescription(req.getDescription());
        if (req.getPrice() != null) {
            if (req.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("价格必须大于0");
            }
            item.setPrice(req.getPrice());
        }
        if (req.getCategoryId() != null) item.setCategoryId(req.getCategoryId());
        if (req.getCondition() != null) item.setCondition(req.getCondition());
        if (req.getTags() != null) item.setTags(req.getTags());
        if (req.getIsOriginal() != null) item.setIsOriginal(req.getIsOriginal());
        if (req.getIsFreeShip() != null) item.setIsFreeShip(req.getIsFreeShip());
        if (req.getStock() != null) {
            if (req.getStock() < 1) throw new RuntimeException("库存至少为1");
            if (req.getStock() > 9999) throw new RuntimeException("库存最大9999");
            // 已售商品库存只能由订单取消回补，不允许直接编辑
            if (!"已售".equals(item.getStatus())) item.setStock(req.getStock());
        }
        itemMapper.updateById(item);

        if (req.getImages() != null) {
            itemImageService.deleteByItemId(id);
            itemImageService.saveImages(id, req.getImages());
        }

        redisTemplate.delete(ITEM_CACHE_PREFIX + id);
        clearHomeItemCache();
        return item;
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        Item item = itemMapper.selectById(id);
        if (item == null) throw new RuntimeException("商品不存在");
        if (!item.getUserId().equals(userId)) throw new RuntimeException("无权操作此商品");
        if ("已售".equals(item.getStatus())) throw new RuntimeException("已售商品无法下架");

        item.setStatus("下架");
        itemMapper.updateById(item);
        redisTemplate.delete(ITEM_CACHE_PREFIX + id);
        clearHomeItemCache();
    }

    @Override
    @Transactional
    public void relist(Long userId, Long id) {
        Item item = itemMapper.selectById(id);
        if (item == null) throw new RuntimeException("商品不存在");
        if (!item.getUserId().equals(userId)) throw new RuntimeException("无权操作此商品");
        if (!"下架".equals(item.getStatus())) throw new RuntimeException("仅下架商品可重新上架");

        item.setStatus("在售");
        itemMapper.updateById(item);
        redisTemplate.delete(ITEM_CACHE_PREFIX + id);
        clearHomeItemCache();
    }

    @Override
    public PageResult<Map<String, Object>> search(String keyword, Integer categoryId, BigDecimal minPrice,
                                                  BigDecimal maxPrice, String condition, String sort, int page, int size) {
        if (StrUtil.isBlank(keyword) && categoryId == null && minPrice == null
                && maxPrice == null && StrUtil.isBlank(condition)) {
            return list(page, size, categoryId, null, null, null, null, sort);
        }
        Page<Item> itemPage = new Page<>(page, size);
        LambdaQueryWrapper<Item> wrapper = buildListWrapper(keyword, categoryId, minPrice, maxPrice, condition);
        applySort(wrapper, sort);
        itemMapper.selectPage(itemPage, wrapper);
        return PageResult.of(itemPage.getTotal(), page, size, buildItemList(itemPage.getRecords()));
    }

    @Override
    public PageResult<Map<String, Object>> byCategory(Integer categoryId, int page, int size) {
        return list(page, size, categoryId, null, null, null, null, null);
    }

    @Override
    public List<Map<String, Object>> getUserItems(Long userId) {
        List<Item> items = itemMapper.selectList(
                new LambdaQueryWrapper<Item>()
                        .eq(Item::getUserId, userId)
                        .orderByDesc(Item::getCreatedAt));
        return buildItemList(items);
    }

    @Override
    public PageResult<Map<String, Object>> searchUserItems(Long userId, String keyword, int page, int size) {
        Page<Item> itemPage = new Page<>(page, size);
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<Item>()
                .eq(Item::getUserId, userId)
                .orderByDesc(Item::getCreatedAt);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(Item::getTitle, keyword);
        }
        itemMapper.selectPage(itemPage, wrapper);
        return PageResult.of(itemPage.getTotal(), page, size, buildItemList(itemPage.getRecords()));
    }

    @Override
    public List<Map<String, Object>> getRelated(Long id, int limit) {
        Item item = itemMapper.selectById(id);
        if (item == null) return Collections.emptyList();
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<Item>()
                .eq(Item::getStatus, "在售")
                .ne(Item::getId, id);
        if (item.getCategoryId() != null) {
            wrapper.eq(Item::getCategoryId, item.getCategoryId());
        }
        wrapper.orderByDesc(Item::getViews).orderByDesc(Item::getCreatedAt).last("LIMIT " + limit);
        List<Item> items = itemMapper.selectList(wrapper);
        if (items.size() < limit) {
            List<Long> existIds = items.stream().map(Item::getId).collect(Collectors.toList());
            existIds.add(id);
            LambdaQueryWrapper<Item> fallback = new LambdaQueryWrapper<Item>()
                    .eq(Item::getStatus, "在售")
                    .notIn(Item::getId, existIds)
                    .orderByDesc(Item::getViews)
                    .last("LIMIT " + (limit - items.size()));
            items.addAll(itemMapper.selectList(fallback));
        }
        return buildItemList(items);
    }

    @Override
    public Map<String, Object> getPriceRef(Integer categoryId) {
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<Item>()
                .eq(Item::getStatus, "在售");
        if (categoryId != null) wrapper.eq(Item::getCategoryId, categoryId);
        List<Item> items = itemMapper.selectList(wrapper);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categoryId", categoryId);
        if (items.isEmpty()) {
            result.put("count", 0);
            result.put("avg", null);
            result.put("min", null);
            result.put("max", null);
            return result;
        }
        List<BigDecimal> prices = items.stream()
                .map(Item::getPrice)
                .filter(p -> p != null)
                .sorted()
                .collect(Collectors.toList());
        BigDecimal sum = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(prices.size()), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal median;
        int mid = prices.size() / 2;
        if (prices.size() % 2 == 1) {
            median = prices.get(mid);
        } else {
            median = prices.get(mid - 1).add(prices.get(mid))
                    .divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
        }
        result.put("count", prices.size());
        result.put("avg", avg);
        result.put("median", median);
        result.put("min", prices.get(0));
        result.put("max", prices.get(prices.size() - 1));
        return result;
    }

    private List<Map<String, Object>> buildItemList(List<Item> items) {
        // 一次性批量查出所有图片与卖家信息，避免逐商品查询（N+1）
        Map<Long, List<String>> imagesByItem = itemImageService.getImagesByItemIds(
                items.stream().map(Item::getId).collect(Collectors.toList()));
        Set<Long> sellerIds = items.stream().map(Item::getUserId).collect(Collectors.toSet());
        Map<Long, User> sellersById = sellerIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(sellerIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
        return items.stream().map(item -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.getId());
            map.put("userId", item.getUserId());
            map.put("categoryId", item.getCategoryId());
            map.put("title", item.getTitle());
            map.put("description", item.getDescription());
            map.put("price", item.getPrice());
            map.put("condition", item.getCondition());
            map.put("tags", item.getTags());
            map.put("isOriginal", item.getIsOriginal());
            map.put("isFreeShip", item.getIsFreeShip());
            map.put("status", item.getStatus());
            map.put("views", item.getViews());
            map.put("stock", item.getStock() == null ? 1 : item.getStock());
            map.put("createdAt", item.getCreatedAt() != null ? item.getCreatedAt().toString() : null);
            map.put("updatedAt", item.getUpdatedAt() != null ? item.getUpdatedAt().toString() : null);
            List<String> images = imagesByItem.getOrDefault(item.getId(), Collections.emptyList());
            map.put("images", images);
            map.put("mainImage", images.isEmpty() ? null : images.get(0));
            User seller = sellersById.get(item.getUserId());
            Map<String, Object> sellerInfo = new LinkedHashMap<>();
            sellerInfo.put("id", item.getUserId());
            sellerInfo.put("nickname", seller != null ? seller.getNickname() : null);
            sellerInfo.put("avatar", seller != null ? seller.getAvatar() : null);
            map.put("seller", sellerInfo);
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> decoratePublicList(List<Item> items) {
        Map<Long, List<String>> imagesByItem = itemImageService.getImagesByItemIds(
                items.stream().map(Item::getId).collect(Collectors.toList()));
        return items.stream().map(item -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", item.getId());
            m.put("title", item.getTitle());
            m.put("price", item.getPrice());
            m.put("condition", item.getCondition());
            m.put("isFreeShip", item.getIsFreeShip());
            m.put("isOriginal", item.getIsOriginal());
            m.put("views", item.getViews());
            m.put("createdAt", item.getCreatedAt() != null ? item.getCreatedAt().toString() : null);
            List<String> images = imagesByItem.getOrDefault(item.getId(), Collections.emptyList());
            m.put("images", images);
            m.put("mainImage", images.isEmpty() ? null : images.get(0));
            return m;
        }).collect(Collectors.toList());
    }
}