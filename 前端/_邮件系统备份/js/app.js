// ============================================================
// 港湾跳蚤市场 — SPA 主应用
// ============================================================

// ===================== App State =====================
const AppState = {
    user: null,
    categories: [],
    currentPage: '',
    params: {},
    searchKeyword: '',
    adminTab: 'dashboard'
};

function loadUser() {
    try { AppState.user = JSON.parse(localStorage.getItem('user') || 'null'); }
    catch(e) { AppState.user = null; }
}
function saveUser(u) { AppState.user = u; localStorage.setItem('user', JSON.stringify(u)); }
function logout() {
    localStorage.removeItem('token'); localStorage.removeItem('user');
    AppState.user = null; navigate('/login');
}
function requireAuth() {
    if (!isLoggedIn()) { navigate('/login'); return false; }
    return true;
}
function requireAdmin() {
    if (!requireAuth()) return false;
    if (!AppState.user || AppState.user.role !== 'ADMIN') {
        showToast('无管理员权限'); navigate('/home'); return false;
    }
    return true;
}

// ===================== Router =====================
const routes = {};
function route(path, fn) { routes[path] = fn; }
function navigate(path) { window.location.hash = '#' + path; }

function getQuery(path) {
    const qi = path.indexOf('?');
    if (qi < 0) return {};
    const q = {};
    path.substring(qi+1).split('&').forEach(p => {
        const [k,v] = p.split('='); if(k) q[decodeURIComponent(k)] = decodeURIComponent(v||'');
    });
    return q;
}

function router() {
    let hash = (window.location.hash||'').replace('#','') || '/home';
    if (hash === '' || hash === '/') hash = '/home';

    const am = hash.match(/^\/admin(?:\/(\w+))?/);
    if (am) { if(!requireAdmin())return; AppState.adminTab=am[1]||'dashboard'; renderPage('admin',{tab:AppState.adminTab}); return; }

    for (const [p,fn] of Object.entries(routes)) { if(p===hash){fn(getQuery(hash));return;} }

    let m;
    if (m=hash.match(/^\/item\/(\d+)$/)) { renderPage('item',{id:m[1]}); return; }
    if (m=hash.match(/^\/edit\/(\d+)$/)) { if(!requireAuth())return; renderPage('edit',{id:m[1]}); return; }
    if (m=hash.match(/^\/category\/(\d+)$/)) { renderPage('category',{id:m[1]}); return; }
    if (hash.match(/^\/search/)) { renderPage('search',getQuery(hash)); return; }
    if (m=hash.match(/^\/chat\/(\d+)/)) { if(!requireAuth())return; renderPage('chat',{otherUserId:m[1]}); return; }
    renderPage('home',{});
}

// ===================== Render Engine =====================
const appEl = document.getElementById('app');
const pageLifecycle = {};

function renderPage(name, params) {
    const prev = AppState.currentPage;
    if (pageLifecycle[prev] && pageLifecycle[prev].unmount) pageLifecycle[prev].unmount();

    AppState.currentPage = name;
    AppState.params = params || {};

    const R = {
        login: renderLogin, register: renderRegister, home: renderHome,
        category: renderCategory, search: renderSearch, item: renderItemDetail,
        publish: renderPublish, edit: renderEdit, profile: renderProfile,
        'my-items': renderMyItems, 'buyer-orders': renderBuyerOrders,
        'seller-orders': renderSellerOrders, favorites: renderFavorites,
        messages: renderMessages, chat: renderChat, admin: renderAdmin
    };
    const fn = R[name];
    if (!fn) { navigate('/home'); return; }
    appEl.innerHTML = fn(params);
    if (pageLifecycle[name] && pageLifecycle[name].mount) setTimeout(()=>pageLifecycle[name].mount(),50);
}

// ===================== Utility =====================
function $el(id) { return document.getElementById(id); }
function getVal(id) { const e=$el(id); return e?e.value.trim():''; }
function setVal(id,v) { const e=$el(id); if(e)e.value=v; }
// ===================== Pages =====================

// ---------- Login ----------
function renderLogin() {
    if (isLoggedIn()) { navigate('/home'); return ''; }
    return '<div class="login-page"><canvas id="particleCanvas" class="login-canvas"></canvas><div class="login-card">'
    + '<div class="login-header"><div class="login-logo">\u{1F3EA}</div><h1>\u{6E2F}\u{6E7E}\u{8DF3}\u{867E}\u{5E02}\u{573A}</h1>'
    + '<p class="login-sub">\u{4E8C}\u{624B}\u{95F2}\u{7F6E}\u{7269}\u{54C1}\u{4EA4}\u{6613}\u{5E73}\u{53F0}</p></div>'
    + '<div class="login-tabs"><button class="login-tab active" data-tab="password" onclick="switchLoginTab(\'password\')">\u{5BC6}\u{7801}\u{767B}\u{5F55}</button>'
    + '<button class="login-tab" data-tab="sms" onclick="switchLoginTab(\'sms\')">\u{77ED}\u{4FE1}\u{767B}\u{5F55}</button></div>'
    + '<div id="loginForm">'
    + '<div class="form-group"><input type="text" id="loginPhone" placeholder="\u{624B}\u{673A}\u{53F7}" maxlength="11"></div>'
    + '<div class="form-group" id="passwordGroup"><input type="password" id="loginPassword" placeholder="\u{5BC6}\u{7801}"></div>'
    + '<div class="form-group" id="smsGroup" style="display:none"><div style="display:flex;gap:8px">'
    + '<input type="text" id="loginSmsCode" placeholder="\u{77ED}\u{4FE1}\u{9A8C}\u{8BC1}\u{7801}" maxlength="6" style="flex:1">'
    + '<button class="btn btn-outline btn-sm" id="getSmsBtn" onclick="handleGetSms()" style="white-space:nowrap;width:auto">\u{83B7}\u{53D6}\u{9A8C}\u{8BC1}\u{7801}</button></div></div>'
    + '<button class="btn btn-primary btn-block btn-lg" onclick="handleLogin()" style="margin-top:8px">\u{767B} \u{5F55}</button></div>'
    + '<div class="login-footer"><a onclick="navigate(\'/register\')" style="color:var(--primary);cursor:pointer">\u{8FD8}\u{6CA1}\u{6709}\u{8D26}\u{53F7}\u{FF1F}\u{53BB}\u{6CE8}\u{518C} \u{2192}</a></div></div></div>';
}

function switchLoginTab(tab) {
    document.querySelectorAll('.login-tab').forEach(t=>t.classList.remove('active'));
    document.querySelector('.login-tab[data-tab="'+tab+'"]').classList.add('active');
    $el('passwordGroup').style.display=tab==='password'?'':'none';
    $el('smsGroup').style.display=tab==='sms'?'':'none';
}

let smsTimer=null;
function handleGetSms() {
    const p=getVal('loginPhone');
    if(!/^1\d{10}$/.test(p)){showToast('\u{8BF7}\u{8F93}\u{5165}\u{6B63}\u{786E}\u{624B}\u{673A}\u{53F7}');return;}
    authApi.sendSms({phone:p}).then(r=>{
        showToast(r.msg||'\u{9A8C}\u{8BC1}\u{7801}\u{5DF2}\u{53D1}\u{9001}');
        const btn=$el('getSmsBtn'); let sec=60;
        if(smsTimer)clearInterval(smsTimer);
        btn.disabled=true; btn.textContent=sec+'s';
        smsTimer=setInterval(()=>{sec--;if(sec<=0){clearInterval(smsTimer);btn.disabled=false;btn.textContent='\u{83B7}\u{53D6}\u{9A8C}\u{8BC1}\u{7801}';}else btn.textContent=sec+'s';},1000);
    }).catch(e=>showToast(e.message));
}

async function handleLogin() {
    const sms=document.querySelector('.login-tab.active')?.dataset.tab==='sms';
    const p=getVal('loginPhone');
    if(!p){showToast('\u{8BF7}\u{8F93}\u{5165}\u{624B}\u{673A}\u{53F7}');return;}
    try{
        let r;
        if(sms){const c=getVal('loginSmsCode');if(!c){showToast('\u{8BF7}\u{8F93}\u{5165}\u{9A8C}\u{8BC1}\u{7801}');return;}r=await authApi.login({phone:p,smsCode:c});}
        else{const pw=getVal('loginPassword');if(!pw){showToast('\u{8BF7}\u{8F93}\u{5165}\u{5BC6}\u{7801}');return;}r=await authApi.login({phone:p,password:pw});}
        if(r.code===200&&r.data){localStorage.setItem('token',r.data.token);saveUser(r.data.user);showToast('\u{767B}\u{5F55}\u{6210}\u{529F}');navigate('/home');}
        else showToast(r.msg||'\u{767B}\u{5F55}\u{5931}\u{8D25}');
    }catch(e){showToast(e.message);}
}

// ---------- Register ----------
function renderRegister() {
    if(isLoggedIn()){navigate('/home');return'';}
    return '<div class="login-page"><canvas id="particleCanvas" class="login-canvas"></canvas><div class="login-card" style="max-width:400px">'
    + '<div class="login-header"><div class="login-logo">\u{1F3EA}</div><h1>\u{6CE8}\u{518C}\u{8D26}\u{53F7}</h1><p class="login-sub">\u{52A0}\u{5165}\u{6E2F}\u{6E7E}\u{8DF3}\u{867E}\u{5E02}\u{573A}</p></div>'
    + '<div id="registerForm">'
    + '<div class="form-group"><input type="text" id="regPhone" placeholder="\u{624B}\u{673A}\u{53F7}" maxlength="11"></div>'
    + '<div class="form-group"><input type="text" id="regNickname" placeholder="\u{6635}\u{79F0}\u{FF08}\u{9009}\u{586B}\u{FF09}"></div>'
    + '<div class="form-group"><input type="password" id="regPassword" placeholder="\u{5BC6}\u{7801}\u{FF08}\u{81F3}\u{5C11}8\u{4F4D}\u{FF0C}\u{542B}\u{5B57}\u{6BCD}\u{548C}\u{6570}\u{5B57}\u{FF09}"></div>'
    + '<div class="form-group"><input type="password" id="regConfirm" placeholder="\u{786E}\u{8BA4}\u{5BC6}\u{7801}"></div>'
    + '<button class="btn btn-primary btn-block btn-lg" onclick="handleRegister()">\u{6CE8} \u{518C}</button></div>'
    + '<div class="login-footer"><a onclick="navigate(\'/login\')" style="color:var(--primary);cursor:pointer">\u{5DF2}\u{6709}\u{8D26}\u{53F7}\u{FF1F}\u{53BB}\u{767B}\u{5F55} \u{2192}</a></div></div></div>';
}

