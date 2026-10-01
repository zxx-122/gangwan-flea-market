package org.example.gwtzsc.config;

import org.example.gwtzsc.entity.Category;
import org.example.gwtzsc.entity.User;
import org.example.gwtzsc.mapper.CategoryMapper;
import org.example.gwtzsc.mapper.UserMapper;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 初始化：默认管理员 + 基础分类
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;

    public DataInitializer(UserMapper userMapper, CategoryMapper categoryMapper) {
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public void run(String... args) {
        initAdmin();
        initCategories();
    }

    private void initAdmin() {
        if (userMapper.selectCount(null) > 0) {
            return;
        }
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(BCrypt.hashpw("Admin123", BCrypt.gensalt()));
        admin.setNickname("管理员");
        admin.setRole("ADMIN");
        admin.setStatus(1);
        admin.setBalance(BigDecimal.ZERO);
        userMapper.insert(admin);
        System.out.println("===== 已初始化管理员：admin / Admin123 =====");
    }

    private void initCategories() {
        if (categoryMapper.selectCount(null) > 0) {
            return;
        }
        String[] names = {"手机数码", "家具电器", "图书教材", "服饰鞋包", "运动户外", "游戏娱乐", "美妆护肤", "其他"};
        String[] icons = {"phone", "furniture", "book", "clothes", "sports", "game", "beauty", "other"};
        int sort = 1;
        for (int i = 0; i < names.length; i++) {
            Category c = new Category();
            c.setName(names[i]);
            c.setIcon("/icons/" + icons[i] + ".png");
            c.setSort(sort++);
            categoryMapper.insert(c);
        }
        System.out.println("===== 已初始化商品分类 =====");
    }
}