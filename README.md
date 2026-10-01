# 港湾跳蚤市场

一个仿闲鱼的移动端二手交易平台，采用 Spring Boot 3.x + Vue 3（CDN 单页应用，无需构建）构建。

**线上地址**：https://www.zxczhuanshu.xyz/gwtzsc/

## 项目结构

```
港湾跳蚤市场/
├── 前端/                    # 纯 HTML + Vue3 + CSS，无需构建
│   ├── index.html           # 主 SPA（首页 / 商品详情 / 发布 / 我的 / 订单 / 聊天 / 收藏 / 钱包）
│   ├── login.html           # 登录 / 注册（账号密码 + 短信验证码双方式）
│   ├── admin.html           # 管理后台
│   ├── lib/                 # 自托管前端库（Vue / Vue Router / axios / Font Awesome）
│   ├── css/style.css
│   └── js/
│       ├── config.js        # API 地址配置（本地自动用 localhost，线上走同域反代）
│       ├── api.js           # 接口封装与全局工具
│       └── effects.js       # 视觉特效（撒花、波纹、粒子等）
├── 后端/gwtzsc/             # Spring Boot 后端
└── sql/init.sql             # MySQL 初始化脚本（含默认管理员、分类）
```

## 技术栈

- 后端：Spring Boot 3.2.5、MyBatis-Plus 3.5.7、JWT（含 Redis 黑名单登出）、SQLite / MySQL / H2 三数据库 profile、Redis 缓存
- 前端：Vue 3、Vue Router 4、Axios、FontAwesome 4（全部自托管在 `前端/lib/`，不依赖国外 CDN）
- 安全：BCrypt 密码加密、JWT 鉴权 + 登出黑名单、订单详情仅买卖双方可见、上传图片类型白名单

## 功能清单

- 用户：注册（自动登录）/ 账号密码登录 / 手机号短信验证码登录、改资料、改密码、头像上传
- 商品：发布（图片、卖点标签、实物拍摄、包邮、发布时显示类目成交参考价）、编辑、下架、重新上架、游客浏览、搜索、分类筛选、最新/最热/价格排序
- 交易：下单、卖家发货（快递公司+运单号）、买家确认收货、取消订单；确认收货自动结算（平台抽佣 1.6%）
- 钱包：注册赠送体验金、余额、提现、资金流水（收入/支出/提现/充值）
- 消息：商品私信、会话列表、未读角标
- 收藏：收藏 / 取消收藏
- 管理后台：数据概览、用户管理（启用/禁用/调整余额/删除）、商品管理（下架/重新上架/筛选）、订单监控（状态+关键词筛选）、每日佣金统计、分类管理

## 环境要求

- JDK 21
- Redis 5+
- Maven（已提供 `mvnw` 包装器）
- 数据库三选一：SQLite（默认，零配置）/ MySQL 8 / H2

## 快速启动（本地开发）

### 方式一：一键启动

双击 `start.bat`（可选 MySQL/SQLite，2=SQLite 零配置），脚本会自动起 Redis、后端(8080)、前端(3000) 并打开浏览器。

### 方式二：手动启动

```bash
# 1. 启动 Redis
redis-server

# 2. 启动后端（SQLite profile，零配置）
cd 后端/gwtzsc
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=sqlite

# 3. 前端：直接双击 前端/index.html，或
cd 前端
python -m http.server 3000
```

本地打开时前端自动请求 `http://localhost:8080/api`；部署到服务器后走同域相对路径 `/api`（由 Nginx 反代），无需改配置。

## 默认账号

| 账号 | 密码 | 说明 |
|------|------|------|
| admin | Admin123 | 管理员，可进入 `admin.html` |
| 普通用户 | 自行注册 | 演示环境注册即送 1000 体验金 |

> 提示：演示环境短信验证码会直接弹窗显示（同时打印在后端控制台），无需真实短信通道。

## 服务器部署（Nginx）

- 前端静态目录：`/var/www/gwtzsc/`（Nginx `location /gwtzsc/`）
- 后端：`/opt/gwtzsc/gwtzsc.jar`，systemd 服务 `gwtzsc.service`，SQLite 库 `/opt/gwtzsc/gwtzsc.db`
- Nginx 反代：`/api/`、`/uploads/`、`/icons/` → `127.0.0.1:8080`
- 表结构变更优先扩展现有 `config/DatabaseMigration.java`（幂等，兼容 SQLite/MySQL）

## 注意事项

- **`gwtzsc.db` 与 `uploads/` 含用户数据，已加入 .gitignore，严禁提交到仓库**
- MySQL 请使用 `utf8mb4` 字符集以支持 emoji
- `order` 是 SQL 保留字，SQL 中必须写成 `` `order` ``