async function handleRegister() {
    const p=getVal('regPhone'),nick=getVal('regNickname'),pw=getVal('regPassword'),cf=getVal('regConfirm');
    if(!/^1\d{10}$/.test(p)){showToast('\u{8BF7}\u{8F93}\u{5165}\u{6B63}\u{786E}\u{624B}\u{673A}\u{53F7}');return;}
    if(pw.length<8){showToast('\u{5BC6}\u{7801}\u{81F3}\u{5C11}8\u{4F4D}');return;}
    if(pw!==cf){showToast('\u{4E24}\u{6B21}\u{5BC6}\u{7801}\u{4E0D}\u{4E00}\u{81F4}');return;}
    try{
        const r=await authApi.register({phone:p,nickname:nick||p,password:pw,username:p});
        if(r.code===200){showToast('\u{6CE8}\u{518C}\u{6210}\u{529F}');navigate('/login');}
        else showToast(r.msg||'\u{6CE8}\u{518C}\u{5931}\u{8D25}');
    }catch(e){showToast(e.message);}
}
// ---------- Home ----------
function renderHome() {
    loadUser(); const u=AppState.user;
    return '<div class="page"><div class="home-header"><div class="home-header-top"><div>'
    + '<div class="home-greeting">'+getGreeting()+(u?', '+(u.nickname||u.username):'')+'</div>'
    + '<div class="home-slogan">\u{53D1}\u{73B0}\u{597D}\u{7269}\u{FF0C}\u{6DD8}\u{4F60}\u{6240}\u{7231}</div></div>'
    + '<div class="home-actions">'+(u?'<span class="home-msg-bell" onclick="navigate(\'/messages\')">\u{1F514}</span>':'')+'</div></div>'
    + '<div class="home-search" onclick="navigate(\'/search\')"><span class="home-search-icon">\u{1F50D}</span>'
    + '<span class="home-search-text">\u{641C}\u{7D22}\u{5546}\u{54C1}\u{540D}\u{79F0}...</span></div></div>'
    + '<div class="home-categories" id="categoryChips"><div class="home-cat-chip active" data-cat="" onclick="filterCategory(\'\')">\u{5168}\u{90E8}</div></div>'
    + '<div class="home-items"><div class="waterfall" id="waterfall"></div>'
    + '<div class="home-loading" id="homeLoading" style="display:flex"><div class="loading-spinner"></div></div></div></div>'
    + '<div class="tabbar"><div class="tabbar-item active" onclick="navigate(\'/home\')"><span class="tab-icon">\u{1F3E0}</span><span>\u{9996}\u{9875}</span></div>'
    + '<div class="tabbar-item" onclick="checkAuthThen(\'/publish\')"><span class="tab-icon">\u{2795}</span><span>\u{53D1}\u{5E03}</span></div>'
    + '<div class="tabbar-item" onclick="checkAuthThen(\'/messages\')"><span class="tab-icon">\u{1F4AC}</span><span>\u{6D88}\u{606F}</span></div>'
    + '<div class="tabbar-item" onclick="checkAuthThen(\'/profile\')"><span class="tab-icon">\u{1F464}</span><span>\u{6211}\u{7684}</span></div></div>';
}
function checkAuthThen(p){if(requireAuth())navigate(p);}
function getGreeting(){const h=new Date().getHours();if(h<6)return'\u{591C}\u{6DF1}\u{4E86}';if(h<9)return'\u{65E9}\u{4E0A}\u{597D}';if(h<12)return'\u{4E0A}\u{5348}\u{597D}';if(h<14)return'\u{4E2D}\u{5348}\u{597D}';if(h<18)return'\u{4E0B}\u{5348}\u{597D}';return'\u{665A}\u{4E0A}\u{597D}';}

let homePage=1,homeLoading=false,homeHasMore=true,selectedCategory='';
async function loadHomeItems(reset){
    if(reset){homePage=1;homeHasMore=true;}
    if(homeLoading||!homeHasMore)return;
    homeLoading=true;
    const ld=$el('homeLoading');if(ld)ld.style.display='flex';
    try{
        const p={page:homePage,size:20};if(selectedCategory)p.categoryId=selectedCategory;
        const r=await itemApi.list(p);
        const items=r.data?.data||[];
        const ct=$el('waterfall');if(!ct)return;
        if(reset)ct.innerHTML='';
        items.forEach(i=>ct.appendChild(createItemCard(i)));
        if(items.length<20)homeHasMore=false;
        homePage++;
    }catch(e){showToast(e.message);}
    finally{homeLoading=false;if(ld)ld.style.display='none';}
}
function filterCategory(id){selectedCategory=id;document.querySelectorAll('.home-cat-chip').forEach(c=>c.classList.toggle('active',c.dataset.cat==id));loadHomeItems(true);}

function createItemCard(item){
    const imgs=item.images||[],imgUrl=imgs.length?getImageUrl(imgs[0]):'';
    const d=document.createElement('div');d.className='item-card';
    d.innerHTML='<div class="item-img" style="background-image:url(\''+imgUrl+'\')">'
    +(!imgUrl?'<span style="font-size:40px;opacity:0.3;display:flex;align-items:center;justify-content:center;height:100%">\u{1F4E6}</span>':'')
    +'</div><div class="item-info"><div class="item-title">'+esc(item.title||'')+'</div>'
    +'<div class="item-price">'+formatPrice(item.price)+'</div>'
    +'<div class="item-meta"><span>'+(item.condition||'')+'</span><span>'+formatTime(item.createdAt)+'</span></div></div>';
    d.onclick=()=>navigate('/item/'+item.id);
    return d;
}
function esc(t){const e=document.createElement('div');e.textContent=t;return e.innerHTML;}

// ---------- Category ----------
function renderCategory(){
    return '<div class="page-no-tabbar"><div class="navbar"><span class="navbar-back" onclick="history.back()">\u{2190}</span>'
    +'<span class="navbar-title">\u{5206}\u{7C7B}</span><span class="navbar-right"></span></div>'
    +'<div class="home-items" style="padding:12px"><div class="waterfall" id="catWaterfall"></div>'
    +'<div class="home-loading" id="catLoading" style="display:flex"><div class="loading-spinner"></div></div></div></div>';
}

// ---------- Search ----------
function renderSearch(){
    return '<div class="page-no-tabbar"><div class="navbar"><span class="navbar-back" onclick="history.back()">\u{2190}</span>'
    +'<div style="flex:1;display:flex"><input type="text" id="searchInput" placeholder="\u{641C}\u{7D22}\u{5546}\u{54C1}\u{540D}\u{79F0}..." style="border-radius:20px;padding:8px 14px;font-size:14px" onkeydown="if(event.key===\'Enter\')doSearch()"></div>'
    +'<span class="navbar-right"><button onclick="doSearch()" style="color:var(--primary);font-weight:600;background:none;padding:4px 8px">\u{641C}\u{7D22}</button></span></div>'
    +'<div class="home-items" style="padding:12px"><div class="waterfall" id="searchWaterfall"></div>'
    +'<div class="home-loading" id="searchLoading" style="display:none"><div class="loading-spinner"></div></div>'
    +'<div id="searchEmpty" class="empty-state" style="display:none"><div class="icon">\u{1F50D}</div><p>\u{672A}\u{627E}\u{5230}\u{76F8}\u{5173}\u{5546}\u{54C1}</p></div></div></div>';
}

let searchPage=1,searchLoading=false,searchHasMore=true;
async function doSearch(){
    const k=getVal('searchInput');if(!k){showToast('\u{8BF7}\u{8F93}\u{5165}\u{641C}\u{7D22}\u{5173}\u{952E}\u{8BCD}');return;}
    searchPage=1;searchHasMore=true;await loadSearchResults(k);
}
async function loadSearchResults(keyword,reset){
    if(reset){searchPage=1;searchHasMore=true;}
    if(searchLoading||!searchHasMore)return;
    searchLoading=true;
    const ld=$el('searchLoading');if(ld)ld.style.display='flex';
    try{
        const r=await itemApi.search({keyword,page:searchPage,size:20});
        const items=r.data?.data||[],ct=$el('searchWaterfall'),em=$el('searchEmpty');
        if(!ct)return;
        if(reset||searchPage===1)ct.innerHTML='';
        if(items.length===0&&searchPage===1){if(em)em.style.display='flex';}else{if(em)em.style.display='none';items.forEach(i=>ct.appendChild(createItemCard(i)));if(items.length<20)searchHasMore=false;searchPage++;}
    }catch(e){showToast(e.message);}
    finally{searchLoading=false;if(ld)ld.style.display='none';}
}
// ---------- Item Detail ----------
function renderItemDetail(){
    return '<div class="page-no-tabbar"><div class="navbar"><span class="navbar-back" onclick="history.back()">\u{2190}</span>'
    +'<span class="navbar-title">\u{5546}\u{54C1}\u{8BE6}\u{60C5}</span><span class="navbar-right"></span></div>'
    +'<div class="detail-loading" id="detailLoading"><div class="loading-spinner"></div></div>'
    +'<div id="detailContent" style="display:none"></div></div>';
}

