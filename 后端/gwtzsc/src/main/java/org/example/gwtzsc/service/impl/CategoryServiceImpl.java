package org.example.gwtzsc.service.impl;

import org.example.gwtzsc.entity.Category;
import org.example.gwtzsc.mapper.CategoryMapper;
import org.example.gwtzsc.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY = "category:list";

    private List<Category> buildDefaultCategories() {
        List<Category> list = new ArrayList<>();
        String[][] cats = {
            {"1", "手机数码", "/icons/phone.png", "1"},
            {"2", "家具电器", "/icons/furniture.png", "2"},
            {"3", "图书教材", "/icons/book.png", "3"},
            {"4", "服饰鞋包", "/icons/clothes.png", "4"},
            {"5", "运动户外", "/icons/sports.png", "5"},
            {"6", "游戏娱乐", "/icons/game.png", "6"},
            {"7", "美妆护肤", "/icons/beauty.png", "7"},
            {"8", "其他", "/icons/other.png", "8"}
        };
        for (String[] row : cats) {
            Category c = new Category();
            c.setId(Integer.parseInt(row[0]));
            c.setName(row[1]);
            c.setIcon(row[2]);
            c.setSort(Integer.parseInt(row[3]));
            list.add(c);
        }
        return list;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Category> getList() {
        List<Category> cached = (List<Category>) redisTemplate.opsForValue().get(CACHE_KEY);
        if (cached != null) return cached;

        // Try database first, fall back to hardcoded categories on encoding issues
        List<Category> list;
        try {
            list = categoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Category>()
                    .orderByAsc(Category::getSort));
            if (list == null || list.isEmpty()) {
                list = buildDefaultCategories();
            }
        } catch (Exception e) {
            list = buildDefaultCategories();
        }

        redisTemplate.opsForValue().set(CACHE_KEY, list, 30, TimeUnit.MINUTES);
        return list;
    }

    @Override
    public void create(Category category) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new RuntimeException("分类名称不能为空");
        }
        if (category.getSort() == null) category.setSort(0);
        if (category.getIcon() == null) category.setIcon("/icons/default.png");
        categoryMapper.insert(category);
        clearCache();
    }

    @Override
    public void update(Category category) {
        Category existing = categoryMapper.selectById(category.getId());
        if (existing == null) throw new RuntimeException("分类不存在");
        if (category.getName() != null && !category.getName().isBlank()) existing.setName(category.getName());
        if (category.getIcon() != null) existing.setIcon(category.getIcon());
        if (category.getSort() != null) existing.setSort(category.getSort());
        categoryMapper.updateById(existing);
        clearCache();
    }

    @Override
    public void delete(Integer id) {
        Category existing = categoryMapper.selectById(id);
        if (existing == null) throw new RuntimeException("分类不存在");
        categoryMapper.deleteById(id);
        clearCache();
    }

    private void clearCache() {
        redisTemplate.delete(CACHE_KEY);
    }
}