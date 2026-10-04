const api = require('../../utils/api');

Page({
  data: {
    wxing: false,
    mode: 'login',       // login | register
    username: '',
    password: '',
    nickname: '',
    submitting: false
  },
  switchMode(e) {
    this.setData({ mode: e.currentTarget.dataset.mode });
  },
  onInput(e) {
    const f = {};
    f[e.currentTarget.dataset.field] = e.detail.value;
    this.setData(f);
  },
  // 微信一键登录：wx.login 拿 code → 后端换 openid 签发 JWT
  wxLogin() {
    if (this.data.wxing) return;
    this.setData({ wxing: true });
    wx.login({
      success: (r) => {
        if (!r.code) { this.setData({ wxing: false }); return wx.showToast({ title: '微信登录失败', icon: 'none' }); }
        api.post('/auth/wx-login', { code: r.code }).then(res => {
          const data = res.data || {};
          wx.setStorageSync('token', data.token);
          wx.setStorageSync('user', data.user || {});
          wx.showToast({ title: data.isNewUser ? '登录成功，已送1000体验金' : '登录成功', icon: 'none' });
          setTimeout(() => wx.switchTab({ url: '/pages/index/index' }), 800);
        }).catch(() => {}).then(() => this.setData({ wxing: false }));
      },
      fail: () => { this.setData({ wxing: false }); wx.showToast({ title: '微信登录失败', icon: 'none' }); }
    });
  },
  submit() {
    const d = this.data;
    if (this.data.submitting) return;
    if (!d.username.trim()) return wx.showToast({ title: '请输入账号', icon: 'none' });
    if (!d.password) return wx.showToast({ title: '请输入密码', icon: 'none' });
    if (d.mode === 'register' && d.password.length < 6) return wx.showToast({ title: '密码至少6位', icon: 'none' });

    this.setData({ submitting: true });
    const req = d.mode === 'login'
      ? api.post('/auth/login', { username: d.username, password: d.password })
      : api.post('/auth/register', { username: d.username, password: d.password, nickname: d.nickname || d.username })
          .then(() => api.post('/auth/login', { username: d.username, password: d.password }));

    req.then(res => {
      const data = res.data || {};
      if (data.token) {
        wx.setStorageSync('token', data.token);
        wx.setStorageSync('user', data.user || {});
        wx.showToast({ title: d.mode === 'login' ? '登录成功' : '注册成功' });
        setTimeout(() => wx.switchTab({ url: '/pages/index/index' }), 600);
      }
    }).catch(() => {}).then(() => {
      this.setData({ submitting: false });
    });
  }
});
