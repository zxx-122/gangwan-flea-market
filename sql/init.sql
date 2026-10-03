-- 创建数据库
CREATE DATABASE IF NOT EXISTS gwtzsc DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE gwtzsc;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(50) DEFAULT NULL,
    `avatar` VARCHAR(255) DEFAULT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT 'USER/ADMIN',
    `balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `status` INT NOT NULL DEFAULT 1 COMMENT '1正常/0禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_username` (`username`),
    INDEX `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 默认管理员 (账号: admin, 密码: Admin123)
INSERT IGNORE INTO `user` (`username`, `password`, `nickname`, `role`, `status`) VALUES
('admin', '$2a$10$6LBDHEb9.N4fHCCdvzsRMeyVgarMJoAKotFI9ErquIKIWkAWTgJfq', '管理员', 'ADMIN', 1);

-- 分类表
CREATE TABLE IF NOT EXISTS `category` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL,
    `icon` VARCHAR(255) DEFAULT NULL,
    `sort` INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

-- 商品表
CREATE TABLE IF NOT EXISTS `item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `category_id` INT DEFAULT NULL,
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `price` DECIMAL(10,2) NOT NULL,
    `condition` VARCHAR(20) DEFAULT NULL COMMENT '全新/几乎全新/轻微使用痕迹/明显使用痕迹',
    `status` VARCHAR(10) NOT NULL DEFAULT '在售' COMMENT '在售/已售/下架',
    `views` INT NOT NULL DEFAULT 0,
    `stock` INT NOT NULL DEFAULT 1 COMMENT '库存数量，售罄自动已售',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '卖点标签，逗号分隔',
    `is_original` TINYINT NOT NULL DEFAULT 0 COMMENT '是否实物拍摄 1是/0否',
    `is_free_ship` TINYINT NOT NULL DEFAULT 0 COMMENT '是否包邮 1是/0否',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_category_id` (`category_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`),
    FULLTEXT INDEX `ft_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商品图片表
CREATE TABLE IF NOT EXISTS `item_image` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `item_id` BIGINT NOT NULL,
    `url` VARCHAR(255) NOT NULL,
    `sort` INT NOT NULL DEFAULT 0,
    INDEX `idx_item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
    `status` VARCHAR(10) NOT NULL DEFAULT '待发货' COMMENT '待发货/待收货/已完成/已取消',
    `receiver_name` VARCHAR(50) DEFAULT NULL,
    `receiver_phone` VARCHAR(20) DEFAULT NULL,
    `receiver_address` VARCHAR(255) DEFAULT NULL,
    `courier_company` VARCHAR(50) DEFAULT NULL,
    `tracking_no` VARCHAR(100) DEFAULT NULL,
    `shipped_at` DATETIME DEFAULT NULL,
    `completed_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_item_id` (`item_id`),
    INDEX `idx_seller_id` (`seller_id`),
    INDEX `idx_buyer_id` (`buyer_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 消息表
CREATE TABLE IF NOT EXISTS `message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `item_id` BIGINT DEFAULT NULL,
    `from_user_id` BIGINT NOT NULL,
    `to_user_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `is_read` INT NOT NULL DEFAULT 0 COMMENT '0未读/1已读',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_from_user_id` (`from_user_id`),
    INDEX `idx_to_user_id` (`to_user_id`),
    INDEX `idx_item_id` (`item_id`),
    INDEX `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 收藏表
CREATE TABLE IF NOT EXISTS `favorite` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `item_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_item` (`user_id`, `item_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 资金流水表
CREATE TABLE IF NOT EXISTS `fund_flow` (
    `id` BIGINT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `type` VARCHAR(20) NOT NULL COMMENT 'INCOME/EXPENSE/WITHDRAW/RECHARGE',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '变动金额',
    `balance` DECIMAL(10,2) NOT NULL COMMENT '变动后余额',
    `remark` VARCHAR(255) DEFAULT NULL,
    `order_id` BIGINT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单评价表（一单可互评：B2S 买家评卖家 / S2B 卖家评买家）
CREATE TABLE IF NOT EXISTS `review` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `order_id` BIGINT NOT NULL,
    `item_id` BIGINT NOT NULL,
    `from_user_id` BIGINT NOT NULL,
    `to_user_id` BIGINT NOT NULL,
    `rating` INT NOT NULL DEFAULT 5 COMMENT '1-5星',
    `content` TEXT DEFAULT NULL,
    `direction` VARCHAR(4) NOT NULL DEFAULT 'B2S',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_order_dir` (`order_id`, `direction`),
    INDEX `idx_item_id` (`item_id`),
    INDEX `idx_to_user` (`to_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 公告表
CREATE TABLE IF NOT EXISTS `announcement` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `title` VARCHAR(100) NOT NULL,
    `content` TEXT DEFAULT NULL,
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用/0停用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 举报表
CREATE TABLE IF NOT EXISTS `report` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `reporter_id` BIGINT NOT NULL,
    `target_type` VARCHAR(10) NOT NULL DEFAULT 'ITEM' COMMENT 'ITEM/USER',
    `target_id` BIGINT NOT NULL,
    `reason` VARCHAR(500) DEFAULT NULL,
    `status` VARCHAR(10) NOT NULL DEFAULT '待处理' COMMENT '待处理/已处理',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

