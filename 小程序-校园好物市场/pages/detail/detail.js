const api = require('../../utils/api');

Page({
  data: {
    id: null,
    detail: null,
    imgs: [],
    cur: 0,
    reviews: [],
    sellerRating: null,
    favorited: false,
    isOwner: false,
    showBuy: false,
    myBalance: 0,
    balanceShort: false,
    order: { receiverName: '', receiverPhone: '', receiverAddress: '' },
    submitting: false
  },
  noop() {},
  onLoad(options) {
    this.setData({ id: options.id });
    this.load();
  },
  load() {
    api.get('/item/' + this.data.id).then(res => {
      const d = res.data || {};
      const imgs = ((d.images || []).map(x => api.imgUrl(x.url || x)));
      // 记录浏览历史
      try {
        const key = 'browseHistory';
        const list = wx.getStorageSync(key) || [];
        const filtered = list.filter(x => String(x.id) !== String(d.id));
        filtered.unshift({ id: d.id, title: d.title, price: d.price, img: imgs[0] || '', time: Date.now() });
        wx.setStorageSync(key, filtered.slice(0, 50));
      } catch (e) {}
      this.setData({
        detail: d,
        imgs: imgs,
        isOwner: !!api.getUser() && api.getUser().id === d.userId
      });
      // 评价 + 卖家评分
      api.get('/review/item/' + this.data.id, null, { silent: true }).then(r => {
        const list = (r.data || []).map(x => ({
          rating: x.rating,
          content: x.content,
          time: api.fmtTime(x.createdAt),
          nickname: x.user ? x.user.nickname : '匿名',
          avatar: x.user ? api.imgUrl(x.user.avatar) : ''
        }));
        this.setData({ reviews: list });
      }).catch(() => {});
      api.get('/review/user/' + d.userId + '/summary', null, { silent: true }).then(r => {
        this.setData({ sellerRating: r.data && r.data.avgRating, sellerReviewCount: r.data ? r.data.count : 0 });
      }).catch(() => {});
      // 收藏状态
      if (api.isLoggedIn()) {
        api.get('/favorite/check', { itemId: this.data.id }, { silent: true }).then(r => {
          this.setData({ favorited: !!r.data });
        }).catch(() => {});
      }
    }).catch(() => {});
  },
  prev() { const i = this.data.cur; if (i > 0) this.setData({ cur: i - 1 }); },
  next() { const i = this.data.cur; if (i < this.data.imgs.length - 1) this.setData({ cur: i + 1 }); },
  preview() { wx.previewImage({ urls: this.data.imgs, current: this.data.imgs[this.data.cur] }); },
  toggleFav() {
    if (!api.isLoggedIn()) return wx.navigateTo({ url: '/pages/login/login' });
    const req = this.data.favorited ? api.del('/favorite/' + this.data.id) : api.post('/favorite', { itemId: this.data.id });
    req.then(() => {
      this.setData({ favorited: !this.data.favorited });
      wx.showToast({ title: this.data.favorited ? '已收藏' : '已取消收藏', icon: 'none' });
    }).catch(() => {});
  },
  contact() {
    if (!api.isLoggedIn()) return wx.navigateTo({ url: '/pages/login/login' });
    wx.navigateTo({ url: '/pages/chat/chat?userId=' + this.data.detail.userId + '&itemId=' + this.data.id });
  },
  openBuy() {
    if (!api.isLoggedIn()) return wx.navigateTo({ url: '/pages/login/login' });
    api.get('/auth/info').then(res => {
      const bal = res.data ? res.data.balance : 0;
      this.setData({ showBuy: true, myBalance: bal, balanceShort: bal < parseFloat(this.data.detail.price) });
    }).catch(() => {});
  },
  closeBuy() { this.setData({ showBuy: false }); },
  onOrderInput(e) {
    const f = {};
    f['order.' + e.currentTarget.dataset.field] = e.detail.value;
    this.setData(f);
  },
  submitOrder() {
    if (this.data.balanceShort) return wx.navigateTo({ url: '/pages/wallet/wallet' });
    const o = this.data.order;
    if (!o.receiverName || !o.receiverPhone || !o.receiverAddress) return wx.showToast({ title: '请填写完整收货信息', icon: 'none' });
    this.setData({ submitting: true });
    api.post('/order', { itemId: this.data.id, receiverName: o.receiverName, receiverPhone: o.receiverPhone, receiverAddress: o.receiverAddress })
      .then(() => {
        wx.showToast({ title: '下单成功' });
        this.setData({ showBuy: false });
        setTimeout(() => wx.navigateTo({ url: '/pages/orders/orders' }), 600);
      }).catch(() => {}).then(() => this.setData({ submitting: false }));
  },
  report() {
    if (!api.isLoggedIn()) return wx.navigateTo({ url: '/pages/login/login' });
    wx.showActionSheet({
      itemList: ['虚假商品', '违禁物品', '涉嫌欺诈', '描述严重不符'],
      success: (res) => {
        const reasons = ['虚假商品', '违禁物品', '涉嫌欺诈', '描述严重不符'];
        this.reportReason = reasons[res.tapIndex];
        wx.showModal({
          title: '举报此商品',
          editable: true,
          placeholderText: '补充说明（可选）',
          success: (m) => {
            if (!m.confirm) return;
            const reason = this.reportReason + (m.content ? '：' + m.content : '');
            api.post('/report', { targetType: 'ITEM', targetId: this.data.id, reason: reason })
              .then(() => wx.showToast({ title: '举报已提交' })).catch(() => {});
          }
        });
      }
    });
  }
});
