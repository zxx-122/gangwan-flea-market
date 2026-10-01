// ===================== API 请求封装 =====================
// API_BASE 在 config.js 中已定义，这里直接使用
if (typeof API_BASE === 'undefined') { var API_BASE = 'http://localhost:8080/api'; }
if (typeof IMG_BASE === 'undefined') { var IMG_BASE = 'http://localhost:8080'; }

const api = axios.create({
    baseURL: API_BASE,
    timeout: 15000,
    headers: { 'Content-Type': 'application/json' }
});

// 请求拦截器 - 自动携带 Token
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = 'Bearer ' + token;
    }
    return config;
});

// 响应拦截器 - 统一错误处理
api.interceptors.response.use(
    res => {
        const data = res.data;
        // 后端业务错误：HTTP 200 但 code !== 200（如登录失败）
        if (data && typeof data.code !== 'undefined' && data.code !== 200) {
            return Promise.reject(new Error(data.msg || '请求失败'));
        }
        return data;
    },
    err => {
        if (err.response) {
            const { code, msg } = err.response.data;
            if (code === 401) {
                localStorage.removeItem('token');
                localStorage.removeItem('user');
                if (!window.location.hash.includes('#/login')) {
                    window.location.hash = '/login';
                }
            }
            return Promise.reject(new Error(msg || '请求失败'));
        }
        return Promise.reject(new Error('网络异常，请检查连接'));
    }
);

// ===================== 认证接口 =====================
const authApi = {
    register(data) { return api.post('/auth/register', data); },
    login(data) { return api.post('/auth/login', data); },
    logout() { return api.post('/auth/logout'); },
    getInfo() { return api.get('/auth/info'); },
    sendSms(data) { return api.post('/auth/send-sms', data); }
};

