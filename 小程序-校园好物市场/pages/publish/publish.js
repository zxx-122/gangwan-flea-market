const api = require('../../utils/api');

Page({
  data: {
    editId: null,
    title: '',
    price: '',
    stock: '1',
    categoryId: null,
    selCatName: '',
    condition: '',
    description: '',
    categories: [],
    conditions: ['全新', '几乎全新', '轻微使用痕迹', '明显使用痕迹'],
    images: [],
    submitting: false,
    priceRef: null
  },
  onLoad(options) {
    if (!api.isLoggedIn()) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      setTimeout(() => wx.reLaunch({ url: '/pages/login/login' }), 600);
      return;
    }
    api.get('/category/list').then(res => this.setData({ categories: res.data || [] })).catch(() => {});
    if (options.id) {
      this.setData({ editId: options.id });
      wx.setNavigationBarTitle({ title: '编辑商品' });
      api.get('/item/' + options.id).then(res => {
        const d = res.data;
        this.setData({
          title: d.title,
          price: String(d.price),
          stock: String(d.stock == null ? 1 : d.stock),
          categoryId: d.categoryId,
          condition: d.condition || '',
          description: d.description || '',
          images: (d.images || []).map(x => x.url || x)
        });
      }).catch(() => {});
    }
  },
  onField(e) {
    const f = {};
    f[e.currentTarget.dataset.field] = e.detail.value;
    this.setData(f);
  },
  onPickCat(e) {
    const c = this.data.categories[e.detail.value];
    this.setData({ categoryId: c ? c.id : null, selCatName: c ? c.name : '' });
    if (c) this.fetchPriceRef();
  },
  pickCondition(e) { this.setData({ condition: this.data.conditions[e.currentTarget.dataset.i] }); },
  pickImage() {
    const remain = 9 - this.data.images.length;
    if (remain <= 0) return wx.showToast({ title: '最多9张', icon: 'none' });
    wx.chooseMedia({
      count: remain,
      mediaType: ['image'],
      success: (res) => {
        wx.showLoading({ title: '上传中' });
        Promise.all(res.tempFiles.map(f => api.uploadImage(f.tempFilePath)))
          .then(results => {
            wx.hideLoading();
            this.setData({ images: this.data.images.concat(results.map(r => r.url)) });
          })
          .catch(() => wx.hideLoading());
      }
    });
  },
  removeImg(e) {
    const i = e.currentTarget.dataset.i;
    const imgs = this.data.images.slice();
    imgs.splice(i, 1);
    this.setData({ images: imgs });
  },
  fetchPriceRef() {
    api.get('/item/price-ref', { categoryId: this.data.categoryId }, { silent: true }).then(res => {
      this.setData({ priceRef: res.data && res.data.count > 0 ? res.data : null });
    }).catch(() => {});
  },
  submit() {
    const d = this.data;
    if (d.submitting) return;
    if (!d.title.trim()) return wx.showToast({ title: '请填写标题', icon: 'none' });
    if (!d.price || parseFloat(d.price) <= 0) return wx.showToast({ title: '请填写正确价格', icon: 'none' });
    this.setData({ submitting: true });
    const payload = {
      title: d.title,
      price: parseFloat(d.price),
      stock: parseInt(d.stock) || 1,
      categoryId: d.categoryId,
      condition: d.condition,
      tags: '',
      isOriginal: 1,
      isFreeShip: 0,
      description: d.description,
      images: d.images
    };
    const req = d.editId ? api.put('/item/' + d.editId, payload) : api.post('/item', payload);
    req.then(() => {
      wx.showToast({ title: d.editId ? '修改成功' : '发布成功' });
      setTimeout(() => wx.navigateBack({ fail: () => wx.switchTab({ url: '/pages/index/index' }) }), 600);
    }).catch(() => {}).then(() => this.setData({ submitting: false }));
  }
});
