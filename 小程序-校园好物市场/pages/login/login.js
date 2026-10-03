const api = require('../../utils/api');

Page({
  data: {
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