// ===================== 用户接口 =====================
const userApi = {
    updateProfile(data) { return api.put('/user/profile', data); },
    updatePassword(data) { return api.put('/user/password', data); },
    getMyItems() { return api.get('/user/items'); },
    uploadAvatar(file) {
        const formData = new FormData();
        formData.append('file', file);
        return api.post('/user/avatar', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
    },
    withdraw(data) { return api.post('/user/withdraw', data); },
    getFundFlows() { return api.get('/user/fund-flows'); }
};

// ===================== 商品接口 =====================
const itemApi = {
    list(params) { return api.get('/item/list', { params }); },
    getById(id) { return api.get('/item/' + id); },
    create(data) { return api.post('/item', data); },
    update(id, data) { return api.put('/item/' + id, data); },
    delete(id) { return api.delete('/item/' + id); },
    search(params) { return api.get('/item/search', { params }); },
    byCategory(id, params) { return api.get('/item/category/' + id, { params }); },
    relist(id) { return api.put('/item/' + id + '/relist'); },
    related(id, params) { return api.get('/item/' + id + '/related', { params }); },
    priceRef(params) { return api.get('/item/price-ref', { params }); }
};

// ===================== 订单接口 =====================
const orderApi = {
    create(data) { return api.post('/order', data); },
    getBuyerOrders() { return api.get('/order/buyer'); },
    getSellerOrders() { return api.get('/order/seller'); },
    ship(id, data) { return api.put('/order/' + id + '/ship', data || {}); },
    confirm(id) { return api.put('/order/' + id + '/confirm'); },
    cancel(id) { return api.put('/order/' + id + '/cancel'); },
    getById(id) { return api.get('/order/' + id); }
};

// ===================== 消息接口 =====================
const messageApi = {
    send(data) { return api.post('/message', data); },
    getSessions() { return api.get('/message/sessions'); },
    getChat(params) { return api.get('/message/chat', { params }); },
    markRead(data) { return api.put('/message/read', data); }
};

// ===================== 收藏接口 =====================
const favoriteApi = {
    add(data) { return api.post('/favorite', data); },
    remove(itemId) { return api.delete('/favorite/' + itemId); },
    getList() { return api.get('/favorite/list'); },
    check(itemId) { return api.get('/favorite/check', { params: { itemId } }); }
};

// ===================== 分类接口 =====================
const categoryApi = {
    getList() { return api.get('/category/list'); }
};

// ===================== 上传接口 =====================
const uploadApi = {
    uploadImage(file) {
        const formData = new FormData();
        formData.append('file', file);
        return api.post('/upload/image', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
    }
};

// ===================== 管理员接口 =====================
const adminApi = {
    getStats() { return api.get('/admin/stats'); },
    getUsers(params) { return api.get('/admin/users', { params }); },
    updateUserStatus(id, data) { return api.put('/admin/users/' + id + '/status', data); },
    setUserBalance(id, data) { return api.put('/admin/users/' + id + '/balance', data); },
    getItems(params) { return api.get('/admin/items', { params }); },
    offlineItem(id) { return api.put('/admin/items/' + id + '/offline'); },
    relistItem(id) { return api.put('/admin/items/' + id + '/relist'); },
    getOrders(params) { return api.get('/admin/orders', { params }); },
    getRevenue() { return api.get('/admin/revenue'); },
    deleteUser(id) { return api.delete('/admin/users/' + id); },
    getCategories() { return api.get('/category/list'); },
    createCategory(data) { return api.post('/category', data); },
    updateCategory(id, data) { return api.put('/category/' + id, data); },
    deleteCategory(id) { return api.delete('/category/' + id); }
};

// ===================== 工具函数 =====================
function showToast(msg, duration) {
    duration = duration || 2000;
    const old = document.querySelector('.toast');
    if (old) old.remove();
    const el = document.createElement('div');
    el.className = 'toast';
    el.textContent = msg;
    document.body.appendChild(el);
    setTimeout(() => el.remove(), duration);
}

function showModal(title, msg) {
    return new Promise(resolve => {
        const overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.innerHTML = [
            '<div class="modal-box">',
            '<h3></h3>',
            '<p></p>',
            '<div class="modal-actions">',
            '<button class="btn btn-ghost modal-cancel">取消</button>',
            '<button class="btn btn-primary modal-confirm">确定</button>',
            '</div></div>'
        ].join('');
        overlay.querySelector('h3').textContent = title;
        overlay.querySelector('p').textContent = msg;
        document.body.appendChild(overlay);
        overlay.querySelector('.modal-cancel').onclick = function() { overlay.remove(); resolve(false); };
        overlay.querySelector('.modal-confirm').onclick = function() { overlay.remove(); resolve(true); };
        overlay.onclick = function(e) { if (e.target === overlay) { overlay.remove(); resolve(false); } };
    });
}

function getCurrentUserId() {
    var user = JSON.parse(localStorage.getItem('user') || '{}');
    return user.id || null;
}

function isLoggedIn() {
    return !!localStorage.getItem('token');
}

function formatTime(timeStr) {
    if (!timeStr) return '';
    var d = new Date(timeStr);
    var now = new Date();
    var diff = now - d;
    if (diff < 60000) return '刚刚';
    if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
    if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';
    if (diff < 172800000) return '昨天';
    var month = (d.getMonth() + 1).toString().padStart(2, '0');
    var day = d.getDate().toString().padStart(2, '0');
    if (d.getFullYear() === now.getFullYear()) return month + '-' + day;
    return d.getFullYear() + '-' + month + '-' + day;
}

function fmtDateTime(timeStr) {
    if (!timeStr) return '';
    var d = new Date(timeStr);
    if (isNaN(d.getTime())) return timeStr;
    var pad = n => (n < 10 ? '0' : '') + n;
    return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
        + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
}

function formatPrice(price) {
    return '¥' + parseFloat(price || 0).toFixed(2);
}

function fmtPrice(price) {
    return parseFloat(price || 0).toFixed(2);
}

var IMG_BASE = (typeof window.IMG_BASE !== 'undefined' ? window.IMG_BASE : 'http://localhost:8080');
function getImageUrl(url) {
    if (!url) return '';
    if (url.startsWith('http')) return url;
    return IMG_BASE + url;
}

function isIconImage(icon) {
    return icon && (icon.startsWith('/') || icon.startsWith('http'));
}
