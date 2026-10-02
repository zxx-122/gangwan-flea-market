/**
 * 港湾跳蚤市场 — 视觉特效引擎
 */

// ========== Canvas 粒子系统 ==========
class ParticleSystem {
    constructor(container, options = {}) {
        this.container = container;
        this.options = Object.assign({
            count: 60,
            color: 'rgba(255,122,69,0.3)',
            lineColor: 'rgba(255,122,69,0.08)',
            speed: 0.4,
            radius: 2,
            connectDistance: 120
        }, options);

        this.canvas = document.createElement('canvas');
        this.canvas.className = 'particle-canvas';
        this.ctx = this.canvas.getContext('2d');
        this.particles = [];
        this.animId = null;

        container.style.position = 'relative';
        this.canvas.style.cssText = 'position:absolute;top:0;left:0;width:100%;height:100%;pointer-events:none;z-index:0;';
        container.insertBefore(this.canvas, container.firstChild);
        this.resize();
        this.init();
    }

    resize() {
        const rect = this.container.getBoundingClientRect();
        this.canvas.width = rect.width * (window.devicePixelRatio || 1);
        this.canvas.height = rect.height * (window.devicePixelRatio || 1);
        this.canvas.style.width = rect.width + 'px';
        this.canvas.style.height = rect.height + 'px';
        this.ctx.scale(window.devicePixelRatio || 1, window.devicePixelRatio || 1);
        this.w = rect.width;
        this.h = rect.height;
    }

    init() {
        this.particles = [];
        for (let i = 0; i < this.options.count; i++) {
            this.particles.push({
                x: Math.random() * this.w,
                y: Math.random() * this.h,
                vx: (Math.random() - 0.5) * this.options.speed,
                vy: (Math.random() - 0.5) * this.options.speed,
                r: Math.random() * this.options.radius + 1
            });
        }
    }

    animate() {
        const { ctx, w, h, particles, options } = this;
        ctx.clearRect(0, 0, w, h);

        for (let i = 0; i < particles.length; i++) {
            const p = particles[i];
            p.x += p.vx;
            p.y += p.vy;
            if (p.x < 0 || p.x > w) p.vx *= -1;
            if (p.y < 0 || p.y > h) p.vy *= -1;

            ctx.beginPath();
            ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
            ctx.fillStyle = options.color;
            ctx.fill();

            // Connect nearby particles
            for (let j = i + 1; j < particles.length; j++) {
                const q = particles[j];
                const dx = p.x - q.x;
                const dy = p.y - q.y;
                const dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < options.connectDistance) {
                    ctx.beginPath();
                    ctx.moveTo(p.x, p.y);
                    ctx.lineTo(q.x, q.y);
                    ctx.strokeStyle = options.lineColor;
                    ctx.lineWidth = 0.5;
                    ctx.stroke();
                }
            }
        }
        this.animId = requestAnimationFrame(() => this.animate());
    }

    start() { this.animate(); return this; }
    stop() { cancelAnimationFrame(this.animId); return this; }
    destroy() { this.stop(); this.canvas.remove(); }
}

// ========== 环境光斑背景（液态玻璃氛围）==========
class AmbientBackground {
    static init(container = document.body) {
        if (container.querySelector('.ambient-bg')) return;
        const bg = document.createElement('div');
        bg.className = 'ambient-bg';
        bg.setAttribute('aria-hidden', 'true');
        bg.innerHTML = [
            '<div class="ambient-blob blob-1"></div>',
            '<div class="ambient-blob blob-2"></div>',
            '<div class="ambient-blob blob-3"></div>'
        ].join('');
        const firstChild = container.firstElementChild;
        if (firstChild) container.insertBefore(bg, firstChild);
        else container.appendChild(bg);
    }
}

// ========== 3D 卡片倾斜 ==========
class TiltEffect {
    static init(selector = '.item-card') {
        document.querySelectorAll(selector).forEach(card => {
            card.addEventListener('mousemove', TiltEffect._handleMove);
            card.addEventListener('mouseleave', TiltEffect._handleLeave);
            card.style.transformStyle = 'preserve-3d';
            card.style.transition = 'transform 0.1s ease, box-shadow 0.3s ease';
        });
    }

