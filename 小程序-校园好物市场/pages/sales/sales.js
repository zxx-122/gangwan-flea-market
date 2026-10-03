const api = require('../../utils/api');

Page({
  data: { orders: [], loading: true },
  onShow() { this.load(); },
  load() {
    this.setData({ loading: true });
    api.get('/order/seller').then(res => {
      const list = (res.data || []).map(o => ({
        id: o.id,
        orderNo: o.orderNo,
        itemTitle: o.itemTitle || '商品已删除',
        priceTxt: api.fmtPrice(o.price),
        incomeTxt: api.fmtPrice(o.sellerIncome),
        commissionTxt: api.fmtPrice(o.commission),
        status: o.status,
        img: api.imgUrl(o.itemImage),
        buyer: o.receiverName ? o.receiverName + ' · ' + o.receiverPhone : '',
        address: o.receiverAddress || '',
        courier: o.courierCompany,
        tracking: o.trackingNo
      }));
      this.setData({ orders: list, loading: false });
    }).catch(() => this.setData({ loading: false }));
  },
  goDetail(e) { wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id }); },
  ship(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '填写物流信息',
      editable: true,
      placeholderText: '格式：快递公司 运单号（如 顺丰 SF123）',
      success: m => {
        if (!m.confirm || !m.content) return wx.showToast({ title: '已取消发货', icon: 'none' });
        const parts = (m.content || '').trim().split(/\s+/);
        if (parts.length < 2) return wx.showToast({ title: '格式：快递公司 运单号', icon: 'none' });
        api.put('/order/' + id + '/ship', { courierCompany: parts[0], trackingNo: parts.slice(1).join(' ') })
          .then(() => { wx.showToast({ title: '已发货' }); this.load(); }).catch(() => {});
      }
    });
  },
  review(e) {
    const id = e.currentTarget.dataset.id;
    api.get('/review/order/' + id).then(res => {
      const mine = (res.data || []).find(r => r.direction === 'S2B');
      if (mine) return wx.showToast({ title: '已评价过买家', icon: 'none' });
      wx.showModal({
        title: '评价这位买家',
        editable: true,
        placeholderText: '买家的沟通与付款体验…（默认好评5星）',
        success: m => {
          if (!m.confirm) return;
          api.post('/review', { orderId: id, rating: 5, content: m.content || '愉快的交易！' })
            .then(() => { wx.showToast({ title: '评价成功' }); this.load(); }).catch(() => {});
        }
      });
    }).catch(() => {});
  }
});
