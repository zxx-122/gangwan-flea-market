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