async function loadItemDetail(id){
    try{
        const r=await itemApi.getById(id),item=r.data||{},imgs=item.images||[],seller=item.seller||{};
        let isFav=false;
        if(isLoggedIn()){try{const f=await favoriteApi.check(id);isFav=f.data===true;}catch(e){}}
        const h='<div class="detail-gallery">'
        +(imgs.length?imgs.map(u=>'<div class="detail-img" style="background-image:url(\''+getImageUrl(u)+'\')"></div>').join(''):'<div class="detail-img" style="background:#f0f0f0;display:flex;align-items:center;justify-content:center;font-size:60px;opacity:0.3">\u{1F4E6}</div>')
        +'</div><div class="detail-section"><div class="detail-price">'+formatPrice(item.price)+'</div>'
        +'<h1 class="detail-title">'+esc(item.title||'')+'</h1>'
        +'<div class="detail-tags"><span class="detail-tag">'+(item.condition||'\u{6210}\u{8272}\u{672A}\u{77E5}')+'</span>'
        +'<span class="detail-tag">'+(item.categoryName||'')+'</span>'
        +'<span class="detail-tag">\u{1F441} '+(item.views||0)+'\u{6B21}\u{6D4F}\u{89C8}</span></div></div>'
        +'<div class="detail-section"><h3 class="detail-section-title">\u{5546}\u{54C1}\u{63CF}\u{8FF0}</h3>'
        +'<p class="detail-desc">'+esc(item.description||'\u{6682}\u{65E0}\u{63CF}\u{8FF0}')+'</p></div>'
        +'<div class="detail-section"><h3 class="detail-section-title">\u{5356}\u{5BB6}\u{4FE1}\u{606F}</h3>'
        +'<div class="detail-seller"'+(seller.id?' onclick="navigate(\'/chat/'+seller.id+'?itemId='+id+'\')"':'')+'>'
        +'<div class="detail-seller-avatar">'+(seller.avatar?'<img src="'+getImageUrl(seller.avatar)+'">':'<span>'+(seller.nickname||'?')[0]+'</span>')+'</div>'
        +'<div class="detail-seller-info"><div class="detail-seller-name">'+esc(seller.nickname||'\u{672A}\u{77E5}\u{7528}\u{6237}')+'</div>'
        +'<div class="detail-seller-time">'+formatTime(item.createdAt)+'\u{53D1}\u{5E03}</div></div>'
        +'<span style="color:var(--primary);font-size:13px">\u{8054}\u{7CFB}ta \u{2192}</span></div></div>'
        +'<div style="padding:16px;display:flex;gap:10px">'
        +(isLoggedIn()?'<button class="btn btn-outline btn-lg" style="flex:1" onclick="toggleFavorite('+id+','+isFav+')">'+(isFav?'\u{2764}\u{FE0F} \u{5DF2}\u{6536}\u{85CF}':'\u{1F90D} \u{6536}\u{85CF}')+'</button>'
        +'<button class="btn btn-primary btn-lg" style="flex:2" onclick="orderNow('+id+')">\u{7ACB}\u{5373}\u{8D2D}\u{4E70}</button>'
        :'<button class="btn btn-primary btn-block btn-lg" onclick="navigate(\'/login\')">\u{767B}\u{5F55}\u{540E}\u{8D2D}\u{4E70}</button>')+'</div>';
        const ld=$el('detailLoading'),ct=$el('detailContent');
        if(ld)ld.style.display='none';
        if(ct){ct.style.display='block';ct.innerHTML=h;}
    }catch(e){showToast(e.message);const ld=$el('detailLoading');if(ld)ld.innerHTML='<div class="empty-state"><div class="icon">\u{1F635}</div><p>\u{52A0}\u{8F7D}\u{5931}\u{8D25}</p></div>';}
}
async function toggleFavorite(id,cur){
    try{if(cur){await favoriteApi.remove(id);showToast('\u{5DF2}\u{53D6}\u{6D88}\u{6536}\u{85CF}');}else{await favoriteApi.add({itemId:id});showToast('\u{5DF2}\u{6536}\u{85CF}');}loadItemDetail(id);}
    catch(e){showToast(e.message);}
}
async function orderNow(id){
    if(!requireAuth())return;
    const ok=await showModal('\u{786E}\u{8BA4}\u{8D2D}\u{4E70}','\u{786E}\u{5B9A}\u{8981}\u{8D2D}\u{4E70}\u{6B64}\u{5546}\u{54C1}\u{5417}\u{FF1F}');
    if(!ok)return;
    appEl.innerHTML='<div class="page-no-tabbar"><div class="navbar"><span class="navbar-back" onclick="history.back()">\u{2190}</span><span class="navbar-title">\u{786E}\u{8BA4}\u{8BA2}\u{5355}</span><span class="navbar-right"></span></div><div style="padding:16px">'
    +'<div class="form-group"><input type="text" id="orderName" placeholder="\u{6536}\u{8D27}\u{4EBA}\u{59D3}\u{540D}"></div>'
    +'<div class="form-group"><input type="text" id="orderPhone" placeholder="\u{6536}\u{8D27}\u{4EBA}\u{624B}\u{673A}\u{53F7}" maxlength="11"></div>'
    +'<div class="form-group"><input type="text" id="orderAddress" placeholder="\u{6536}\u{8D27}\u{5730}\u{5740}"></div>'
    +'<button class="btn btn-primary btn-block btn-lg" onclick="submitOrder('+id+')">\u{63D0}\u{4EA4}\u{8BA2}\u{5355}</button></div></div>';
}
async function submitOrder(id){
    const n=getVal('orderName'),p=getVal('orderPhone'),a=getVal('orderAddress');
    if(!n||!p||!a){showToast('\u{8BF7}\u{586B}\u{5199}\u{5B8C}\u{6574}\u{4FE1}\u{606F}');return;}
    try{
        const r=await orderApi.create({itemId:id,receiverName:n,receiverPhone:p,receiverAddress:a});
        if(r.code===200){showToast('\u{4E0B}\u{5355}\u{6210}\u{529F}');navigate('/buyer-orders');}
        else showToast(r.msg||'\u{4E0B}\u{5355}\u{5931}\u{8D25}');
    }catch(e){showToast(e.message);}
}

// ---------- Publish ----------
function renderPublish(){
    if(!requireAuth())return'';
    return '<div class="page-no-tabbar"><div class="navbar"><span class="navbar-back" onclick="history.back()">\u{2190}</span>'
    +'<span class="navbar-title">\u{53D1}\u{5E03}\u{5546}\u{54C1}</span><span class="navbar-right"></span></div><div style="padding:16px">'
    +'<div class="form-group"><input type="text" id="pubTitle" placeholder="\u{5546}\u{54C1}\u{6807}\u{9898}" maxlength="100"></div>'
    +'<div class="form-group"><select id="pubCategory"><option value="">\u{9009}\u{62E9}\u{5206}\u{7C7B}</option></select></div>'
    +'<div class="form-group"><select id="pubCondition"><option value="\u{5168}\u{65B0}">\u{5168}\u{65B0}</option><option value="\u{51E0}\u{4E4E}\u{5168}\u{65B0}">\u{51E0}\u{4E4E}\u{5168}\u{65B0}</option><option value="\u{8F7B}\u{5FAE}\u{4F7F}\u{7528}\u{75D5}\u{8FF9}">\u{8F7B}\u{5FAE}\u{4F7F}\u{7528}\u{75D5}\u{8FF9}</option><option value="\u{660E}\u{663E}\u{4F7F}\u{7528}\u{75D5}\u{8FF9}">\u{660E}\u{663E}\u{4F7F}\u{7528}\u{75D5}\u{8FF9}</option></select></div>'
    +'<div class="form-group"><div style="position:relative"><span style="position:absolute;left:14px;top:50%;transform:translateY(-50%);color:var(--text-light);font-weight:600">\u{FFE5}</span>'
    +'<input type="number" id="pubPrice" placeholder="\u{4EF7}\u{683C}" min="0" step="0.01" style="padding-left:32px"></div></div>'
    +'<div class="form-group"><textarea id="pubDesc" placeholder="\u{63CF}\u{8FF0}\u{5546}\u{54C1}\u{8BE6}\u{60C5}\u{FF08}\u{6210}\u{8272}\u{3001}\u{89C4}\u{683C}\u{3001}\u{4EA4}\u{6613}\u{65B9}\u{5F0F}\u{7B49}\u{FF09}" rows="5"></textarea></div>'
    +'<div class="form-group"><label style="display:block;margin-bottom:8px;font-weight:500;color:var(--text-secondary)">\u{5546}\u{54C1}\u{56FE}\u{7247}</label>'
    +'<div class="upload-area" onclick="$el(\'fileInput\').click()"><div class="upload-hint">+ \u{70B9}\u{51FB}\u{4E0A}\u{4F20}\u{56FE}\u{7247}</div>'
    +'<input type="file" id="fileInput" accept="image/*" multiple style="display:none" onchange="handleUpload()"></div>'
    +'<div class="upload-previews" id="uploadPreviews"></div></div>'
    +'<button class="btn btn-primary btn-block btn-lg" onclick="handlePublish()">\u{53D1}\u{5E03}\u{5546}\u{54C1}</button></div></div>';
}

