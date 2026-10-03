const api = require('../../utils/api');

Page({
  data: {
    user: null,
    keyword: '',
    categories: [],
    activeCat: null,
    activeSort: 'new',
    sorts: [
      { key: 'new', label: '最新' },
      { key: 'hot', label: '最热' },
      { key: 'price_asc', label: '价格↑' },
      { key: 'price_desc', label: '价格↓' }
    ],
    items: [],
    page: 1,
    total: 0,
    loading: false,
    noMore: false,
    announcement: null
  },
  onLoad() {
    this.setData({ user: api.getUser() });
    this.loadCategories();
    this.loadAnnouncement();
    this.reload();
  },
  onShow() {
    this.setData({ user: api.getUser() });
  },
  onPullDownRefresh() {
    this.reload().then(() => wx.stopPullDownRefresh());
  },
  onReachBottom() {
    if (this.data.noMore || this.data.loading) return;
    this.setData({ page: this.data.page + 1 });
    this.loadMore();
  },
  loadCategories() {
    api.get('/category/list', null, { silent: true }).then(res => {
      this.setData({ categories: res.data || [] });
    }).catch(() => {});
  },
  loadAnnouncement() {
    api.get('/announcement/list', null, { silent: true }).then(res => {
      const list = res.data || [];
      const ann = list.length ? list[0] : null;
      const readId = wx.getStorageSync('noticeRead');
      this.setData({ announcement: ann && String(ann.id) !== String(readId) ? ann : null });
    }).catch(() => {});
  },
  openAnn() {
    const a = this.data.announcement;
    if (!a) return;
    wx.setStorageSync('noticeRead', String(a.id));
    this.setData({ announcement: null });
    wx.showModal({ title: a.title, content: a.content || '暂无详细内容', showCancel: false, confirmText: '我知道了' });
  },
  reload() {
    this.setData({ page: 1, items: [], noMore: false });
    return this.loadMore();
  },
  loadMore() {
    if (this.data.loading) return Promise.resolve();
    this.setData({ loading: true });
    const d = this.data;
    const req = d.activeCat
      ? api.get('/item/category/' + d.activeCat, { page: d.page, size: 10, sort: d.activeSort })
      : api.get('/item/list', { page: d.page, size: 10, sort: d.activeSort });
    return req.then(res => {
      const pr = res.data || {};
      const list = (pr.data || []).map(it => ({
        id: it.id,
        title: it.title,
        priceTxt: api.fmtPrice(it.price),
        img: api.imgUrl(it.mainImage || (it.images && it.images[0])),
        views: it.views || 0,
        stock: it.stock == null ? 1 : it.stock
      }));
      this.setData({
        items: d.page === 1 ? list : this.data.items.concat(list),
        total: pr.total || 0,
        noMore: this.data.items.length >= (pr.total || 0)
      });
    }).catch(() => {}).then(() => this.setData({ loading: false }));
  },
  pickCat(e) {
    const id = e.currentTarget.dataset.id === '' ? null : Number(e.currentTarget.dataset.id);
    this.setData({ activeCat: id });
    this.reload();
  },
  pickSort(e) {
    const k = e.currentTarget.dataset.key;
    if (k === this.data.activeSort) return;
    this.setData({ activeSort: k });
    this.reload();
  },
  onSearch(e) { this.setData({ keyword: e.detail.value }); },
  doSearch() {
    const kw = this.data.keyword.trim();
    if (!kw) return;
    wx.navigateTo({ url: '/pages/history/history?search=' + encodeURIComponent(kw) });
  },
  goDetail(e) { wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id }); },
  goPublish() { wx.switchTab({ url: '/pages/publish/publish' }); }
});
