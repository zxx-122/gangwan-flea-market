const api = require('./utils/api');

App({
  onLaunch() {
    // 启动时若有 token，拉取最新用户信息
    const token = wx.getStorageSync('token');
    if (token) {
      api.get('/auth/info').then(res => {
        if (res.data) wx.setStorageSync('user', res.data);
      }).catch(() => {});
    }
  },
  globalData: {}
});