let uploadedImages=[];
async function handleUpload(){
    const inp=$el('fileInput'),files=inp.files;if(!files||!files.length)return;
    const pv=$el('uploadPreviews');
    for(const f of files){
        if(uploadedImages.length>=9){showToast('\u{6700}\u{591A}\u{4E0A}\u{4F20}9\u{5F20}');break;}
        const reader=new FileReader();
        reader.onload=function(e){const d=document.createElement('div');d.className='upload-preview';d.innerHTML='<img src="'+e.target.result+'"><span class="upload-remove" onclick="this.parentElement.remove()">\u{D7}</span>';pv.appendChild(d);};
        reader.readAsDataURL(f);
        try{const r=await uploadApi.uploadImage(f);if(r.code===200&&r.data)uploadedImages.push(r.data.url);}catch(e){showToast('\u{4E0A}\u{4F20}\u{5931}\u{8D25}: '+e.message);}
    }
    inp.value='';
}
async function handlePublish(){
    const t=getVal('pubTitle'),c=getVal('pubCategory'),cd=getVal('pubCondition'),p=getVal('pubPrice'),d=getVal('pubDesc');
    if(!t){showToast('\u{8BF7}\u{8F93}\u{5165}\u{5546}\u{54C1}\u{6807}\u{9898}');return;}
    if(!c){showToast('\u{8BF7}\u{9009}\u{62E9}\u{5206}\u{7C7B}');return;}
    if(!p||parseFloat(p)<=0){showToast('\u{8BF7}\u{8F93}\u{5165}\u{6709}\u{6548}\u{4EF7}\u{683C}');return;}
    try{
        const r=await itemApi.create({title:t,categoryId:parseInt(c),condition:cd,price:parseFloat(p),description:d,images:uploadedImages});
        if(r.code===200){showToast('\u{53D1}\u{5E03}\u{6210}\u{529F}');uploadedImages=[];navigate('/my-items');}
        else showToast(r.msg||'\u{53D1}\u{5E03}\u{5931}\u{8D25}');
    }catch(e){showToast(e.message);}
}
// ---------- Profile ----------
function renderProfile(){
    if(!requireAuth())return'';const u=AppState.user||{};
    return '<div class="page"><div class="profile-header"><div class="profile-avatar">'
    +(u.avatar?'<img src="'+getImageUrl(u.avatar)+'">':'<span>'+(u.nickname||u.username||'?')[0]+'</span>')+'</div>'
    +'<div class="profile-name">'+esc(u.nickname||u.username||'')+'</div>'
    +'<div class="profile-phone">'+(u.phone||'')+'</div>'
    +'<div class="profile-balance">\u{4F59}\u{989D}\u{FF1A}'+formatPrice(u.balance||0)+'</div></div>'
    +'<div class="profile-stats"><div class="profile-stat" onclick="navigate(\'/my-items\')"><div class="profile-stat-num" id="statItems">-</div><div class="profile-stat-label">\u{53D1}\u{5E03}</div></div>'
    +'<div class="profile-stat" onclick="navigate(\'/buyer-orders\')"><div class="profile-stat-num" id="statBuyOrders">-</div><div class="profile-stat-label">\u{4E70}\u{5165}</div></div>'
    +'<div class="profile-stat" onclick="navigate(\'/seller-orders\')"><div class="profile-stat-num" id="statSellOrders">-</div><div class="profile-stat-label">\u{5356}\u{51FA}</div></div>'
    +'<div class="profile-stat" onclick="navigate(\'/favorites\')"><div class="profile-stat-num" id="statFavs">-</div><div class="profile-stat-label">\u{6536}\u{85CF}</div></div></div>'
    +'<div class="profile-menu"><div class="profile-menu-item" onclick="navigate(\'/my-items\')">\u{1F4E6} \u{6211}\u{7684}\u{53D1}\u{5E03} <span>\u{2192}</span></div>'
    +'<div class="profile-menu-item" onclick="navigate(\'/buyer-orders\')">\u{1F6D2} \u{6211}\u{7684}\u{8BA2}\u{5355} <span>\u{2192}</span></div>'
    +'<div class="profile-menu-item" onclick="navigate(\'/seller-orders\')">\u{1F4B0} \u{6211}\u{7684}\u{5356}\u{5355} <span>\u{2192}</span></div>'
    +'<div class="profile-menu-item" onclick="navigate(\'/favorites\')">\u{2764}\u{FE0F} \u{6211}\u{7684}\u{6536}\u{85CF} <span>\u{2192}</span></div>'
    +'<div class="profile-menu-item" onclick="navigate(\'/messages\')">\u{1F4AC} \u{6211}\u{7684}\u{6D88}\u{606F} <span>\u{2192}</span></div></div>'
    +'<div class="profile-menu" style="margin-top:8px"><div class="profile-menu-item" onclick="showProfileEdit()">\u{270F}\u{FE0F} \u{7F16}\u{8F91}\u{8D44}\u{6599} <span>\u{2192}</span></div>'
    +'<div class="profile-menu-item" onclick="showPasswordChange()">\u{1F512} \u{4FEE}\u{6539}\u{5BC6}\u{7801} <span>\u{2192}</span></div></div>'
    +(u.role==='ADMIN'?'<div class="profile-menu" style="margin-top:8px"><div class="profile-menu-item" onclick="navigate(\'/admin\')" style="color:var(--primary)">\u{2699}\u{FE0F} \u{7BA1}\u{7406}\u{540E}\u{53F0} <span>\u{2192}</span></div></div>':'')
    +'<div style="padding:20px 16px"><button class="btn btn-block btn-outline" style="color:#FF4757;border-color:#FF4757" onclick="handleLogout()">\u{9000}\u{51FA}\u{767B}\u{5F55}</button></div></div>'
    +'<div class="tabbar"><div class="tabbar-item" onclick="navigate(\'/home\')"><span class="tab-icon">\u{1F3E0}</span><span>\u{9996}\u{9875}</span></div>'
    +'<div class="tabbar-item" onclick="checkAuthThen(\'/publish\')"><span class="tab-icon">\u{2795}</span><span>\u{53D1}\u{5E03}</span></div>'
    +'<div class="tabbar-item" onclick="checkAuthThen(\'/messages\')"><span class="tab-icon">\u{1F4AC}</span><span>\u{6D88}\u{606F}</span></div>'
    +'<div class="tabbar-item active" onclick="navigate(\'/profile\')"><span class="tab-icon">\u{1F464}</span><span>\u{6211}\u{7684}</span></div></div>';
}
async function loadProfileStats(){
    try{
        const [iR,bR,sR,fR]=await Promise.all([userApi.getMyItems(),orderApi.getBuyerOrders(),orderApi.getSellerOrders(),favoriteApi.getList()]);
        if(iR.code===200){const e=$el('statItems');if(e)e.textContent=(iR.data||[]).length;}
        if(bR.code===200){const e=$el('statBuyOrders');if(e)e.textContent=(bR.data||[]).length;}
        if(sR.code===200){const e=$el('statSellOrders');if(e)e.textContent=(sR.data||[]).length;}
        if(fR.code===200){const e=$el('statFavs');if(e)e.textContent=(fR.data||[]).length;}
    }catch(e){}
}
function handleLogout(){showModal('\u{9000}\u{51FA}\u{767B}\u{5F55}','\u{786E}\u{5B9A}\u{9000}\u{51FA}\u{5F53}\u{524D}\u{8D26}\u{53F7}\u{FF1F}').then(ok=>{if(ok)logout();});}
function showProfileEdit(){const u=AppState.user||{};appEl.innerHTML='<div class="page-no-tabbar"><div class="navbar"><span class="navbar-back" onclick="navigate(\'/profile\')">\u{2190}</span><span class="navbar-title">\u{7F16}\u{8F91}\u{8D44}\u{6599}</span><span class="navbar-right"></span></div><div style="padding:16px">'
    +'<div class="form-group"><input type="text" id="editNickname" value="'+esc(u.nickname||'')+'" placeholder="\u{6635}\u{79F0}"></div>'
    +'<div class="form-group"><input type="text" id="editPhone" value="'+(u.phone||'')+'" placeholder="\u{624B}\u{673A}\u{53F7}" maxlength="11"></div>'
    +'<button class="btn btn-primary btn-block btn-lg" onclick="saveProfile()">\u{4FDD}\u{5B58}</button></div></div>';}
async function saveProfile(){
    const n=getVal('editNickname'),p=getVal('editPhone');
    try{
        const r=await userApi.updateProfile({nickname:n,phone:p});
        if(r.code===200){const i=await authApi.getInfo();if(i.code===200&&i.data)saveUser(i.data);showToast('\u{4FDD}\u{5B58}\u{6210}\u{529F}');navigate('/profile');}
        else showToast(r.msg||'\u{4FDD}\u{5B58}\u{5931}\u{8D25}');
    }catch(e){showToast(e.message);}
}
function showPasswordChange(){appEl.innerHTML='<div class="page-no-tabbar"><div class="navbar"><span class="navbar-back" onclick="navigate(\'/profile\')">\u{2190}</span><span class="navbar-title">\u{4FEE}\u{6539}\u{5BC6}\u{7801}</span><span class="navbar-right"></span></div><div style="padding:16px">'
    +'<div class="form-group"><input type="password" id="oldPwd" placeholder="\u{5F53}\u{524D}\u{5BC6}\u{7801}"></div>'
    +'<div class="form-group"><input type="password" id="newPwd" placeholder="\u{65B0}\u{5BC6}\u{7801}\u{FF08}\u{81F3}\u{5C11}8\u{4F4D}\u{FF09}"></div>'
    +'<div class="form-group"><input type="password" id="confirmPwd" placeholder="\u{786E}\u{8BA4}\u{65B0}\u{5BC6}\u{7801}"></div>'
    +'<button class="btn btn-primary btn-block btn-lg" onclick="savePassword()">\u{4FEE}\u{6539}</button></div></div>';}
async function savePassword(){
    const o=getVal('oldPwd'),n=getVal('newPwd'),c=getVal('confirmPwd');
    if(!o||!n){showToast('\u{8BF7}\u{586B}\u{5199}\u{5B8C}\u{6574}');return;}
    if(n.length<8){showToast('\u{5BC6}\u{7801}\u{81F3}\u{5C11}8\u{4F4D}');return;}
    if(n!==c){showToast('\u{4E24}\u{6B21}\u{5BC6}\u{7801}\u{4E0D}\u{4E00}\u{81F4}');return;}
    try{const r=await userApi.updatePassword({oldPassword:o,newPassword:n});if(r.code===200){showToast('\u{4FEE}\u{6539}\u{6210}\u{529F}');navigate('/profile');}else showToast(r.msg||'\u{4FEE}\u{6539}\u{5931}\u{8D25}');}catch(e){showToast(e.message);}
}

