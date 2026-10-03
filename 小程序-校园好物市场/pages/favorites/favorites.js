const api = require('../../utils/api');

Page({
  data: { list: [], loading: true },
  onShow() { this.load(); },
  load() {
    this.setData({ loading: true });
    api.get('/favorite/list').then(res => {
      const list = (res.data || []).map(it => ({
        id: it.id,
        title: it.title,
        priceTxt: api.fmtPrice(it.price),
        status: it.status,
        img: api.imgUrl(it.mainImage || (it.images && it.images[0]))
      }));
      this.setData({ list: list, loading: false });
    }).catch(() => this.setData({ loading: false }));
  },
  goDetail(e) { wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id }); },
  removeFav(e) {
    const id = e.currentTarget.dataset.id;
    api.del('/favorite/' + id).then(() => { wx.showToast({ title: '已取消收藏', icon: 'none' }); this.load(); }).catch(() => {});
  }
});
