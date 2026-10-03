const api = require('../../utils/api');

Page({
  data: { sessions: [], loading: true, unread: 0 },
  onShow() { this.load(); },
  onUnload() { if (this.timer) clearInterval(this.timer); },
  onHide() { if (this.timer) clearInterval(this.timer); },
  load() {
    if (!api.isLoggedIn()) { this.setData({ sessions: [], loading: false }); return; }
    api.get('/message/sessions').then(res => {
      const list = (res.data || []).map(x => ({
        userId: x.userId,
        name: x.nickname || x.username || '用户',
        avatar: api.imgUrl(x.avatar),
        last: x.lastContent || '',
        time: api.fmtTime(x.lastTime),
        unread: x.unread || 0
      }));
      const unread = list.reduce((s, x) => s + x.unread, 0);
      this.setData({ sessions: list, loading: false, unread: unread });
      if (unread > 0) wx.setTabBarBadge({ index: 2, text: String(unread) }).catch(() => {});
      else wx.removeTabBarBadge({ index: 2 }).catch(() => {});
    }).catch(() => this.setData({ loading: false }));
  },
  goChat(e) {
    wx.navigateTo({ url: '/pages/chat/chat?userId=' + e.currentTarget.dataset.id });
  }
});