// ---------- My Items ----------
function renderMyItems(){
    if(!requireAuth())return'';
    return '<div class="page-no-tabbar"><div class="navbar"><span class="navbar-back" onclick="history.back()">\u{2190}</span><span class="navbar-title">\u{6211}\u{7684}\u{53D1}\u{5E03}</span><span class="navbar-right"></span></div>'
    +'<div id="myItemsList"><div class="empty-state"><div class="loading-spinner"></div></div></div></div>';
}
async function loadMyItems(){
    try{
        const r=await userApi.getMyItems(),items=(r.code===200&&r.data)?r.data:[];
        const ct=$el('myItemsList');if(!ct)return;
        if(!items.length){ct.innerHTML='<div class="empty-state"><div class="icon">\u{1F4E6}</div><p>\u{8FD8}\u{6CA1}\u{6709}\u{53D1}\u{5E03}\u{5546}\u{54C1}</p><button class="btn btn-primary" onclick="navigate(\'/publish\')" style="margin-top:12px">\u{53BB}\u{53D1}\u{5E03}</button></div>';return;}
        ct.innerHTML=items.map(i=>'<div class="my-item-card"><img src="'+(i.images&&i.images[0]?getImageUrl(i.images[0]):'')+'" onerror="this.style.display=\'none\'" style="'+(i.images&&i.images[0]?'':'display:none')+'">'
        +'<div style="flex:1;min-width:0"><div class="my-item-title">'+esc(i.title||'')+'</div>'
        +'<div class="my-item-price">'+formatPrice(i.price)+'</div>'
        +'<div class="my-item-meta"><span class="my-item-status status-'+(i.status||'\u{5728}\u{552E}')+'">'+(i.status||'\u{5728}\u{552E}')+'</span><span>'+formatTime(i.createdAt)+'</span></div></div>'
        +'<div class="my-item-actions"><button class="btn btn-sm btn-outline" onclick="navigate(\'/edit/'+i.id+'\')">\u{7F16}\u{8F91}</button>'
        +'<button class="btn btn-sm btn-ghost" style="color:#FF4757" onclick="deleteItem('+i.id+')">\u{4E0B}\u{67B6}</button></div></div>').join('');
    }catch(e){showToast(e.message);}
}
async function deleteItem(id){
    const ok=await showModal('\u{4E0B}\u{67B6}\u{5546}\u{54C1}','\u{786E}\u{5B9A}\u{4E0B}\u{67B6}\u{6B64}\u{5546}\u{54C1}\u{FF1F}');
    if(!ok)return;
    try{const r=await itemApi.delete(id);if(r.code===200){showToast('\u{5DF2}\u{4E0B}\u{67B6}');loadMyItems();}else showToast(r.msg||'\u{64CD}\u{4F5C}\u{5931}\u{8D25}');}catch(e){showToast(e.message);}
}
// ---------- Buyer Orders ----------
function renderBuyerOrders(){
    if(!requireAuth())return'';
    return '<div class="page-no-tabbar"><div class="navbar"><span class="navbar-back" onclick="history.back()">\u{2190}</span><span class="navbar-title">\u{6211}\u{7684}\u{8BA2}\u{5355}</span><span class="navbar-right"></span></div>'
    +'<div id="buyerOrdersList"><div class="empty-state"><div class="loading-spinner"></div></div></div></div>';
}
async function loadBuyerOrders(){
    try{
        const r=await orderApi.getBuyerOrders(),orders=(r.code===200&&r.data)?r.data:[];
        const ct=$el('buyerOrdersList');if(!ct)return;
        if(!orders.length){ct.innerHTML='<div class="empty-state"><div class="icon">\u{1F6D2}</div><p>\u{6682}\u{65E0}\u{8BA2}\u{5355}</p></div>';return;}
        ct.innerHTML=orders.map(o=>'<div class="my-item-card"><img src="'+(o.itemImage?getImageUrl(o.itemImage):'')+'" onerror="this.style.display=\'none\'">'
        +'<div style="flex:1;min-width:0"><div class="my-item-title">'+esc(o.itemTitle||'')+'</div>'
        +'<div class="my-item-price">'+formatPrice(o.price)+'</div>'
        +'<div class="my-item-meta"><span class="my-item-status status-'+(o.status||'')+'">'+(o.status||'')+'</span><span>'+formatTime(o.createdAt)+'</span></div>'
        +'<div style="font-size:12px;color:var(--text-light)">\u{5356}\u{5BB6}\u{FF1A}'+esc(o.sellerName||'')+'</div></div>'
        +'<div class="my-item-actions">'+(o.status==='\u{5F85}\u{53D1}\u{8D27}'?'<button class="btn btn-sm btn-ghost" style="color:#FF4757" onclick="cancelOrder('+o.id+')">\u{53D6}\u{6D88}</button>':'')
        +(o.status==='\u{5F85}\u{6536}\u{8D27}'?'<button class="btn btn-sm btn-primary" onclick="confirmOrder('+o.id+')">\u{786E}\u{8BA4}\u{6536}\u{8D27}</button>':'')+'</div></div>').join('');
    }catch(e){showToast(e.message);}
}
async function cancelOrder(id){const ok=await showModal('\u{53D6}\u{6D88}\u{8BA2}\u{5355}','\u{786E}\u{5B9A}\u{53D6}\u{6D88}\u{6B64}\u{8BA2}\u{5355}\u{FF1F}');if(!ok)return;try{const r=await orderApi.cancel(id);if(r.code===200){showToast('\u{5DF2}\u{53D6}\u{6D88}');loadBuyerOrders();}else showToast(r.msg||'\u{64CD}\u{4F5C}\u{5931}\u{8D25}');}catch(e){showToast(e.message);}}
async function confirmOrder(id){const ok=await showModal('\u{786E}\u{8BA4}\u{6536}\u{8D27}','\u{786E}\u{5B9A}\u{5DF2}\u{6536}\u{5230}\u{5546}\u{54C1}\u{FF1F}\u{786E}\u{8BA4}\u{540E}\u{6B3E}\u{9879}\u{5C06}\u{7ED3}\u{7B97}\u{7ED9}\u{5356}\u{5BB6}\u{3002}');if(!ok)return;try{const r=await orderApi.confirm(id);if(r.code===200){showToast('\u{5DF2}\u{786E}\u{8BA4}\u{6536}\u{8D27}');loadBuyerOrders();}else showToast(r.msg||'\u{64CD}\u{4F5C}\u{5931}\u{8D25}');}catch(e){showToast(e.message);}}

// ---------- Seller Orders ----------
function renderSellerOrders(){
    if(!requireAuth())return'';
    return '<div class="page-no-tabbar"><div class="navbar"><span class="navbar-back" onclick="history.back()">\u{2190}</span><span class="navbar-title">\u{6211}\u{7684}\u{5356}\u{5355}</span><span class="navbar-right"></span></div>'
    +'<div id="sellerOrdersList"><div class="empty-state"><div class="loading-spinner"></div></div></div></div>';
}
async function loadSellerOrders(){
    try{
        const r=await orderApi.getSellerOrders(),orders=(r.code===200&&r.data)?r.data:[];
        const ct=$el('sellerOrdersList');if(!ct)return;
        if(!orders.length){ct.innerHTML='<div class="empty-state"><div class="icon">\u{1F4B0}</div><p>\u{6682}\u{65E0}\u{5356}\u{5355}</p></div>';return;}
        ct.innerHTML=orders.map(o=>'<div class="my-item-card"><img src="'+(o.itemImage?getImageUrl(o.itemImage):'')+'" onerror="this.style.display=\'none\'">'
        +'<div style="flex:1;min-width:0"><div class="my-item-title">'+esc(o.itemTitle||'')+'</div>'
        +'<div class="my-item-price">'+formatPrice(o.price)+'</div>'
        +'<div style="font-size:12px;color:var(--text-secondary)">\u{4F63}\u{91D1}\u{FF1A}'+formatPrice(o.commission||0)+' | \u{6536}\u{5165}\u{FF1A}'+formatPrice(o.sellerIncome||0)+'</div>'
        +'<div class="my-item-meta"><span class="my-item-status status-'+(o.status||'')+'">'+(o.status||'')+'</span><span>'+formatTime(o.createdAt)+'</span></div></div>'
        +'<div class="my-item-actions">'+(o.status==='\u{5F85}\u{53D1}\u{8D27}'?'<button class="btn btn-sm btn-primary" onclick="shipOrder('+o.id+')">\u{53D1}\u{8D27}</button>':'')+'</div></div>').join('');
    }catch(e){showToast(e.message);}
}
async function shipOrder(id){const ok=await showModal('\u{786E}\u{8BA4}\u{53D1}\u{8D27}','\u{786E}\u{5B9A}\u{5DF2}\u{53D1}\u{8D27}\u{FF1F}');if(!ok)return;try{const r=await orderApi.ship(id);if(r.code===200){showToast('\u{5DF2}\u{6807}\u{8BB0}\u{53D1}\u{8D27}');loadSellerOrders();}else showToast(r.msg||'\u{64CD}\u{4F5C}\u{5931}\u{8D25}');}catch(e){showToast(e.message);}}

// ---------- Favorites ----------
function renderFavorites(){
    if(!requireAuth())return'';
    return '<div class="page-no-tabbar"><div class="navbar"><span class="navbar-back" onclick="history.back()">\u{2190}</span><span class="navbar-title">\u{6211}\u{7684}\u{6536}\u{85CF}</span><span class="navbar-right"></span></div>'
    +'<div id="favList"><div class="empty-state"><div class="loading-spinner"></div></div></div></div>';
}
async function loadFavorites(){
    try{
        const r=await favoriteApi.getList(),items=(r.code===200&&r.data)?r.data:[];
        const ct=$el('favList');if(!ct)return;
        if(!items.length){ct.innerHTML='<div class="empty-state"><div class="icon">\u{2764}\u{FE0F}</div><p>\u{8FD8}\u{6CA1}\u{6709}\u{6536}\u{85CF}\u{7684}\u{5546}\u{54C1}</p></div>';return;}
        ct.innerHTML='<div class="waterfall" style="padding:12px"></div>';
        const wf=ct.querySelector('.waterfall');items.forEach(i=>wf.appendChild(createItemCard(i)));
    }catch(e){showToast(e.message);}
}