    static _handleMove(e) {
        const card = e.currentTarget;
        const rect = card.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        const centerX = rect.width / 2;
        const centerY = rect.height / 2;
        const rotateX = ((y - centerY) / centerY) * -8;
        const rotateY = ((x - centerX) / centerX) * 8;
        card.style.transform = 'perspective(800px) rotateX(' + rotateX + 'deg) rotateY(' + rotateY + 'deg) scale3d(1.02,1.02,1.02)';
        card.style.boxShadow = (rotateY > 0 ? '6px' : '-6px') + ' 8px 24px rgba(255,122,69,0.15), 0 2px 8px rgba(0,0,0,0.06)';
    }

    static _handleLeave(e) {
        const card = e.currentTarget;
        card.style.transform = 'perspective(800px) rotateX(0) rotateY(0) scale3d(1,1,1)';
        card.style.boxShadow = '0 2px 12px rgba(0,0,0,0.08)';
    }
}

// ========== 滚动渐入 ==========
class ScrollReveal {
    static init(selector = '.reveal', options = {}) {
        const opts = Object.assign({ threshold: 0.15, stagger: 80, rootMargin: '0px 0px -40px 0px' }, options);
        const elements = document.querySelectorAll(selector);

        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry, idx) => {
                if (entry.isIntersecting) {
                    const el = entry.target;
                    const delay = Array.from(elements).indexOf(el) * opts.stagger;
                    el.style.animationDelay = delay + 'ms';
                    el.classList.add('revealed');
                    observer.unobserve(el);
                }
            });
        }, { threshold: opts.threshold, rootMargin: opts.rootMargin });

        elements.forEach(el => {
            el.classList.add('reveal-hidden');
            observer.observe(el);
        });

        return observer;
    }

    static refresh(selector = '.reveal-hidden') {
        document.querySelectorAll(selector).forEach(el => {
            el.classList.remove('revealed', 'reveal-hidden');
        });
    }
}

// ========== 波纹涟漪 ==========
function rippleEffect(e) {
    const btn = e.currentTarget;
    const rect = btn.getBoundingClientRect();
    const size = Math.max(rect.width, rect.height) * 2;
    const x = e.clientX - rect.left - size / 2;
    const y = e.clientY - rect.top - size / 2;

    const ripple = document.createElement('span');
    ripple.className = 'ripple';
    ripple.style.cssText = 'left:' + x + 'px;top:' + y + 'px;width:' + size + 'px;height:' + size + 'px;';
    btn.appendChild(ripple);
    ripple.addEventListener('animationend', () => ripple.remove());
}

// ========== 数字跳动 ==========
function countUp(el, target, duration) {
    duration = duration || 800;
    const start = performance.now();
    const from = parseFloat(el.textContent.replace(/[^0-9.]/g, '')) || 0;
    const isPrice = el.textContent.startsWith('¥');
    const isInt = !String(target).includes('.');

    function update(now) {
        const elapsed = now - start;
        const progress = Math.min(elapsed / duration, 1);
        // ease-out
        const eased = 1 - Math.pow(1 - progress, 3);
        const current = from + (target - from) * eased;
        if (isPrice) {
            el.textContent = '¥' + current.toFixed(2);
        } else if (isInt) {
            el.textContent = Math.floor(current);
        } else {
            el.textContent = current.toFixed(2);
        }
        if (progress < 1) {
            requestAnimationFrame(update);
        }
    }
    requestAnimationFrame(update);
}

// ========== 成功撒花 ==========
function celebrate() {
    const colors = ['#FF7A45','#FFA502','#2ED573','#1E90FF','#FF4757','#FF9A70','#7BED9F','#70A1FF'];
    for (let i = 0; i < 40; i++) {
        setTimeout(() => {
            const confetti = document.createElement('div');
            confetti.className = 'confetti-piece';
            confetti.style.cssText = [
                'left:' + Math.random() * 100 + '%',
                'background:' + colors[Math.floor(Math.random() * colors.length)],
                'animation-delay:' + Math.random() * 0.3 + 's',
                'animation-duration:' + (0.6 + Math.random() * 0.8) + 's',
                '--x:' + (Math.random() * 200 - 100) + 'px'
            ].join(';');
            document.body.appendChild(confetti);
            confetti.addEventListener('animationend', () => confetti.remove());
        }, i * 30);
    }
}

