const api = require('../../utils/api');

Page({
  data: { balance: '0.00', flows: [], loading: true, showRecharge: false, amount: '', recharging: false },
  onShow() { this.load(); },
  load() {
    this.setData({ loading: true });
    api.get('/user/fund-flows').then(res => {
      const list = (res.data || []).map(f => ({
        id: f.id,
        typeTxt: { INCOME: '收入', EXPENSE: '支出', WITHDRAW: '提现', RECHARGE: '充值' }[f.type] || f.type,
        income: f.type === 'INCOME' || f.type === 'RECHARGE',
        amountTxt: api.fmtPrice(f.amount),
        balanceTxt: api.fmtPrice(f.balance),
        remark: f.remark || '',
        time: api.fmtTime(f.createdAt)
      }));
      const bal = list.length ? list[0].balanceTxt : '0.00';
      this.setData({ flows: list, balance: bal, loading: false });
    }).catch(() => this.setData({ loading: false }));
  },
  openRecharge() {
    if (!api.isLoggedIn()) return wx.navigateTo({ url: '/pages/login/login' });
    this.setData({ showRecharge: true, amount: '' });
  },
  closeRecharge() { this.setData({ showRecharge: false }); },
  onAmount(e) { this.setData({ amount: e.detail.value }); },
  pick(e) { this.setData({ amount: e.currentTarget.dataset.v }); },
  doRecharge() {
    const amt = parseFloat(this.data.amount);
    if (!amt || amt <= 0) return wx.showToast({ title: '请输入正确金额', icon: 'none' });
    this.setData({ recharging: true });
    api.post('/user/recharge', { amount: amt }).then(res => {
      wx.showToast({ title: '充值成功' });
      this.setData({ showRecharge: false });
      this.load();
    }).catch(() => {}).then(() => this.setData({ recharging: false }));
  }
});
