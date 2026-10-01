-- ================================================
-- SQLite 初始数据
-- ================================================

-- 默认管理员 (账号: admin, 密码: Admin123)
INSERT OR IGNORE INTO user (id, username, password, nickname, role, status, balance) VALUES
(1, 'admin', '$2a$10$6LBDHEb9.N4fHCCdvzsRMeyVgarMJoAKotFI9ErquIKIWkAWTgJfq', '管理员', 'ADMIN', 1, 0.00);

-- 默认商品分类
INSERT OR IGNORE INTO category (id, name, icon, sort) VALUES
(1, '手机数码', '/icons/phone.png', 1),
(2, '家具电器', '/icons/furniture.png', 2),
(3, '图书教材', '/icons/book.png', 3),
(4, '服饰鞋包', '/icons/clothes.png', 4),
(5, '运动户外', '/icons/sports.png', 5),
(6, '游戏娱乐', '/icons/game.png', 6),
(7, '美妆护肤', '/icons/beauty.png', 7),
(8, '其他', '/icons/other.png', 8);