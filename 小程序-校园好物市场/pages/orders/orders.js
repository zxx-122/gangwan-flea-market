const api = require('../../utils/api');

Page({
  data: {
    orders: [],
    loading: true
  },
  onShow() { this.load(); },
  load() {
    this.setData({ loading: true });
    api.get('/order/buyer').then(res => {
      const list = (res.data || []).map(o => ({
        id: o.id,
        orderNo: o.orderNo,
        itemTitle: o.itemTitle || '商品已删除',
        priceTxt: api.fmtPrice(o.price),
        status: o.status,
        img: api.imgUrl(o.itemImage),
        courier: o.courierCompany,
        tracking: o.trackingNo,
        shippedTxt: o.shippedAt ? api.fmtTime(o.shippedAt) : ''
      }));
      this.setData({ orders: list, loading: false });
    }).catch(() => this.setData({ loading: false }));
  },
  goDetail(e) { wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id }); },
  cancel(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({ title: '取消订单', content: '确认取消该订单吗？', success: m => {
      if (!m.confirm) return;
      api.put('/order/' + id + '/cancel').then(() => { wx.showToast({ title: '已取消' }); this.load(); }).catch(() => {});
    }});
  },
  confirmReceipt(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({ title: '确认收货', content: '确认收货后将结算给卖家', success: m => {
      if (!m.confirm) return;
      api.put('/order/' + id + '/confirm').then(() => {
        wx.showToast({ title: '确认收货成功，记得评价哦', icon: 'none' });
        this.load();
      }).catch(() => {});
    }});
  },
  review(e) {
    const id = e.currentTarget.dataset.id;
    api.get('/review/order/' + id).then(res => {
      const mine = (res.data || []).find(r => r.direction === 'B2S');
      if (mine) return wx.showToast({ title: '已评价过卖家', icon: 'none' });
      wx.showModal({
        title: '评价这次交易',
        editable: true,
        placeholderText: '说说宝贝的成色、卖家的服务…（默认好评5星）',
        success: m => {
          if (!m.confirm) return;
          api.post('/review', { orderId: id, rating: 5, content: m.content || '好评！' })
            .then(() => { wx.showToast({ title: '评价成功' }); this.load(); }).catch(() => {});
        }
      });
    }).catch(() => {});
  },
  copyTracking(e) {
    wx.setClipboardData({ data: e.currentTarget.dataset.t });
  }
});