// ---------- Messages ----------
function renderMessages(){
    if(!requireAuth())return'';
    return '<div class="page"><div class="navbar"><span class="navbar-back" onclick="history.back()">\u{2190}</span><span class="navbar-title">\u{6D88}\u{606F}</span><span class="navbar-right"></span></div>'
    +'<div id="msgList"><div class="empty-state"><div class="loading-spinner"></div></div></div></div>'
    +'<div class="tabbar"><div class="tabbar-item" onclick="navigate(\'/home\')"><span class="tab-icon">\u{1F3E0}</span><span>\u{9996}\u{9875}</span></div>'
    +'<div class="tabbar-item" onclick="checkAuthThen(\'/publish\')"><span class="tab-icon">\u{2795}</span><span>\u{53D1}\u{5E03}</span></div>'
    +'<div class="tabbar-item active" onclick="navigate(\'/messages\')"><span class="tab-icon">\u{1F4AC}</span><span>\u{6D88}\u{606F}</span></div>'
    +'<div class="tabbar-item" onclick="navigate(\'/profile\')"><span class="tab-icon">\u{1F464}</span><span>\u{6211}\u{7684}</span></div></div>';
}
async function loadMessages(){
    try{
        const r=await messageApi.getSessions(),sessions=(r.code===200&&r.data)?r.data:[];
        const ct=$el('msgList');if(!ct)return;
        if(!sessions.length){ct.innerHTML='<div class="empty-state"><div class="icon">\u{1F4AC}</div><p>\u{6682}\u{65E0}\u{6D88}\u{606F}</p></div>';return;}
        ct.innerHTML=sessions.map(s=>'<div class="msg-session" onclick="navigate(\'/chat/'+s.otherUserId+'?itemId='+(s.itemId||'')+'\')">'
        +'<div class="msg-avatar">'+(s.otherNickname?s.otherNickname[0]:'?')+'</div>'
        +'<div class="msg-info"><div class="msg-name">'+esc(s.otherNickname||'\u{672A}\u{77E5}')+(s.unread>0?'<span class="msg-badge">'+s.unread+'</span>':'')+'</div>'
        +'<div class="msg-preview">'+esc(s.lastContent||'')+'</div></div>'
        +'<div class="msg-time">'+formatTime(s.lastTime)+'</div></div>').join('');
    }catch(e){showToast(e.message);}
}

// ---------- Chat ----------
function renderChat(){return '<div class="page-no-tabbar"><div class="navbar"><span class="navbar-back" onclick="history.back()">\u{2190}</span><span class="navbar-title">\u{804A}\u{5929}</span><span class="navbar-right"></span></div>'
    +'<div class="chat-body" id="chatBody"><div class="empty-state"><div class="loading-spinner"></div></div></div>'
    +'<div class="chat-input-bar"><input type="text" id="chatInput" placeholder="\u{8F93}\u{5165}\u{6D88}\u{606F}..." onkeydown="if(event.key===\'Enter\')sendMessage()">'
    +'<button class="btn btn-primary btn-sm" onclick="sendMessage()">\u{53D1}\u{9001}</button></div></div>';}
let chatOtherUserId=null,chatItemId=null;
async function loadChat(otherUserId,itemId){
    chatOtherUserId=otherUserId;chatItemId=itemId;
    try{
        await messageApi.markRead({otherUserId:parseInt(otherUserId)});
        const p={otherUserId:parseInt(otherUserId)};if(itemId)p.itemId=parseInt(itemId);
        const r=await messageApi.getChat(p),msgs=(r.code===200&&r.data)?r.data:[];
        const ct=$el('chatBody');if(!ct)return;
        const uid=getCurrentUserId();
        if(!msgs.length){ct.innerHTML='<div class="empty-state"><div class="icon">\u{1F4AC}</div><p>\u{5F00}\u{59CB}\u{804A}\u{5929}\u{5427}</p></div>';return;}
        ct.innerHTML=msgs.map(m=>'<div class="chat-msg '+(m.fromUserId==uid?'chat-msg-self':'chat-msg-other')+'"><div class="chat-bubble">'+esc(m.content)+'</div><div class="chat-time">'+formatTime(m.createdAt)+'</div></div>').join('');
        ct.scrollTop=ct.scrollHeight;
    }catch(e){showToast(e.message);}
}
async function sendMessage(){
    const c=getVal('chatInput');if(!c)return;
    if(!chatOtherUserId){showToast('\u{8BF7}\u{5148}\u{9009}\u{62E9}\u{804A}\u{5929}\u{5BF9}\u{8C61}');return;}
    try{
        const r=await messageApi.send({toUserId:parseInt(chatOtherUserId),content:c,...(chatItemId?{itemId:parseInt(chatItemId)}:{})});
        if(r.code===200){setVal('chatInput','');loadChat(chatOtherUserId,chatItemId);}else showToast(r.msg||'\u{53D1}\u{9001}\u{5931}\u{8D25}');
    }catch(e){showToast(e.message);}
}
// ===================== Admin =====================
function renderAdmin(params){
    const tab=params.tab||'dashboard';
    return '<div class="page-no-tabbar"><div class="admin-layout"><aside class="admin-sidebar"><div class="admin-logo">\u{1F3EA} \u{7BA1}\u{7406}\u{540E}\u{53F0}</div>'
    +'<nav class="admin-nav"><a class="admin-nav-item '+(tab==='dashboard'?'active':'')+'" onclick="navigate(\'/admin\')">\u{1F4CA} \u{6982}\u{89C8}</a>'
    +'<a class="admin-nav-item '+(tab==='users'?'active':'')+'" onclick="navigate(\'/admin/users\')">\u{1F465} \u{7528}\u{6237}</a>'
    +'<a class="admin-nav-item '+(tab==='items'?'active':'')+'" onclick="navigate(\'/admin/items\')">\u{1F4E6} \u{5546}\u{54C1}</a>'
    +'<a class="admin-nav-item '+(tab==='orders'?'active':'')+'" onclick="navigate(\'/admin/orders\')">\u{1F4CB} \u{8BA2}\u{5355}</a>'
    +'<a class="admin-nav-item '+(tab==='categories'?'active':'')+'" onclick="navigate(\'/admin/categories\')">\u{1F3F7}\u{FE0F} \u{5206}\u{7C7B}</a>'
    +'<a class="admin-nav-item '+(tab==='revenue'?'active':'')+'" onclick="navigate(\'/admin/revenue\')">\u{1F4B0} \u{6536}\u{5165}</a></nav>'
    +'<div class="admin-nav-bottom"><a class="admin-nav-item" onclick="navigate(\'/home\')">\u{2190} \u{8FD4}\u{56DE}\u{524D}\u{53F0}</a></div></aside>'
    +'<main class="admin-content" id="adminContent"><div class="loading-spinner"></div></main></div></div>';
}

async function loadAdminTab(tab){
    const el=$el('adminContent');if(!el)return;
    try{
        if(tab==='dashboard'){const r=await adminApi.getStats(),s=r.data||{};el.innerHTML='<h2 style="font-size:20px;font-weight:600;margin-bottom:20px">\u{7CFB}\u{7EDF}\u{6982}\u{89C8}</h2><div class="stat-grid">'
        +'<div class="stat-card" style="--c:#FF6B35"><div class="stat-num">'+(s.userCount||0)+'</div><div class="stat-label">\u{7528}\u{6237}\u{603B}\u{6570}</div></div>'
        +'<div class="stat-card" style="--c:#0E9F6E"><div class="stat-num">'+(s.itemCount||0)+'</div><div class="stat-label">\u{5546}\u{54C1}\u{603B}\u{6570}</div></div>'
        +'<div class="stat-card" style="--c:#3B82F6"><div class="stat-num">'+(s.orderCount||0)+'</div><div class="stat-label">\u{8BA2}\u{5355}\u{603B}\u{6570}</div></div>'
        +'<div class="stat-card" style="--c:#F59E0B"><div class="stat-num">'+formatPrice(s.totalCommission||0)+'</div><div class="stat-label">\u{603B}\u{4F63}\u{91D1}</div></div>'
        +'<div class="stat-card" style="--c:#8B5CF6"><div class="stat-num">'+formatPrice(s.todayCommission||0)+'</div><div class="stat-label">\u{4ECA}\u{65E5}\u{4F63}\u{91D1}</div></div>'
        +'<div class="stat-card" style="--c:#EC4899"><div class="stat-num">'+(s.todayOrders||0)+'</div><div class="stat-label">\u{4ECA}\u{65E5}\u{8BA2}\u{5355}</div></div></div>';return;}
        if(tab==='users'){await renderAdminUsers(el);return;}
        if(tab==='items'){await renderAdminItems(el);return;}
        if(tab==='orders'){await renderAdminOrders(el);return;}
        if(tab==='categories'){await renderAdminCategories(el);return;}
        if(tab==='revenue'){await renderAdminRevenue(el);return;}
    }catch(e){el.innerHTML='<div class="empty-state"><div class="icon">\u{1F635}</div><p>\u{52A0}\u{8F7D}\u{5931}\u{8D25}</p></div>';}
}

