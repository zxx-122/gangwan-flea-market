-- ============================================================
-- H2 数据库建表脚本 (MySQL 兼容模式)
-- 注意：不要在 H2 中使用 ENGINE=InnoDB、FULLTEXT INDEX 等 MySQL 专有语法
-- ============================================================

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(50) DEFAULT NULL,
    `avatar` VARCHAR(255) DEFAULT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER',
    `balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `status` INT NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_username ON `user`(`username`);
CREATE INDEX IF NOT EXISTS idx_user_role ON `user`(`role`);

-- 商品表
CREATE TABLE IF NOT EXISTS `item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `category_id` INT DEFAULT NULL,
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `price` DECIMAL(10,2) NOT NULL,
    `condition` VARCHAR(20) DEFAULT NULL,
    `status` VARCHAR(10) NOT NULL DEFAULT '在售',
    `views` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_item_user_id ON `item`(`user_id`);
CREATE INDEX IF NOT EXISTS idx_item_category_id ON `item`(`category_id`);
CREATE INDEX IF NOT EXISTS idx_item_status ON `item`(`status`);
CREATE INDEX IF NOT EXISTS idx_item_created_at ON `item`(`created_at`);

-- 商品图片表
CREATE TABLE IF NOT EXISTS `item_image` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `item_id` BIGINT NOT NULL,
    `url` VARCHAR(255) NOT NULL,
    `sort` INT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_item_image_item_id ON `item_image`(`item_id`);

-- 订单表
CREATE TABLE IF NOT EXISTS `order` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `order_no` VARCHAR(50) NOT NULL UNIQUE,
    `item_id` BIGINT NOT NULL,
    `seller_id` BIGINT NOT NULL,
    `buyer_id` BIGINT NOT NULL,
    `price` DECIMAL(10,2) NOT NULL,
    `commission` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `seller_income` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `status` VARCHAR(10) NOT NULL DEFAULT '待发货',
    `receiver_name` VARCHAR(50) DEFAULT NULL,
    `receiver_phone` VARCHAR(20) DEFAULT NULL,
    `receiver_address` VARCHAR(255) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_order_item_id ON `order`(`item_id`);
CREATE INDEX IF NOT EXISTS idx_order_seller_id ON `order`(`seller_id`);
CREATE INDEX IF NOT EXISTS idx_order_buyer_id ON `order`(`buyer_id`);
CREATE INDEX IF NOT EXISTS idx_order_status ON `order`(`status`);

-- 消息表
CREATE TABLE IF NOT EXISTS `message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `item_id` BIGINT DEFAULT NULL,
    `from_user_id` BIGINT NOT NULL,
    `to_user_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `is_read` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_message_from_user_id ON `message`(`from_user_id`);
CREATE INDEX IF NOT EXISTS idx_message_to_user_id ON `message`(`to_user_id`);
CREATE INDEX IF NOT EXISTS idx_message_item_id ON `message`(`item_id`);

-- 收藏表
CREATE TABLE IF NOT EXISTS `favorite` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `item_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_favorite_user_item ON `favorite`(`user_id`, `item_id`);
CREATE INDEX IF NOT EXISTS idx_favorite_user_id ON `favorite`(`user_id`);
CREATE INDEX IF NOT EXISTS idx_favorite_item_id ON `favorite`(`item_id`);

-- 分类表
CREATE TABLE IF NOT EXISTS `category` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL,
    `icon` VARCHAR(255) DEFAULT NULL,
    `sort` INT NOT NULL DEFAULT 0
);
