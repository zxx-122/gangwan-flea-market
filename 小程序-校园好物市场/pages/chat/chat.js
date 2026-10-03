const api = require('../../utils/api');

Page({
  data: {
    otherName: '',
    messages: [],
    myId: null,
    text: '',
    scrollInto: '',
    quick: ['还在吗？', '可以便宜一点吗？', '什么时候能发货？', '我想要，怎么交易？']
  },
  onLoad(options) {
    this.otherUserId = options.userId;
    this.itemId = options.itemId || null;
    this.setData({ myId: api.getUser() ? api.getUser().id : null });
  },
  onShow() {
    this.load();
    this.timer = setInterval(() => this.load(false), 3000);
  },
  onUnload() { if (this.timer) clearInterval(this.timer); },
  onHide() { if (this.timer) clearInterval(this.timer); },
  load(scroll) {
    api.get('/message/chat', { otherUserId: this.otherUserId, itemId: this.itemId || '' }).then(res => {
      const list = (res.data || []).map(m => ({
        id: m.id,
        content: m.content,
        self: m.fromUserId === this.data.myId,
        time: api.fmtTime(m.createdAt)
      }));
      this.setData({ messages: list });
      api.put('/message/read', { otherUserId: this.otherUserId }, { silent: true }).then(() => {}).catch(() => {});
      if (scroll !== false) this.scrollBottom();
      // 对方昵称
      if (!this.data.otherName) {
        api.get('/message/sessions', null, { silent: true }).then(s => {
          const found = (s.data || []).find(x => String(x.userId) === String(this.otherUserId));
          if (found) this.setData({ otherName: found.nickname || found.username });
        }).catch(() => {});
      }
    }).catch(() => {});
  },
  scrollBottom() {
    const last = this.data.messages.length ? 'msg-' + this.data.messages[this.data.messages.length - 1].id : '';
    this.setData({ scrollInto: last });
  },
  onInput(e) { this.setData({ text: e.detail.value }); },
  useQuick(e) { this.setData({ text: e.currentTarget.dataset.q }); },
  send() {
    const t = this.data.text.trim();
    if (!t) return;
    api.post('/message', { toUserId: this.otherUserId, content: t, itemId: this.itemId }).then(() => {
      this.setData({ text: '' });
      this.load();
    }).catch(() => {});
  }
});
