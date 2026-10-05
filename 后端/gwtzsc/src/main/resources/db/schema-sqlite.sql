-- ================================================
-- SQLite 建表脚本 (Spring Boot sql.init 自动执行)
-- ================================================

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    nickname TEXT DEFAULT NULL,
    avatar TEXT DEFAULT NULL,
    phone TEXT DEFAULT NULL,
    wx_openid TEXT DEFAULT NULL UNIQUE,
    role TEXT NOT NULL DEFAULT 'USER',
    balance REAL NOT NULL DEFAULT 0.00,
    status INTEGER NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 分类表
CREATE TABLE IF NOT EXISTS category (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    icon TEXT DEFAULT NULL,
    sort INTEGER NOT NULL DEFAULT 0
);

-- 商品表
CREATE TABLE IF NOT EXISTS item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    category_id INTEGER DEFAULT NULL,
    title TEXT NOT NULL,
    description TEXT,
    price REAL NOT NULL,
    "condition" TEXT DEFAULT NULL,
    status TEXT NOT NULL DEFAULT '在售',
    views INTEGER NOT NULL DEFAULT 0,
    stock INTEGER NOT NULL DEFAULT 1,
    tags TEXT DEFAULT NULL,
    is_original INTEGER NOT NULL DEFAULT 0,
    is_free_ship INTEGER NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_item_user_id ON item(user_id);
CREATE INDEX IF NOT EXISTS idx_item_category_id ON item(category_id);
CREATE INDEX IF NOT EXISTS idx_item_status ON item(status);
CREATE INDEX IF NOT EXISTS idx_item_created_at ON item(created_at);

-- 商品图片表
CREATE TABLE IF NOT EXISTS item_image (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER NOT NULL,
    url TEXT NOT NULL,
    sort INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_item_image_item_id ON item_image(item_id);

-- 订单表
CREATE TABLE IF NOT EXISTS "order" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_no TEXT NOT NULL UNIQUE,
    item_id INTEGER NOT NULL,
    seller_id INTEGER NOT NULL,
    buyer_id INTEGER NOT NULL,
    price REAL NOT NULL,
    commission REAL NOT NULL DEFAULT 0.00,
    seller_income REAL NOT NULL DEFAULT 0.00,
    status TEXT NOT NULL DEFAULT '待发货',
    receiver_name TEXT DEFAULT NULL,
    receiver_phone TEXT DEFAULT NULL,
    receiver_address TEXT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_order_item_id ON "order"(item_id);
CREATE INDEX IF NOT EXISTS idx_order_seller_id ON "order"(seller_id);
CREATE INDEX IF NOT EXISTS idx_order_buyer_id ON "order"(buyer_id);
CREATE INDEX IF NOT EXISTS idx_order_status ON "order"(status);

-- 消息表
CREATE TABLE IF NOT EXISTS message (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER DEFAULT NULL,
    from_user_id INTEGER NOT NULL,
    to_user_id INTEGER NOT NULL,
    content TEXT NOT NULL,
    is_read INTEGER NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_msg_from_user ON message(from_user_id);
CREATE INDEX IF NOT EXISTS idx_msg_to_user ON message(to_user_id);
CREATE INDEX IF NOT EXISTS idx_msg_item_id ON message(item_id);

-- 收藏表
CREATE TABLE IF NOT EXISTS favorite (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    item_id INTEGER NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, item_id)
);

CREATE INDEX IF NOT EXISTS idx_fav_user_id ON favorite(user_id);
CREATE INDEX IF NOT EXISTS idx_fav_item_id ON favorite(item_id);

-- 资金流水表
CREATE TABLE IF NOT EXISTS fund_flow (
    id INTEGER PRIMARY KEY,
    user_id INTEGER NOT NULL,
    type TEXT NOT NULL,
    amount REAL NOT NULL,
    balance REAL NOT NULL,
    remark TEXT DEFAULT NULL,
    order_id INTEGER DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fund_flow_user_id ON fund_flow(user_id);
CREATE INDEX IF NOT EXISTS idx_fund_flow_created_at ON fund_flow(created_at);

-- 订单评价表（一单可互评：B2S 买家评卖家 / S2B 卖家评买家）
CREATE TABLE IF NOT EXISTS review (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    item_id INTEGER NOT NULL,
    from_user_id INTEGER NOT NULL,
    to_user_id INTEGER NOT NULL,
    rating INTEGER NOT NULL DEFAULT 5,
    content TEXT DEFAULT NULL,
    direction TEXT NOT NULL DEFAULT 'B2S',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(order_id, direction)
);

CREATE INDEX IF NOT EXISTS idx_review_item_id ON review(item_id);
CREATE INDEX IF NOT EXISTS idx_review_to_user ON review(to_user_id);

-- 公告表
CREATE TABLE IF NOT EXISTS announcement (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    content TEXT DEFAULT NULL,
    enabled INTEGER NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 举报表
CREATE TABLE IF NOT EXISTS report (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    reporter_id INTEGER NOT NULL,
    target_type TEXT NOT NULL DEFAULT 'ITEM',
    target_id INTEGER NOT NULL,
    reason TEXT DEFAULT NULL,
    status TEXT NOT NULL DEFAULT '待处理',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_report_status ON report(status);


-- 求购表
CREATE TABLE IF NOT EXISTS want (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    description TEXT DEFAULT NULL,
    price_max REAL DEFAULT NULL,
    category_id INTEGER DEFAULT NULL,
    status TEXT NOT NULL DEFAULT '求购中',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_want_status ON want(status);
CREATE INDEX IF NOT EXISTS idx_want_user ON want(user_id);