let auPage=1,auKw='';
async function renderAdminUsers(el){
    const r=await adminApi.getUsers({page:auPage,size:20,keyword:auKw}),d=r.data||{},users=d.data||[],total=d.total||0;
    el.innerHTML='<h2 style="font-size:20px;font-weight:600;margin-bottom:16px">\u{7528}\u{6237}\u{7BA1}\u{7406}</h2>'
    +'<div class="admin-search"><input type="text" id="auKw" placeholder="\u{641C}\u{7D22}\u{7528}\u{6237}\u{540D}/\u{624B}\u{673A}\u{53F7}" value="'+auKw+'" onkeydown="if(event.key===\'Enter\'){auKw=this.value;auPage=1;renderAdminUsers($el(\'adminContent\'));}">'
    +'<button class="btn btn-sm btn-primary" onclick="auKw=$el(\'auKw\').value;auPage=1;renderAdminUsers($el(\'adminContent\'))">\u{641C}\u{7D22}</button></div>'
    +'<div class="admin-table-wrap"><table class="admin-table"><thead><tr><th>ID</th><th>\u{7528}\u{6237}\u{540D}</th><th>\u{6635}\u{79F0}</th><th>\u{624B}\u{673A}\u{53F7}</th><th>\u{89D2}\u{8272}</th><th>\u{4F59}\u{989D}</th><th>\u{72B6}\u{6001}</th><th>\u{6CE8}\u{518C}\u{65F6}\u{95F4}</th><th>\u{64CD}\u{4F5C}</th></tr></thead>'
    +'<tbody>'+users.map(u=>'<tr><td>'+u.id+'</td><td>'+esc(u.username||'')+'</td><td>'+esc(u.nickname||'')+'</td><td>'+(u.phone||'')+'</td>'
    +'<td><span class="admin-badge '+(u.role==='ADMIN'?'badge-admin':'')+'">'+(u.role||'USER')+'</span></td><td>'+formatPrice(u.balance)+'</td>'
    +'<td><span class="admin-badge '+(u.status===1?'badge-on':'badge-off')+'">'+(u.status===1?'\u{6B63}\u{5E38}':'\u{7981}\u{7528}')+'</span></td>'
    +'<td>'+formatTime(u.createdAt)+'</td>'
    +'<td class="admin-actions">'+(u.role!=='ADMIN'?'<button class="btn btn-sm btn-outline" onclick="toggleUserStatus('+u.id+','+u.status+')">'+(u.status===1?'\u{7981}\u{7528}':'\u{542F}\u{7528}')+'</button>':'')
    +(u.role!=='ADMIN'?'<button class="btn btn-sm btn-ghost" style="color:#FF4757" onclick="deleteUser('+u.id+')">\u{5220}\u{9664}</button>':'')+'</td></tr>').join('')+'</tbody></table></div>'
    +'<div class="admin-pager"><button onclick="auPage--;renderAdminUsers($el(\'adminContent\'))" '+(auPage<=1?'disabled':'')+'>\u{4E0A}\u{4E00}\u{9875}</button>'
    +'<span>\u{7B2C} '+auPage+' \u{9875} / \u{5171} '+Math.ceil(total/20)+' \u{9875}</span>'
    +'<button onclick="auPage++;renderAdminUsers($el(\'adminContent\'))" '+(auPage*20>=total?'disabled':'')+'>\u{4E0B}\u{4E00}\u{9875}</button></div>';
}
async function toggleUserStatus(id,cur){try{const r=await adminApi.updateUserStatus(id,{status:cur===1?0:1});if(r.code===200){showToast('\u{64CD}\u{4F5C}\u{6210}\u{529F}');renderAdminUsers($el('adminContent'));}else showToast(r.msg||'\u{64CD}\u{4F5C}\u{5931}\u{8D25}');}catch(e){showToast(e.message);}}
async function deleteUser(id){const ok=await showModal('\u{5220}\u{9664}\u{7528}\u{6237}','\u{786E}\u{5B9A}\u{5220}\u{9664}\u{6B64}\u{7528}\u{6237}\u{FF1F}\u{FF08}\u{4E0D}\u{53EF}\u{6062}\u{590D}\u{FF09}');if(!ok)return;try{const r=await adminApi.deleteUser(id);if(r.code===200){showToast('\u{5DF2}\u{5220}\u{9664}');renderAdminUsers($el('adminContent'));}else showToast(r.msg||'\u{64CD}\u{4F5C}\u{5931}\u{8D25}');}catch(e){showToast(e.message);}}

let aiPage=1,aiKw='',aiSt='';
async function renderAdminItems(el){
    const r=await adminApi.getItems({page:aiPage,size:20,keyword:aiKw,status:aiSt}),d=r.data||{},items=d.data||[],total=d.total||0;
    el.innerHTML='<h2 style="font-size:20px;font-weight:600;margin-bottom:16px">\u{5546}\u{54C1}\u{7BA1}\u{7406}</h2>'
    +'<div class="admin-search"><input type="text" id="aiKw" placeholder="\u{641C}\u{7D22}\u{5546}\u{54C1}\u{6807}\u{9898}" value="'+aiKw+'" onkeydown="if(event.key===\'Enter\'){aiKw=this.value;aiPage=1;renderAdminItems($el(\'adminContent\'));}">'
    +'<select id="aiSt" onchange="aiSt=this.value;aiPage=1;renderAdminItems($el(\'adminContent\'))"><option value="">\u{5168}\u{90E8}\u{72B6}\u{6001}</option>'
    +'<option value="\u{5728}\u{552E}" '+(aiSt==='\u{5728}\u{552E}'?'selected':'')+'>\u{5728}\u{552E}</option>'
    +'<option value="\u{5DF2}\u{552E}" '+(aiSt==='\u{5DF2}\u{552E}'?'selected':'')+'>\u{5DF2}\u{552E}</option>'
    +'<option value="\u{4E0B}\u{67B6}" '+(aiSt==='\u{4E0B}\u{67B6}'?'selected':'')+'>\u{4E0B}\u{67B6}</option></select>'
    +'<button class="btn btn-sm btn-primary" onclick="aiKw=$el(\'aiKw\').value;aiPage=1;renderAdminItems($el(\'adminContent\'))">\u{641C}\u{7D22}</button></div>'
    +'<div class="admin-table-wrap"><table class="admin-table"><thead><tr><th>ID</th><th>\u{6807}\u{9898}</th><th>\u{4EF7}\u{683C}</th><th>\u{5206}\u{7C7B}</th><th>\u{6210}\u{8272}</th><th>\u{72B6}\u{6001}</th><th>\u{6D4F}\u{89C8}\u{91CF}</th><th>\u{53D1}\u{5E03}\u{65F6}\u{95F4}</th><th>\u{64CD}\u{4F5C}</th></tr></thead>'
    +'<tbody>'+items.map(i=>'<tr><td>'+i.id+'</td><td class="td-title">'+esc(i.title||'')+'</td><td>'+formatPrice(i.price)+'</td><td>'+(i.categoryName||'')+'</td><td>'+(i.condition||'')+'</td>'
    +'<td><span class="admin-badge '+(i.status==='\u{5728}\u{552E}'?'badge-on':i.status==='\u{5DF2}\u{552E}'?'':'badge-off')+'">'+(i.status||'')+'</span></td><td>'+(i.views||0)+'</td>'
    +'<td>'+formatTime(i.createdAt)+'</td><td class="admin-actions">'+(i.status!=='\u{4E0B}\u{67B6}'?'<button class="btn btn-sm btn-ghost" style="color:#FF4757" onclick="offlineItem('+i.id+')">\u{4E0B}\u{67B6}</button>':'')+'</td></tr>').join('')+'</tbody></table></div>'
    +'<div class="admin-pager"><button onclick="aiPage--;renderAdminItems($el(\'adminContent\'))" '+(aiPage<=1?'disabled':'')+'>\u{4E0A}\u{4E00}\u{9875}</button>'
    +'<span>\u{7B2C} '+aiPage+' \u{9875} / \u{5171} '+Math.ceil(total/20)+' \u{9875}</span>'
    +'<button onclick="aiPage++;renderAdminItems($el(\'adminContent\'))" '+(aiPage*20>=total?'disabled':'')+'>\u{4E0B}\u{4E00}\u{9875}</button></div>';
}
async function offlineItem(id){const ok=await showModal('\u{4E0B}\u{67B6}\u{5546}\u{54C1}','\u{786E}\u{5B9A}\u{4E0B}\u{67B6}\u{6B64}\u{5546}\u{54C1}\u{FF1F}');if(!ok)return;try{const r=await adminApi.offlineItem(id);if(r.code===200){showToast('\u{5DF2}\u{4E0B}\u{67B6}');renderAdminItems($el('adminContent'));}else showToast(r.msg||'\u{64CD}\u{4F5C}\u{5931}\u{8D25}');}catch(e){showToast(e.message);}}

let aoPage=1;
async function renderAdminOrders(el){
    const r=await adminApi.getOrders({page:aoPage,size:20}),d=r.data||{},orders=d.data||[],total=d.total||0;
    el.innerHTML='<h2 style="font-size:20px;font-weight:600;margin-bottom:16px">\u{8BA2}\u{5355}\u{7BA1}\u{7406}</h2>'
    +'<div class="admin-table-wrap"><table class="admin-table"><thead><tr><th>\u{8BA2}\u{5355}\u{53F7}</th><th>\u{5546}\u{54C1}</th><th>\u{91D1}\u{989D}</th><th>\u{4F63}\u{91D1}</th><th>\u{5356}\u{5BB6}\u{6536}\u{5165}</th><th>\u{72B6}\u{6001}</th><th>\u{4E70}\u{5BB6}</th><th>\u{65F6}\u{95F4}</th></tr></thead>'
    +'<tbody>'+orders.map(o=>'<tr><td>'+(o.orderNo||'')+'</td><td class="td-title">'+esc(o.itemTitle||'')+'</td><td>'+formatPrice(o.price)+'</td><td>'+formatPrice(o.commission||0)+'</td><td>'+formatPrice(o.sellerIncome||0)+'</td>'
    +'<td><span class="admin-badge">'+(o.status||'')+'</span></td><td>'+esc(o.buyerName||'')+'</td><td>'+formatTime(o.createdAt)+'</td></tr>').join('')+'</tbody></table></div>'
    +'<div class="admin-pager"><button onclick="aoPage--;renderAdminOrders($el(\'adminContent\'))" '+(aoPage<=1?'disabled':'')+'>\u{4E0A}\u{4E00}\u{9875}</button>'
    +'<span>\u{7B2C} '+aoPage+' \u{9875} / \u{5171} '+Math.ceil(total/20)+' \u{9875}</span>'
    +'<button onclick="aoPage++;renderAdminOrders($el(\'adminContent\'))" '+(aoPage*20>=total?'disabled':'')+'>\u{4E0B}\u{4E00}\u{9875}</button></div>';
}

async function renderAdminCategories(el){
    const r=await categoryApi.getList(),cats=(r.code===200&&r.data)?r.data:[];
    el.innerHTML='<h2 style="font-size:20px;font-weight:600;margin-bottom:16px">\u{5206}\u{7C7B}\u{7BA1}\u{7406}</h2>'
    +'<div class="admin-table-wrap"><table class="admin-table"><thead><tr><th>ID</th><th>\u{540D}\u{79F0}</th><th>\u{56FE}\u{6807}</th><th>\u{6392}\u{5E8F}</th></tr></thead>'
    +'<tbody>'+cats.map(c=>'<tr><td>'+c.id+'</td><td>'+esc(c.name||'')+'</td><td>'+(c.icon||'')+'</td><td>'+(c.sort||0)+'</td></tr>').join('')+'</tbody></table></div>';
}

