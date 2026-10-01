// ===================== 环境配置 =====================
// 本地开发（file:// 直开 或 localhost 静态服务器）走 localhost:8080
// 部署到服务器后走同域相对路径（由 Nginx 反代 /api、/uploads、/icons）
var __IS_LOCAL = location.protocol === 'file:' ||
                 location.hostname === 'localhost' ||
                 location.hostname === '127.0.0.1';

var API_BASE = __IS_LOCAL ? 'http://localhost:8080/api' : '/api';
var IMG_BASE = __IS_LOCAL ? 'http://localhost:8080' : '';
