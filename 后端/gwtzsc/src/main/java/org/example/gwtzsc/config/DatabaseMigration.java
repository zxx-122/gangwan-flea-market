package org.example.gwtzsc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 轻量数据库迁移：为已有数据库补充 order 表新增的物流字段，
 * 同时兼容 SQLite 与 MySQL。
 */
@Component
public class DatabaseMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        boolean sqlite = isSqlite();
        migrateOrderColumns(sqlite);
        migrateItemColumns(sqlite);
        migrateFundFlowTable(sqlite);
        migrateUserWxOpenid(sqlite);
        migrateWantTable(sqlite);
        migrateReviewTable(sqlite);
        migrateAnnouncementTable(sqlite);
        log.info("[DatabaseMigration] 数据库结构校验完成（sqlite={}）", sqlite);
    }

    private boolean isSqlite() {
        try {
            return jdbcTemplate.execute((Connection conn) -> {
                DatabaseMetaData meta = conn.getMetaData();
                String url = meta.getURL();
                return url != null && url.contains("sqlite");
            });
        } catch (Exception e) {
            return false;
        }
    }

    private List<String> existingColumns(String table) {
        List<String> cols = new ArrayList<>();
        try {
            cols = jdbcTemplate.execute((Connection conn) -> {
                DatabaseMetaData meta = conn.getMetaData();
                List<String> result = new ArrayList<>();
                try (ResultSet rs = meta.getColumns(null, null, table, "%")) {
                    while (rs.next()) {
                        result.add(rs.getString("COLUMN_NAME").toLowerCase());
                    }
                }
                return result;
            });
        } catch (Exception ignored) {
        }
        return cols;
    }

    private void migrateOrderColumns(boolean sqlite) {
        String table = "`order`";
        List<String> cols = existingColumns("order");
        // SQLite 的 getColumns 对 "order" 可能返回空，用 PRAGMA 兜底
        if (cols.isEmpty() && sqlite) {
            cols = sqliteColumns("order");
        }
        List<String[]> additions = Arrays.asList(
                new String[]{"courier_company", "VARCHAR(50)"},
                new String[]{"tracking_no", "VARCHAR(100)"},
                new String[]{"shipped_at", "DATETIME"},
                new String[]{"completed_at", "DATETIME"}
        );
        for (String[] add : additions) {
            String name = add[0];
            String type = add[1];
            if (cols.contains(name)) continue;
            try {
                jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + name + " " + type);
                log.info("[DatabaseMigration] 已为 order 表新增字段：{}", name);
            } catch (Exception e) {
                log.warn("[DatabaseMigration] 新增字段 {} 失败：{}", name, e.getMessage());
            }
        }
    }

    /** 求购表（幂等创建） */
    private void migrateWantTable(boolean sqlite) {
        String ddl;
        if (sqlite) {
            ddl = "CREATE TABLE IF NOT EXISTS want ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "user_id INTEGER NOT NULL, "
                    + "title TEXT NOT NULL, "
                    + "description TEXT DEFAULT NULL, "
                    + "price_max REAL DEFAULT NULL, "
                    + "category_id INTEGER DEFAULT NULL, "
                    + "status TEXT NOT NULL DEFAULT '求购中', "
                    + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)";
        } else {
            ddl = "CREATE TABLE IF NOT EXISTS want ("
                    + "id BIGINT PRIMARY KEY AUTO_INCREMENT, "
                    + "user_id BIGINT NOT NULL, "
                    + "title VARCHAR(100) NOT NULL, "
                    + "description TEXT, "
                    + "price_max DECIMAL(10,2) DEFAULT NULL, "
                    + "category_id INT DEFAULT NULL, "
                    + "status VARCHAR(10) NOT NULL DEFAULT '求购中', "
                    + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "INDEX idx_status (status), "
                    + "INDEX idx_user (user_id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        }
        try {
            jdbcTemplate.execute(ddl);
            log.info("[DatabaseMigration] want 表结构校验完成");
        } catch (Exception e) {
            log.warn("[DatabaseMigration] 创建 want 表失败：{}", e.getMessage());
        }
    }

    /** user 表补微信 openid 列（幂等） */
    private void migrateUserWxOpenid(boolean sqlite) {
        List<String> cols = existingColumns("user");
        if (cols.isEmpty() && sqlite) {
            cols = sqliteColumns("user");
        }
        if (cols.contains("wx_openid")) return;
        try {
            String type = sqlite ? "TEXT DEFAULT NULL" : "VARCHAR(64) DEFAULT NULL";
            jdbcTemplate.execute("ALTER TABLE `user` ADD COLUMN wx_openid " + type);
            try {
                jdbcTemplate.execute("CREATE UNIQUE INDEX uk_wx_openid ON `user` (wx_openid)");
            } catch (Exception ignored) {}
            log.info("[DatabaseMigration] 已为 user 表新增字段：wx_openid");
        } catch (Exception e) {
            log.warn("[DatabaseMigration] 新增 wx_openid 失败：{}", e.getMessage());
        }
    }

    /** item 表补商品增强字段（幂等，兼容 SQLite 与 MySQL） */
    private void migrateItemColumns(boolean sqlite) {
        List<String> cols = existingColumns("item");
        if (cols.isEmpty() && sqlite) {
            cols = sqliteColumns("item");
        }
        List<String[]> additions = Arrays.asList(
                new String[]{"tags", "TEXT"},
                new String[]{"is_original", "TINYINT DEFAULT 0"},
                new String[]{"is_free_ship", "TINYINT DEFAULT 0"},
                new String[]{"stock", "INTEGER DEFAULT 1"}
        );
        for (String[] add : additions) {
            String name = add[0];
            String type = add[1];
            if (cols.contains(name)) continue;
            try {
                jdbcTemplate.execute("ALTER TABLE item ADD COLUMN " + name + " " + type);
                log.info("[DatabaseMigration] 已为 item 表新增字段：{}", name);
            } catch (Exception e) {
                log.warn("[DatabaseMigration] 新增字段 {} 失败：{}", name, e.getMessage());
            }
        }
    }

    /** 订单评价表（一单可互评：买家评卖家 B2S / 卖家评买家 S2B；幂等创建+升级） */
    private void migrateReviewTable(boolean sqlite) {
        // 旧表（无 direction 列、order_id 单列 UNIQUE）需要重建为支持互评的结构
        List<String> cols = existingColumns("review");
        if (!cols.isEmpty() && !cols.contains("direction")) {
            try {
                jdbcTemplate.execute("ALTER TABLE review RENAME TO review_old");
                String ddlNew = sqlite ? reviewDdlSqlite() : reviewDdlMysql();
                jdbcTemplate.execute(ddlNew);
                jdbcTemplate.execute("INSERT INTO review (id, order_id, item_id, from_user_id, to_user_id, rating, content, created_at, direction) "
                        + "SELECT id, order_id, item_id, from_user_id, to_user_id, rating, content, created_at, 'B2S' FROM review_old");
                jdbcTemplate.execute("DROP TABLE review_old");
                log.info("[DatabaseMigration] review 表已升级为支持互评（direction）");
            } catch (Exception e) {
                log.warn("[DatabaseMigration] review 表升级失败：{}", e.getMessage());
                // 升级失败时确保新表存在（忽略已存在错误）
                try { jdbcTemplate.execute(sqlite ? reviewDdlSqlite() : reviewDdlMysql()); } catch (Exception ignored) {}
            }
            return;
        }
        String ddl = sqlite ? reviewDdlSqlite() : reviewDdlMysql();
        try {
            jdbcTemplate.execute(ddl);
            log.info("[DatabaseMigration] review 表结构校验完成");
        } catch (Exception e) {
            log.warn("[DatabaseMigration] 创建 review 表失败：{}", e.getMessage());
        }
    }

    private String reviewDdlSqlite() {
        return "CREATE TABLE IF NOT EXISTS review ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "order_id INTEGER NOT NULL, "
                + "item_id INTEGER NOT NULL, "
                + "from_user_id INTEGER NOT NULL, "
                + "to_user_id INTEGER NOT NULL, "
                + "rating INTEGER NOT NULL DEFAULT 5, "
                + "content TEXT DEFAULT NULL, "
                + "direction TEXT NOT NULL DEFAULT 'B2S', "
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "UNIQUE(order_id, direction))";
    }

    private String reviewDdlMysql() {
        return "CREATE TABLE IF NOT EXISTS review ("
                + "id BIGINT PRIMARY KEY AUTO_INCREMENT, "
                + "order_id BIGINT NOT NULL, "
                + "item_id BIGINT NOT NULL, "
                + "from_user_id BIGINT NOT NULL, "
                + "to_user_id BIGINT NOT NULL, "
                + "rating INT NOT NULL DEFAULT 5, "
                + "content TEXT, "
                + "direction VARCHAR(4) NOT NULL DEFAULT 'B2S', "
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "UNIQUE KEY uk_order_dir (order_id, direction), "
                + "INDEX idx_item_id (item_id), "
                + "INDEX idx_to_user (to_user_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
    }

    /** 公告表（幂等创建） */
    private void migrateAnnouncementTable(boolean sqlite) {
        String ddl;
        if (sqlite) {
            ddl = "CREATE TABLE IF NOT EXISTS announcement ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "title TEXT NOT NULL, "
                    + "content TEXT DEFAULT NULL, "
                    + "enabled INTEGER NOT NULL DEFAULT 1, "
                    + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)";
        } else {
            ddl = "CREATE TABLE IF NOT EXISTS announcement ("
                    + "id BIGINT PRIMARY KEY AUTO_INCREMENT, "
                    + "title VARCHAR(100) NOT NULL, "
                    + "content TEXT, "
                    + "enabled TINYINT NOT NULL DEFAULT 1, "
                    + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        }
        try {
            jdbcTemplate.execute(ddl);
            log.info("[DatabaseMigration] announcement 表结构校验完成");
        } catch (Exception e) {
            log.warn("[DatabaseMigration] 创建 announcement 表失败：{}", e.getMessage());
        }
    }

    /** 资金流水表（新表，幂等创建，兼容 SQLite 与 MySQL） */
    private void migrateFundFlowTable(boolean sqlite) {        String ddl;
        if (sqlite) {
            ddl = "CREATE TABLE IF NOT EXISTS fund_flow ("
                    + "id INTEGER PRIMARY KEY, "
                    + "user_id INTEGER NOT NULL, "
                    + "type TEXT NOT NULL, "
                    + "amount REAL NOT NULL, "
                    + "balance REAL NOT NULL, "
                    + "remark TEXT DEFAULT NULL, "
                    + "order_id INTEGER DEFAULT NULL, "
                    + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)";
        } else {
            ddl = "CREATE TABLE IF NOT EXISTS fund_flow ("
                    + "id BIGINT PRIMARY KEY, "
                    + "user_id BIGINT NOT NULL, "
                    + "type VARCHAR(20) NOT NULL COMMENT 'INCOME/EXPENSE/WITHDRAW/RECHARGE', "
                    + "amount DECIMAL(10,2) NOT NULL, "
                    + "balance DECIMAL(10,2) NOT NULL, "
                    + "remark VARCHAR(255) DEFAULT NULL, "
                    + "order_id BIGINT DEFAULT NULL, "
                    + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "INDEX idx_user_id (user_id), "
                    + "INDEX idx_created_at (created_at)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        }
        try {
            jdbcTemplate.execute(ddl);
            log.info("[DatabaseMigration] fund_flow 表结构校验完成");
        } catch (Exception e) {
            log.warn("[DatabaseMigration] 创建 fund_flow 表失败：{}", e.getMessage());
        }
    }

    private List<String> sqliteColumns(String table) {
        List<String> cols = new ArrayList<>();
        try {
            jdbcTemplate.query("PRAGMA table_info(" + table + ")", rs -> {
                cols.add(rs.getString("name").toLowerCase());
            });
        } catch (Exception ignored) {
        }
        return cols;
    }
}