async function renderAdminRevenue(el){
    try{
        const r=await adminApi.getRevenue(),revenues=(r.code===200&&r.data)?r.data:[];
        let html='<h2 style="font-size:20px;font-weight:600;margin-bottom:16px">\u{6536}\u{5165}\u{7EDF}\u{8BA1}</h2><div class="revenue-chart"><div class="chart-title">\u{5E73}\u{53F0}\u{4F63}\u{91D1}\u{8D8B}\u{52BF}</div><div class="chart-bars">';
        const max=Math.max(...revenues.map(r=>r.amount||0),1);
        revenues.forEach(r=>{const pct=Math.max((r.amount/max)*100,4);html+='<div class="chart-bar"><span class="bar-val">'+formatPrice(r.amount||0)+'</span><div class="bar-fill" style="height:'+pct+'%"></div><span class="bar-date">'+(r.date||'')+'</span></div>';});
        html+='</div></div>';el.innerHTML=html;
    }catch(e){el.innerHTML='<div class="empty-state"><div class="icon">\u{1F635}</div><p>\u{52A0}\u{8F7D}\u{5931}\u{8D25}</p></div>';}
}
// ===================== Edit Form =====================
async function loadEditForm(id){
    try{
        const r=await itemApi.getById(id),item=r.data||{},imgs=item.images||[];
        const uid=getCurrentUserId();
        if(item.userId!=uid){showToast('\u{65E0}\u{6743}\u{7F16}\u{8F91}\u{6B64}\u{5546}\u{54C1}');navigate('/my-items');return;}
        const cr=await categoryApi.getList(),cats=(cr.code===200&&cr.data)?cr.data:[];
        const el=$el('editContent');if(!el)return;
        uploadedImages=[...imgs];
        el.innerHTML='<div class="form-group"><input type="text" id="editTitle" value="'+esc(item.title||'')+'" maxlength="100"></div>'
        +'<div class="form-group"><select id="editCategory">'+cats.map(c=>'<option value="'+c.id+'" '+(c.id==item.categoryId?'selected':'')+'>'+esc(c.name)+'</option>').join('')+'</select></div>'
        +'<div class="form-group"><select id="editCondition"><option value="\u{5168}\u{65B0}" '+(item.condition==='\u{5168}\u{65B0}'?'selected':'')+'>\u{5168}\u{65B0}</option><option value="\u{51E0}\u{4E4E}\u{5168}\u{65B0}" '+(item.condition==='\u{51E0}\u{4E4E}\u{5168}\u{65B0}'?'selected':'')+'>\u{51E0}\u{4E4E}\u{5168}\u{65B0}</option><option value="\u{8F7B}\u{5FAE}\u{4F7F}\u{7528}\u{75D5}\u{8FF9}" '+(item.condition==='\u{8F7B}\u{5FAE}\u{4F7F}\u{7528}\u{75D5}\u{8FF9}'?'selected':'')+'>\u{8F7B}\u{5FAE}\u{4F7F}\u{7528}\u{75D5}\u{8FF9}</option><option value="\u{660E}\u{663E}\u{4F7F}\u{7528}\u{75D5}\u{8FF9}" '+(item.condition==='\u{660E}\u{663E}\u{4F7F}\u{7528}\u{75D5}\u{8FF9}'?'selected':'')+'>\u{660E}\u{663E}\u{4F7F}\u{7528}\u{75D5}\u{8FF9}</option></select></div>'
        +'<div class="form-group"><div style="position:relative"><span style="position:absolute;left:14px;top:50%;transform:translateY(-50%);color:var(--text-light);font-weight:600">\u{FFE5}</span><input type="number" id="editPrice" value="'+(item.price||'')+'" min="0" step="0.01" style="padding-left:32px"></div></div>'
        +'<div class="form-group"><textarea id="editDesc" rows="5">'+esc(item.description||'')+'</textarea></div>'
        +'<div class="form-group"><label style="display:block;margin-bottom:8px;font-weight:500;color:var(--text-secondary)">\u{5546}\u{54C1}\u{56FE}\u{7247}\u{FF08}\u{5DF2}\u{6709} '+imgs.length+' \u{5F20}\u{FF09}</label>'
        +'<div class="upload-previews" id="editPreviews">'+imgs.map(u=>'<div class="upload-preview"><img src="'+getImageUrl(u)+'"><span class="upload-remove" onclick="var u=\''+u+'\';uploadedImages=uploadedImages.filter(x=>x!==u);this.parentElement.remove()">\u{D7}</span></div>').join('')+'</div>'
        +'<div class="upload-area" onclick="$el(\'editFileInput\').click()" style="margin-top:8px;padding:16px"><span>+ \u{6DFB}\u{52A0}\u{56FE}\u{7247}</span>'
        +'<input type="file" id="editFileInput" accept="image/*" multiple style="display:none" onchange="handleUpload()"></div></div>'
        +'<button class="btn btn-primary btn-block btn-lg" onclick="handleUpdate('+id+')">\u{4FDD}\u{5B58}\u{4FEE}\u{6539}</button>';
    }catch(e){showToast(e.message);}
}
async function handleUpdate(id){
    const t=getVal('editTitle'),c=getVal('editCategory'),cd=getVal('editCondition'),p=getVal('editPrice'),d=getVal('editDesc');
    if(!t){showToast('\u{8BF7}\u{8F93}\u{5165}\u{5546}\u{54C1}\u{6807}\u{9898}');return;}
    if(!p||parseFloat(p)<=0){showToast('\u{8BF7}\u{8F93}\u{5165}\u{6709}\u{6548}\u{4EF7}\u{683C}');return;}
    try{const r=await itemApi.update(id,{title:t,categoryId:parseInt(c),condition:cd,price:parseFloat(p),description:d,images:uploadedImages});if(r.code===200){showToast('\u{4FDD}\u{5B58}\u{6210}\u{529F}');navigate('/my-items');}else showToast(r.msg||'\u{4FDD}\u{5B58}\u{5931}\u{8D25}');}catch(e){showToast(e.message);}
}

// ===================== Category loading =====================
async function loadCategories(){
    try{
        const r=await categoryApi.getList();AppState.categories=(r.code===200&&r.data)?r.data:[];
        const ch=$el('categoryChips'),pc=$el('pubCategory');
        if(ch)AppState.categories.forEach(c=>{const d=document.createElement('div');d.className='home-cat-chip';d.dataset.cat=c.id;d.textContent=c.name;d.onclick=()=>filterCategory(c.id);ch.appendChild(d);});
        if(pc)AppState.categories.forEach(c=>{const o=document.createElement('option');o.value=c.id;o.textContent=c.name;pc.appendChild(o);});
    }catch(e){}
}

async function loadCategoryItems(reset){
    const id=AppState.params?.id;if(!id)return;
    if(reset){catPage=1;catHasMore=true;}
    if(catLoading||!catHasMore)return;
    catLoading=true;
    try{
        const r=await itemApi.byCategory(id,{page:catPage,size:20}),items=r.data?.data||[];
        const ct=$el('catWaterfall');if(!ct)return;
        if(reset)ct.innerHTML='';
        if(items.length<20)catHasMore=false;
        items.forEach(i=>ct.appendChild(createItemCard(i)));catPage++;
    }catch(e){showToast(e.message);}finally{catLoading=false;}
}

// ===================== Lifecycle =====================
pageLifecycle.home={mount:function(){homePage=1;homeHasMore=true;loadCategories();loadHomeItems(true);this.sh=()=>{if((window.scrollY||document.documentElement.scrollTop)+document.documentElement.clientHeight>=document.documentElement.scrollHeight-200)loadHomeItems(false);};window.addEventListener('scroll',this.sh);},unmount:function(){if(this.sh)window.removeEventListener('scroll',this.sh);}};
pageLifecycle.profile={mount:function(){if(isLoggedIn())loadProfileStats();}};
pageLifecycle['my-items']={mount:function(){if(isLoggedIn())loadMyItems();}};
pageLifecycle['buyer-orders']={mount:function(){if(isLoggedIn())loadBuyerOrders();}};
pageLifecycle['seller-orders']={mount:function(){if(isLoggedIn())loadSellerOrders();}};
pageLifecycle.favorites={mount:function(){if(isLoggedIn())loadFavorites();}};
pageLifecycle.messages={mount:function(){if(isLoggedIn())loadMessages();}};
pageLifecycle.item={mount:function(){const id=AppState.params?.id;if(id)loadItemDetail(id);}};
pageLifecycle.edit={mount:function(){const id=AppState.params?.id;if(id)loadEditForm(id);}};
pageLifecycle.chat={mount:function(){const p=AppState.params;if(p?.otherUserId)loadChat(p.otherUserId,p?.query?.itemId);}};
pageLifecycle.search={mount:function(){searchPage=1;searchHasMore=true;const q=AppState.params?.query?.q;if(q){setTimeout(()=>{setVal('searchInput',q);doSearch();},100);}}};
pageLifecycle.category={mount:function(){catPage=1;catHasMore=true;loadCategoryItems(true);}};
pageLifecycle.admin={mount:function(){const tab=AppState.params?.tab||'dashboard';setTimeout(()=>loadAdminTab(tab),100);}};

// ===================== Init =====================
loadUser();
if(typeof API_BASE==='undefined'){var API_BASE='http://localhost:8080/api';}
if(typeof IMG_BASE==='undefined'){var IMG_BASE='http://localhost:8080';}

route('/login',function(){renderPage('login',{});});
route('/register',function(){renderPage('register',{});});
route('/home',function(){renderPage('home',{});});
route('/publish',function(){renderPage('publish',{});});
route('/profile',function(){renderPage('profile',{});});
route('/my-items',function(){renderPage('my-items',{});});
route('/buyer-orders',function(){renderPage('buyer-orders',{});});
route('/seller-orders',function(){renderPage('seller-orders',{});});
route('/favorites',function(){renderPage('favorites',{});});
route('/messages',function(){renderPage('messages',{});});

window.addEventListener('hashchange',router);
router();
