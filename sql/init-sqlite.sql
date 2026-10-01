# 港湾跳蚤市场 — SQLite 初始化脚本
# 用法: sqlite3 gwtzsc.db < init-sqlite.sql

-- ================================================
-- 1. 用户表
-- ================================================
CREATE TABLE IF NOT EXISTS `user` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `username` TEXT NOT NULL UNIQUE,
    `password` TEXT NOT NULL,
    `nickname` TEXT DEFAULT NULL,
    `avatar` TEXT DEFAULT NULL,
    `phone` TEXT DEFAULT NULL,
    `role` TEXT NOT NULL DEFAULT 'USER',
    `balance` REAL NOT NULL DEFAULT 0.00,
    `status` INTEGER NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 默认管理员 (密码: admin / BCrypt)
INSERT OR IGNORE INTO `user` (`id`, `username`, `password`, `nickname`, `role`, `status`) VALUES
(1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '管理员', 'ADMIN', 1);

-- ================================================
-- 2. 分类表
-- ================================================
CREATE TABLE IF NOT EXISTS `category` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` TEXT NOT NULL,
    `icon` TEXT DEFAULT NULL,
    `sort` INTEGER NOT NULL DEFAULT 0
);

INSERT OR IGNORE INTO `category` (`id`, `name`, `icon`, `sort`) VALUES
(1, '手机数码', '/icons/phone.png', 1),
(2, '家具电器', '/icons/furniture.png', 2),
(3, '图书教材', '/icons/book.png', 3),
(4, '服饰鞋包', '/icons/clothes.png', 4),
(5, '运动户外', '/icons/sports.png', 5),
(6, '游戏娱乐', '/icons/game.png', 6),
(7, '美妆护肤', '/icons/beauty.png', 7),
(8, '其他', '/icons/other.png', 8);

-- ================================================
-- 3. 商品表
-- ================================================
CREATE TABLE IF NOT EXISTS `item` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `user_id` INTEGER NOT NULL,
    `category_id` INTEGER DEFAULT NULL,
    `title` TEXT NOT NULL,
    `description` TEXT,
    `price` REAL NOT NULL,
    `condition` TEXT DEFAULT NULL,
    `status` TEXT NOT NULL DEFAULT '在售',
    `views` INTEGER NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_item_user_id ON `item`(`user_id`);
CREATE INDEX IF NOT EXISTS idx_item_category_id ON `item`(`category_id`);
CREATE INDEX IF NOT EXISTS idx_item_status ON `item`(`status`);
CREATE INDEX IF NOT EXISTS idx_item_created_at ON `item`(`created_at`);

-- ================================================
-- 4. 商品图片表
-- ================================================
CREATE TABLE IF NOT EXISTS `item_image` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `item_id` INTEGER NOT NULL,
    `url` TEXT NOT NULL,
    `sort` INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_item_image_item_id ON `item_image`(`item_id`);

-- ================================================
-- 5. 订单表
-- ================================================
CREATE TABLE IF NOT EXISTS `order` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `order_no` TEXT NOT NULL UNIQUE,
    `item_id` INTEGER NOT NULL,
    `seller_id` INTEGER NOT NULL,
    `buyer_id` INTEGER NOT NULL,
    `price` REAL NOT NULL,
    `commission` REAL NOT NULL DEFAULT 0.00,
    `seller_income` REAL NOT NULL DEFAULT 0.00,
    `status` TEXT NOT NULL DEFAULT '待发货',
    `receiver_name` TEXT DEFAULT NULL,
    `receiver_phone` TEXT DEFAULT NULL,
    `receiver_address` TEXT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_order_item_id ON `order`(`item_id`);
CREATE INDEX IF NOT EXISTS idx_order_seller_id ON `order`(`seller_id`);
CREATE INDEX IF NOT EXISTS idx_order_buyer_id ON `order`(`buyer_id`);
CREATE INDEX IF NOT EXISTS idx_order_status ON `order`(`status`);

-- ================================================
-- 6. 消息表
-- ================================================
CREATE TABLE IF NOT EXISTS `message` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `item_id` INTEGER DEFAULT NULL,
    `from_user_id` INTEGER NOT NULL,
    `to_user_id` INTEGER NOT NULL,
    `content` TEXT NOT NULL,
    `is_read` INTEGER NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_msg_from_user ON `message`(`from_user_id`);
CREATE INDEX IF NOT EXISTS idx_msg_to_user ON `message`(`to_user_id`);
CREATE INDEX IF NOT EXISTS idx_msg_item_id ON `message`(`item_id`);

-- ================================================
-- 7. 收藏表
-- ================================================
CREATE TABLE IF NOT EXISTS `favorite` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `user_id` INTEGER NOT NULL,
    `item_id` INTEGER NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fav_user_id ON `favorite`(`user_id`);
CREATE INDEX IF NOT EXISTS idx_fav_item_id ON `favorite`(`item_id`);
CREATE UNIQUE INDEX IF NOT EXISTS uk_fav_user_item ON `favorite`(`user_id`, `item_id`);

-- ================================================
-- 8. 资金流水表
-- ================================================
CREATE TABLE IF NOT EXISTS `fund_flow` (
    `id` INTEGER PRIMARY KEY,
    `user_id` INTEGER NOT NULL,
    `type` TEXT NOT NULL,
    `amount` REAL NOT NULL,
    `balance` REAL NOT NULL,
    `remark` TEXT DEFAULT NULL,
    `order_id` INTEGER DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fund_flow_user_id ON `fund_flow`(`user_id`);
CREATE INDEX IF NOT EXISTS idx_fund_flow_created_at ON `fund_flow`(`created_at`);
