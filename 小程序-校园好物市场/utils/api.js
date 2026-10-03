// 校园好物市场 - API 请求封装（对接港湾跳蚤市场后端）
const BASE = 'https://www.zxczhuanshu.xyz/api';
const HOST = 'https://www.zxczhuanshu.xyz';

function getToken() { return wx.getStorageSync('token') || ''; }
function isLoggedIn() { return !!getToken(); }
function getUser() { return wx.getStorageSync('user') || null; }

function goLogin() {
  wx.removeStorageSync('token');
  wx.removeStorageSync('user');
  wx.reLaunch({ url: '/pages/login/login' });
}

function request(method, path, data, opts) {
  opts = opts || {};
  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE + path,
      method: method,
      data: data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + getToken()
      },
      success(res) {
        if (res.statusCode === 401) {
          if (!opts.silent) wx.showToast({ title: '请先登录', icon: 'none' });
          if (!opts.noRedirect) goLogin();
          reject(new Error('未登录'));
          return;
        }
        const body = res.data || {};
        if (body.code === 200) {
          resolve(body);
        } else {
          if (!opts.silent) wx.showToast({ title: body.msg || '请求失败', icon: 'none' });
          reject(new Error(body.msg || '请求失败'));
        }
      },
      fail(err) {
        if (!opts.silent) wx.showToast({ title: '网络异常，请检查网络', icon: 'none' });
        reject(err);
      }
    });
  });
}

function get(path, data, opts) { return request('GET', path, data, opts); }
function post(path, data, opts) { return request('POST', path, data, opts); }
function put(path, data, opts) { return request('PUT', path, data, opts); }
function del(path, data, opts) { return request('DELETE', path, data, opts); }

// 上传图片，返回 {url}
function uploadImage(filePath) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: BASE + '/upload/image',
      filePath: filePath,
      name: 'file',
      header: { 'Authorization': 'Bearer ' + getToken() },
      success(res) {
        try {
          const body = JSON.parse(res.data);
          if (body.code === 200) resolve(body.data);
          else { wx.showToast({ title: body.msg || '上传失败', icon: 'none' }); reject(new Error(body.msg)); }
        } catch (e) { reject(e); }
      },
      fail: reject
    });
  });
}

// 图片地址拼接：/uploads/xx → 域名开头
function imgUrl(u) {
  if (!u) return '';
  if (u.indexOf('http') === 0) return u;
  return HOST + u;
}

// 价格格式化（WXML 不支持方法调用，列表需在 JS 里先格式化）
function fmtPrice(p) { return parseFloat(p || 0).toFixed(2); }

// 时间格式化：几分钟前/昨天/日期
function fmtTime(t) {
  if (!t) return '';
  const d = new Date(t.replace && t.indexOf('T') > 0 ? t : t);
  if (isNaN(d.getTime())) return t;
  const now = new Date();
  const diff = now - d;
  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';
  if (diff < 172800000) return '昨天';
  const pad = n => (n < 10 ? '0' + n : '' + n);
  if (d.getFullYear() === now.getFullYear()) return pad(d.getMonth() + 1) + '-' + pad(d.getDate());
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
}

module.exports = {
  BASE, HOST,
  getToken, isLoggedIn, getUser, goLogin,
  get, post, put, del,
  uploadImage, imgUrl, fmtPrice, fmtTime
};
