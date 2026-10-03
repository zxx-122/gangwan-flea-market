const api = require('../../utils/api');

Page({
  data: { list: [], loading: true, searchMode: false, searchResults: [], keyword: '' },
  onLoad(options) {
    if (options && options.search) {
      this.setData({ searchMode: true, keyword: decodeURIComponent(options.search) });
      this.doSearch(this.data.keyword);
    } else {
      this.load();
    }
  },
  goBack() { wx.navigateBack({ fail: () => wx.switchTab({ url: '/pages/index/index' }) }); },
  load() {
    try { this.setData({ list: this.decorate(wx.getStorageSync('browseHistory') || []), loading: false }); }
    catch (e) { this.setData({ list: [], loading: false }); }
  },
  decorate(list) {
    return list.map(it => ({ id: it.id, title: it.title, priceTxt: api.fmtPrice(it.price), img: api.imgUrl(it.img), time: api.fmtTime(it.time) }));
  },
  doSearch(kw) {
    api.get('/item/search', { keyword: kw, page: 1, size: 30 }).then(res => {
      const list = (res.data && res.data.data || []).map(it => ({
        id: it.id, title: it.title, priceTxt: api.fmtPrice(it.price),
        img: api.imgUrl(it.mainImage || (it.images && it.images[0]))
      }));
      this.setData({ searchResults: list, loading: false });
    }).catch(() => this.setData({ loading: false }));
  },
  goDetail(e) { wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id }); },
  clearAll() {
    wx.showModal({ title: '清空浏览历史', content: '确定清空全部浏览记录吗？', success: m => {
      if (!m.confirm) return;
      wx.removeStorageSync('browseHistory');
      this.setData({ list: [] });
    }});
  }
});
