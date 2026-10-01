-- ============================================================
-- H2 默认数据
-- ============================================================

-- 默认管理员 (密码: Admin123，BCrypt 哈希)
INSERT INTO `user` (`username`, `password`, `nickname`, `role`, `status`) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '管理员', 'ADMIN', 1);

-- 默认分类
INSERT INTO `category` (`name`, `icon`, `sort`) VALUES
('手机数码', '/icons/phone.png', 1),
('家具电器', '/icons/furniture.png', 2),
('图书教材', '/icons/book.png', 3),
('服饰鞋包', '/icons/clothes.png', 4),
('运动户外', '/icons/sports.png', 5),
('游戏娱乐', '/icons/game.png', 6),
('美妆护肤', '/icons/beauty.png', 7),
('其他', '/icons/other.png', 8);
