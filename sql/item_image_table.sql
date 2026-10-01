-- 商品图片专用表
-- 如果已经执行过 init.sql，则此表已存在，无需重复创建
CREATE TABLE IF NOT EXISTS `item_image` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `item_id` BIGINT NOT NULL COMMENT '所属商品ID',
    `url` VARCHAR(255) NOT NULL COMMENT '图片访问URL',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    INDEX `idx_item_id` (`item_id`),
    INDEX `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
