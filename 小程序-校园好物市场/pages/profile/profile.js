const api = require('../../utils/api');

Page({
  data: {
    user: null,
    balanceTxt: '0.00',
    stats: { soldCount: 0, onSaleCount: 0 },
    showPwd: false,
    oldPwd: '', newPwd: ''
  },
  onShow() {
    if (!api.isLoggedIn()) {
      wx.reLaunch({ url: '/pages/login/login' });
      return;
    }
    this.setData({ user: api.getUser() });
    api.get('/auth/info').then(res => {
      if (res.data) {
        wx.setStorageSync('user', res.data);
        this.setData({ user: res.data });
      }
    }).catch(() => {});
    api.get('/user/fund-flows', null, { silent: true }).then(res => {
      const flows = res.data || [];
      this.setData({ balanceTxt: api.fmtPrice(flows.length ? flows[0].balance : 0) });
    }).catch(() => {});
    api.get('/auth/info').then(res => {
      const uid = res.data ? res.data.id : null;
      if (uid) api.get('/review/user/' + uid + '/summary', null, { silent: true }).then(() => {}).catch(() => {});
    }).catch(() => {});
    api.get('/user/items', { page: 1, size: 1 }, { silent: true }).then(() => {}).catch(() => {});
    // 卖家统计
    api.get('/auth/info').then(r1 => {
      const uid = r1.data ? r1.data.id : null;
      if (!uid) return;
      // 简化：从订单/商品计数接口不可用时显示 0
    }).catch(() => {});
  },
  go(e) { wx.navigateTo({ url: e.currentTarget.dataset.url }); },
  logout() {
    wx.showModal({ title: '退出登录', content: '确定退出当前账号吗？', success: m => {
      if (!m.confirm) return;
      wx.removeStorageSync('token');
      wx.removeStorageSync('user');
      wx.reLaunch({ url: '/pages/login/login' });
    }});
  },
  togglePwd() { this.setData({ showPwd: !this.data.showPwd }); },
  onPwd(e) {
    const f = {};
    f[e.currentTarget.dataset.field] = e.detail.value;
    this.setData(f);
  },
  savePwd() {
    if (!this.data.oldPwd || !this.data.newPwd) return wx.showToast({ title: '请填写完整', icon: 'none' });
    api.put('/user/password', { oldPassword: this.data.oldPwd, newPassword: this.data.newPwd }).then(() => {
      wx.showToast({ title: '密码已修改，请重新登录' });
      setTimeout(() => {
        wx.removeStorageSync('token');
        wx.removeStorageSync('user');
        wx.reLaunch({ url: '/pages/login/login' });
      }, 1200);
    }).catch(() => {});
  },
  goAdmin() {
    wx.showModal({
      title: '管理后台',
      content: '管理后台请在电脑浏览器打开 admin.html 使用',
      showCancel: false
    });
  }
});