// ========== 页面入场动画 ==========
function pageEnter() {
    const main = document.querySelector('.page, .page-no-tabbar');
    if (!main) return;
    main.style.animation = 'fadeSlideIn 0.35s cubic-bezier(0.16, 1, 0.3, 1)';
    main.addEventListener('animationend', () => {
        main.style.animation = '';
    }, { once: true });
}

// ========== 初始化全局监听 ==========
document.addEventListener('DOMContentLoaded', () => {
    // 自动初始化环境光斑（若页面未手动写入）
    AmbientBackground.init();
    // ========== 全局按钮波纹 ==========
    document.addEventListener('click', (e) => {
        const btn = e.target.closest('.btn-primary, .btn-block, .login-btn, .buy-btn');
        if (!btn) return;
        const rect = btn.getBoundingClientRect();
        const size = Math.max(rect.width, rect.height) * 2;
        const x = e.clientX - rect.left - size / 2;
        const y = e.clientY - rect.top - size / 2;
        const ripple = document.createElement('span');
        ripple.className = 'ripple';
        ripple.style.cssText = 'left:' + x + 'px;top:' + y + 'px;width:' + size + 'px;height:' + size + 'px;';
        btn.appendChild(ripple);
        ripple.addEventListener('animationend', () => ripple.remove());
    });
});

// ========== 全局动画样式注入 ==========
(function injectAnimStyles() {
    const style = document.createElement('style');
    style.textContent = [
        '.particle-canvas { pointer-events: none; }',
        '.reveal-hidden { opacity: 0; transform: translateY(24px); }',
        '.revealed { animation: revealIn 0.5s cubic-bezier(0.16,1,0.3,1) forwards; }',
        '@keyframes revealIn { to { opacity:1; transform:translateY(0); } }',
        '.ripple { position:absolute; border-radius:50%; background:rgba(255,255,255,0.4); transform:scale(0); animation: rippleAnim 0.6s ease-out; pointer-events:none; }',
        '@keyframes rippleAnim { to { transform:scale(4); opacity:0; } }',
        '.confetti-piece { position:fixed; top:-10px; width:8px; height:8px; border-radius:2px; z-index:9999; animation: confettiFall linear forwards; pointer-events:none; }',
        '@keyframes confettiFall { 0% { transform:translateY(0) translateX(0) rotate(0deg); opacity:1; } 100% { transform:translateY(105vh) translateX(var(--x)) rotate(720deg); opacity:0; } }',
        '.float-anim { animation: floatUpDown 3s ease-in-out infinite; }',
        '@keyframes floatUpDown { 0%,100% { transform:translateY(0); } 50% { transform:translateY(-6px); } }',
        '.pulse-anim { animation: pulseGlow 2s ease-in-out infinite; }',
        '@keyframes pulseGlow { 0%,100% { box-shadow:0 0 0 0 rgba(255,122,69,0.4); } 50% { box-shadow:0 0 0 10px rgba(255,122,69,0); } }',
        '.glow-hover { transition: box-shadow 0.3s; }',
        '.glow-hover:hover { box-shadow: 0 0 20px rgba(255,122,69,0.3), 0 0 40px rgba(255,122,69,0.1); }',
        '.shimmer-text { background:linear-gradient(90deg,currentColor 40%,#fff 50%,currentColor 60%); background-size:200% 100%; -webkit-background-clip:text; -webkit-text-fill-color:transparent; animation: shimmerText 2s infinite; }',
        '@keyframes shimmerText { to { background-position:-200% 0; } }',
        '.scale-in { animation: scaleIn 0.3s cubic-bezier(0.16,1,0.3,1); }',
        '@keyframes scaleIn { from { transform:scale(0.9); opacity:0; } to { transform:scale(1); opacity:1; } }'
    ].join('\n');
    document.head.appendChild(style);
})();

// Export for global use
window.ParticleSystem = ParticleSystem;
window.AmbientBackground = AmbientBackground;
window.TiltEffect = TiltEffect;
window.ScrollReveal = ScrollReveal;
window.rippleEffect = rippleEffect;
window.countUp = countUp;
window.celebrate = celebrate;
window.pageEnter = pageEnter;
