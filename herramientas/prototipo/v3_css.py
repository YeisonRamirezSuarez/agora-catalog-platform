# CSS del catálogo público rediseñado al boceto del cliente (noche, crema, Playfair con cursiva dorada) y piezas nuevas del panel.
CAT_CSS = r'''/* ---------- Catálogo público ---------- */
html:has(.top){scroll-padding-top:76px}
html{touch-action:manipulation}
/* !important a propósito: una sola regla cubre los 42 campos del catálogo y del panel, cada uno con su propio tamaño */
@media (pointer:coarse){input:not([type=checkbox],[type=radio],[type=range]),select,textarea{font-size:16px!important}}
/* Con el dedo, zona táctil de unos 44 px sin cambiar cómo se ven: un ::after invisible más grande que el botón. */
@media (pointer:coarse){
.qty button,.crumbs a,.see-all,.foot-nav a{position:relative}
.heart::after,.qty button::after,.crumbs a::after,.see-all::after,.foot-nav a::after{content:"";position:absolute;inset:-4px}
.qty button::after{inset:-3px -5px}
.crumbs a::after,.see-all::after{inset:-13px -6px}
.foot-nav a::after{inset:-5px 0}
}
.store{overflow-x:clip;--grain:url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='180' height='180'%3E%3Cfilter id='g'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.8' numOctaves='3' stitchTiles='stitch'/%3E%3CfeColorMatrix values='0 0 0 0 .45 0 0 0 0 .32 0 0 0 0 .18 0 0 0 .16 0'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23g)'/%3E%3C/svg%3E");min-height:100vh;background:var(--cream);color:var(--c-ink);caret-color:var(--brand)}
.store ::selection,.cat-sheet ::selection{background:color-mix(in srgb,var(--accent) 45%,transparent)}
.store :focus-visible,.cat-sheet :focus-visible{outline-color:var(--brand)}
.top :focus-visible,.hero :focus-visible,.sellos :focus-visible,.band :focus-visible,.cat-hero :focus-visible,.foot :focus-visible,.help-card :focus-visible{outline-color:var(--accent)}
.btn-brand{border-color:var(--brand);background:var(--brand);color:#fff}
.btn-brand:hover{background:var(--brand);filter:brightness(1.18)}
.eyebrow{margin-bottom:14px;color:var(--accent);font-size:11.5px;font-weight:600;letter-spacing:.3em;text-transform:uppercase}

/* Encabezado */
.top{position:fixed;inset:0 0 auto;z-index:40;color:#fff;transition:background-color .3s,box-shadow .3s}
.top.solid{background:rgba(21,18,13,.94);-webkit-backdrop-filter:blur(10px);backdrop-filter:blur(10px);box-shadow:0 1px 0 rgba(255,255,255,.07)}
.top-in{display:flex;align-items:center;gap:10px;max-width:1240px;height:68px;margin:0 auto;padding:0 12px}
.brandlink{display:flex;flex:1;align-items:center;justify-content:center;gap:9px;min-width:0;color:#fff;text-decoration:none}
.brandlink .logo{width:44px;height:44px;border-radius:50%;font-size:15px}
.brandlink .logo.has-img{padding:3px;background:#fff} /* el emblema entra completo en el círculo */
.bn{display:grid;min-width:0;line-height:1}
.bn small{overflow:hidden;font:600 12.5px/1.15 var(--serif);white-space:nowrap;text-overflow:ellipsis}
.bn b{overflow:hidden;margin-bottom:-.16em;padding-bottom:.16em;font:700 23px/1 var(--serif);letter-spacing:-.01em;white-space:nowrap;text-overflow:ellipsis}
.bn i{margin-top:2px;overflow:hidden;color:#D8CEBF;font:600 10px/1.35 var(--font);font-style:normal;letter-spacing:.1em;text-transform:uppercase;white-space:nowrap}
.icon-btn{display:grid;place-items:center;flex:none;width:42px;height:42px;padding:0;border:0;border-radius:50%;background:rgba(255,255,255,.1);color:#fff;transition:background-color .15s}
.icon-btn:hover{background:rgba(255,255,255,.2)}
.find-btn{display:none}
.nav{display:none}
.cart-btn{display:flex;flex:none;align-items:center;gap:10px;padding:0;border:0;background:none;color:#fff;text-align:left}
.cart-ic{position:relative;display:grid;place-items:center;width:42px;height:42px;border-radius:50%;background:rgba(255,255,255,.1)}
.cart-btn:hover .cart-ic{background:rgba(255,255,255,.2)}
.cart-n{position:absolute;top:-3px;right:-5px;min-width:20px;height:20px;padding:0 5px;border-radius:10px;background:var(--price);color:#fff;font-size:12px;font-weight:700;line-height:20px;text-align:center;box-shadow:0 0 0 2px var(--night)}
.cart-txt{display:none;line-height:1.15}
.cart-txt small{display:block;color:#D8CEBF;font-size:13px}
.cart-txt b{font-size:17px;font-weight:700;font-variant-numeric:tabular-nums}
.bump{animation:bump .2s var(--ease)}
@keyframes bump{25%{transform:scale(1.22)}}

/* Portada */
.hero{position:relative;isolation:isolate;display:flex;flex-direction:column;justify-content:flex-end;padding:104px 20px 34px;overflow:hidden;background:var(--night);color:#fff}
.hero > img{position:absolute;inset:0;z-index:-2;width:100%;height:100%;object-fit:cover;object-position:16% 45%;animation:settle .65s var(--ease) both}
.hero::before{content:"";position:absolute;inset:0;z-index:-1;background:linear-gradient(180deg,rgba(21,18,13,.8) 0%,rgba(21,18,13,.62) 24%,rgba(21,18,13,.88) 50%,var(--night) 92%)}
.hero h1{font:600 clamp(40px,11.4vw,50px)/1.04 var(--serif);letter-spacing:-.015em;animation:lift .5s var(--ease) both}
.hero h1 em{display:block;color:var(--accent);font-style:italic;animation:lift .5s .1s var(--ease) both}
.hero-sub{max-width:34ch;margin-top:14px;color:#ECE4D6;font-size:16px;line-height:1.5}
.hero-find{display:flex;align-items:center;height:50px;margin-top:22px;overflow:hidden;border-radius:10px;background:var(--paper);color:var(--c-ink)}
.hero-find:focus-within{box-shadow:0 0 0 3px var(--accent)}
.hero-find input{flex:1;min-width:0;height:100%;padding:0 4px 0 16px;border:0;background:none;outline:none}
.hero-find input::placeholder{color:var(--c-muted)}
.hero-find button{display:grid;place-items:center;flex:none;width:48px;height:100%;border:0;background:none;color:var(--c-ink-2)}
.hero-cta{display:inline-flex;align-items:center;gap:12px;min-height:52px;padding:0 24px;border-radius:8px;background:var(--accent);color:var(--on-accent);font-weight:700;text-decoration:none;transition:filter .15s}
.hero-cta:hover{filter:brightness(1.07)}
.hero-cta svg{transition:transform .25s var(--ease)}
.hero-cta:hover svg{transform:translateX(4px)}
.hero .hero-cta,.hero-note,.stamp,.sellos{display:none}
@keyframes settle{from{transform:scale(1.08)}}
@keyframes lift{from{transform:translateY(16px)}}

/* Mostrador crema */
.counter{position:relative;background:var(--cream) var(--grain)}
.wave{position:absolute;left:0;right:0;top:-26px;display:block;width:100%;height:27px;fill:var(--cream);pointer-events:none}
.circles{display:flex;gap:14px;padding:22px 16px 4px;overflow-x:auto;scrollbar-width:none}
.circles::-webkit-scrollbar,.plan-rail::-webkit-scrollbar,.prow::-webkit-scrollbar,.chips::-webkit-scrollbar{display:none}
.circle{display:grid;flex:none;justify-items:center;gap:7px;width:72px;color:var(--c-ink);font-size:12.5px;font-weight:600;line-height:1.2;text-align:center;text-decoration:none}
.circle .ph{width:64px;height:64px;border-radius:50%;box-shadow:0 2px 8px -2px rgba(33,28,22,.35)}
.planes{position:relative;padding:30px 0 8px;background:radial-gradient(70% 60% at 50% 0%,rgba(255,252,245,.75),rgba(255,252,245,0) 70%)}
.side-note,.deco{display:none}
.sec-title{padding:0 16px;font:700 30px/1.1 var(--serif);letter-spacing:-.01em;text-align:center}
.sec-sub{margin-top:8px;padding:0 16px;color:var(--c-ink-2);text-align:center}
.plan-rail{display:flex;gap:12px;padding:20px 16px 8px;overflow-x:auto;scroll-snap-type:x mandatory;scroll-padding-inline:16px;scrollbar-width:none}
.plan{display:grid;flex:none;grid-template-columns:42% minmax(0,1fr);width:min(310px,82vw);min-height:150px;overflow:hidden;border-radius:16px;background:var(--sand);color:inherit;text-decoration:none;background-image:linear-gradient(135deg,rgba(255,255,255,.45),rgba(255,255,255,0) 60%);box-shadow:0 0 0 1px rgba(33,28,22,.04),0 14px 26px -20px rgba(70,45,20,.5);scroll-snap-align:start;transition:transform .3s var(--ease),box-shadow .3s}
.plan > img,.plan > .ph-none{width:100%;height:100%;object-fit:cover}
.plan-txt{position:relative;display:grid;align-content:start;gap:6px;padding:16px 14px 56px}
.plan-txt b{font:700 20px/1.12 var(--serif);letter-spacing:-.01em}
.plan-desc{color:var(--c-ink-2);font-size:13px;line-height:1.4}
.plan-go{position:absolute;right:12px;bottom:12px;display:grid;place-items:center;width:36px;height:36px;border-radius:50%;background:var(--accent);color:var(--on-accent);transition:transform .3s var(--ease)}
.plan-go svg{width:18px;height:18px}
.plan:hover{box-shadow:0 0 0 1px rgba(33,28,22,.04),0 22px 36px -20px rgba(70,45,20,.6);transform:translateY(-3px)}
.plan:hover .plan-go{transform:translateX(4px)}
.favs{padding:30px 0 28px}
.favs-head{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:0 16px}
.script{font:700 24px/1.1 var(--serif);letter-spacing:-.01em} /* en el celular el boceto usa la serif */
.favs-act{display:flex;align-items:center;gap:8px}
.see-all{display:inline-flex;align-items:center;gap:6px;color:var(--c-ink);font-size:14px;font-weight:600;text-decoration:none}
.see-all:hover{text-decoration:underline;text-underline-offset:3px}
.see-all svg{width:17px;height:17px}
.chips,.rail-btn{display:none}
.prow{display:flex;gap:12px;padding:16px 16px 8px;overflow-x:auto;scroll-snap-type:x mandatory;scroll-padding-inline:16px;scrollbar-width:none}
.prow .pc{flex:none;width:calc((100vw - 44px)/2);scroll-snap-align:start}

/* Tarjeta de producto */
.pc{position:relative;display:flex;flex-direction:column;min-width:0;overflow:hidden;border-radius:14px;background:var(--paper);box-shadow:0 0 0 1px rgba(33,28,22,.05),0 1px 2px rgba(33,28,22,.06),0 14px 28px -18px rgba(70,45,20,.45);transition:transform .25s var(--ease),box-shadow .25s var(--ease)}
@media(hover:hover){.pc:hover{box-shadow:0 0 0 1px rgba(33,28,22,.05),0 2px 4px rgba(33,28,22,.06),0 26px 40px -22px rgba(70,45,20,.55);transform:translateY(-4px)}}
.pc-hit{display:flex;flex:1;flex-direction:column;padding:0;border:0;background:none;color:inherit;text-align:left}
.pc .ph{width:100%;aspect-ratio:4/3;background:var(--sand)}
.pc .ph img{transition:transform .6s var(--ease)}
.pc-hit:hover .ph img{transform:scale(1.04)}
.pc-badge{position:absolute;top:10px;left:10px;z-index:1;pointer-events:none}
.heart{position:absolute;top:8px;right:8px;z-index:1;display:grid;place-items:center;width:36px;height:36px;padding:0;border:0;border-radius:50%;background:rgba(255,253,248,.9);color:var(--c-ink);box-shadow:0 3px 10px -3px rgba(33,28,22,.45)}
.heart svg{width:20px;height:20px;stroke-width:1.7;transition:transform .2s var(--ease)}
.heart:active svg{transform:scale(.85)}
.heart[aria-pressed=true]{color:var(--price)}
.heart[aria-pressed=true] svg{fill:currentColor}
.pc-body{display:grid;flex:1;grid-template-rows:auto auto 1fr;gap:4px;padding:12px 12px 0}
.pc-name{display:-webkit-box;overflow:hidden;font-size:15px;font-weight:700;line-height:1.25;-webkit-box-orient:vertical;-webkit-line-clamp:3} /* el nombre completo va en la ficha */
.pc-desc{display:-webkit-box;overflow:hidden;color:var(--c-muted);font-size:13px;line-height:1.4;-webkit-line-clamp:2;-webkit-box-orient:vertical}
.pc-foot{display:grid;justify-items:start;align-self:end;gap:6px;padding-top:8px}
.price{display:flex;flex-wrap:wrap;align-items:baseline;gap:0 7px;color:var(--price);font-size:18px;font-weight:800;letter-spacing:-.01em}
.price s{color:var(--c-muted);font-size:13px;font-weight:500}
.price small{color:var(--c-muted);font-size:12.5px;font-weight:500}
.price.ask{color:var(--c-ink-2);font-size:14px;font-weight:600}
.pres{max-width:100%;padding:4px 9px;overflow:hidden;border-radius:7px;background:#EFE9DF;color:var(--c-ink-2);font-size:12px;white-space:nowrap;text-overflow:ellipsis}
.prow .pc-desc,.prow .pres{display:none} /* tarjeta corta del boceto en el celular */
.buy{display:flex;align-items:center;gap:8px;padding:12px}
.qty{display:flex;flex:1;align-items:center;min-width:0;height:40px;border:1px solid var(--c-line);border-radius:10px;background:#fff}
.qty button{display:grid;place-items:center;flex:none;width:34px;height:100%;padding:0;border:0;background:none;color:var(--c-ink)}
.qty button svg{width:16px;height:16px}
.qty button:disabled,.qty button[aria-disabled=true]{color:#BFB5A7;cursor:default}
.qty output{flex:1;font-weight:700;text-align:center;font-variant-numeric:tabular-nums}
.add{display:grid;place-items:center;flex:none;width:46px;height:40px;padding:0;border:0;border-radius:10px;background:var(--brand);color:#fff;transition:filter .15s,transform .15s}
.add:hover{filter:brightness(1.2)}
.add:active{transform:scale(.94)}
.add svg{width:21px;height:21px}
.ask-wa{flex:1;min-height:40px;border-color:var(--c-line);color:var(--c-ink)}
.soon{color:var(--c-muted);font-size:13px}
.pc.out .ph img{filter:grayscale(1);opacity:.6}
.tagb{display:inline-flex;align-items:center;height:25px;padding:0 10px;border-radius:999px;font-size:12px;font-weight:700;white-space:nowrap}
.tagb.best{background:#ECB85F;color:#3A2706}
.tagb.promo{background:#F6DF5E;color:#3A3000}
.tagb.new{background:#C9EBDC;color:#0F3D2A}
.tagb.out{background:var(--c-ink);color:#fff}

/* Franja y contacto */
.band{position:relative;isolation:isolate;display:flex;align-items:flex-end;min-height:440px;padding:64px 20px 34px;overflow:hidden;background:var(--night);color:#fff}
.band > img{position:absolute;inset:0;z-index:-2;width:100%;height:100%;object-fit:cover}
.band::before{content:"";position:absolute;inset:0;z-index:-1;background:linear-gradient(180deg,rgba(21,18,13,.05) 22%,rgba(21,18,13,.88) 76%)}
.band .wave{top:-1px;transform:scaleY(-1)}
.band-tag{position:absolute;top:58px;right:18px;padding:10px 16px 12px;border-radius:3px;background:#F3E9D6;color:var(--c-ink);font:400 26px/1.05 var(--script);box-shadow:0 10px 22px -10px rgba(0,0,0,.6);transform:rotate(7deg)}
.band-in{display:grid;justify-items:start;gap:12px;width:100%;max-width:1240px;margin:0 auto}
.band h2{font:600 44px/1.02 var(--serif);letter-spacing:-.015em}
.band-in p{max-width:36ch;color:#ECE4D6}
.visit{position:relative;padding:34px 16px 52px;background:radial-gradient(60% 55% at 50% 0%,rgba(255,252,245,.8),rgba(255,252,245,0) 70%),var(--cream) var(--grain);scroll-margin-top:64px}
.v-in{display:grid;justify-items:center;max-width:1040px;margin:0 auto;text-align:center}
.v-shop{width:230px;height:auto;color:color-mix(in srgb,var(--accent) 52%,#3A2706)}
.visit-note{margin-top:14px;color:color-mix(in srgb,var(--accent) 58%,#3A2706);font:400 30px/1.05 var(--script);transform:rotate(-4deg)}
.v-in h2{margin-top:6px;font:600 40px/1.04 var(--serif);letter-spacing:-.015em}
.v-in h2 em{color:color-mix(in srgb,var(--accent) 58%,#3A2706);font-style:italic}
.v-sub{max-width:40ch;margin-top:12px;text-wrap:balance;color:var(--c-ink-2);font-size:16px;line-height:1.5}
/* Los números de WhatsApp van siempre lado a lado (dos en un celular de 360 px); se achican con la pantalla. */
.v-was{display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:8px;width:100%;max-width:640px;margin-top:24px}
.v-wa{display:flex;align-items:center;gap:clamp(7px,2vw,14px);min-width:0;min-height:58px;padding:8px clamp(8px,2.4vw,20px) 8px 8px;border-radius:14px;background:var(--brand);color:#fff;text-align:left;text-decoration:none;box-shadow:0 16px 26px -20px rgba(20,30,15,.9);transition:transform .2s var(--ease),filter .15s}
.v-wa:hover{filter:brightness(1.15);transform:translateY(-2px)}
.v-wa-ic{display:grid;place-items:center;flex:none;width:clamp(26px,7vw,44px);height:clamp(26px,7vw,44px);border-radius:50%;background:rgba(255,255,255,.14)}
.v-wa-ic svg{width:clamp(16px,4.4vw,22px);height:clamp(16px,4.4vw,22px)}
.v-wa>span:last-child{min-width:0}
.v-wa small{display:block;color:rgba(255,255,255,.78);font-size:clamp(11px,3vw,12.5px)}
.v-wa b{display:block;overflow:hidden;font-size:clamp(11px,3.1vw,18px);font-weight:700;font-variant-numeric:tabular-nums;letter-spacing:.01em;white-space:nowrap;text-overflow:ellipsis}
.v-info{display:grid;width:100%;max-width:900px;margin:30px 0 0}
.v-item{display:grid;align-content:start;gap:6px;padding:18px 8px;border-top:1px solid var(--c-line)}
.v-item:first-child{border-top:0}
.v-item dt{color:color-mix(in srgb,var(--accent) 52%,#3A2706);font:italic 600 19px/1.2 var(--serif)}
.v-item dd span{text-wrap:balance}
.v-item dd{display:grid;justify-items:center;gap:8px;margin:0;text-wrap:pretty;color:var(--c-ink-2);font-size:15px;line-height:1.45}
.v-estado{display:inline-flex;align-items:center;gap:7px;color:var(--c-ink);font-size:14px;font-weight:700}
.v-estado::before{content:"";width:8px;height:8px;border-radius:50%;background:var(--c-muted)}
.v-estado.on::before{background:var(--ok);box-shadow:0 0 0 3px var(--ok-soft)}
.v-link{display:inline-flex;align-items:center;gap:6px;color:var(--c-ink);font-size:14px;font-weight:700;text-decoration:underline;text-decoration-color:color-mix(in srgb,var(--accent) 70%,transparent);text-decoration-thickness:2px;text-underline-offset:4px}
.v-link svg{width:15px;height:15px;transition:transform .2s var(--ease)}
.v-link:hover svg{transform:translateX(3px)}
.foot{padding:44px 20px 30px;background:var(--night) var(--grain);color:#B9AE9E;font-size:14px}
.foot-in{display:grid;gap:30px;max-width:1240px;margin:0 auto}
.foot .brandlink{flex:none;justify-content:flex-start}
.foot-brand p{max-width:36ch;margin-top:14px;color:#B9AE9E;line-height:1.5}
.foot h3{margin-bottom:12px;color:#fff;font:600 19px/1.2 var(--serif)}
.foot-nav{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:2px 16px}
.foot-nav h3{grid-column:1/-1}
.foot-nav a{padding:6px 0;color:#D8CEBF;text-decoration:none}
.foot-nav a:hover{color:#fff;text-decoration:underline;text-underline-offset:3px}
.foot-share p{max-width:32ch;line-height:1.5}
.foot-btn{margin-top:14px;border-color:rgba(243,235,221,.3);background:none;color:#F7F1E6}
.foot-btn:hover{border-color:var(--accent);background:rgba(255,255,255,.04)}
.foot-legal{display:grid;gap:12px;max-width:1240px;margin:34px auto 0;padding-top:22px;border-top:1px solid rgba(243,235,221,.12);font-size:13px}
.foot-made{display:inline-flex;align-items:center;gap:10px;color:#8F8577}
.foot .ec{height:28px;width:auto}
.foot .proto-note{max-width:1240px;margin:14px auto 0;color:#8F8577;font-size:12.5px}
.foot button:not(.btn){padding:0;border:0;background:none;color:#F1E9DC;font-weight:600;text-decoration:underline;text-underline-offset:3px}

/* Página de catálogo */
.cat-hero{position:relative;isolation:isolate;padding:108px 20px 74px;overflow:hidden;background:var(--night);color:#fff}
.cat-hero > img{position:absolute;inset:0;z-index:-2;width:100%;height:100%;object-fit:cover;opacity:.6}
.cat-hero::before{content:"";position:absolute;inset:0;z-index:-1;background:linear-gradient(90deg,var(--night) 8%,rgba(21,18,13,.4))}
.cat-hero-in{max-width:1240px;margin:0 auto}
.cat-hero h1{font:600 40px/1.06 var(--serif);letter-spacing:-.015em}
.cat-hero h1 em{color:var(--accent);font-style:italic}
.cat-hero p:not(.eyebrow){max-width:40ch;margin-top:12px;color:#ECE4D6}
.cat-wrap{position:relative;max-width:1240px;margin:-32px auto 0;padding:18px 16px 44px;border-radius:24px 24px 0 0;background:var(--cream)}
.crumbs{display:flex;align-items:center;gap:8px;color:var(--c-muted);font-size:13px}
.crumbs a{display:inline-flex;align-items:center;gap:4px;color:var(--c-muted);text-decoration:none}
.crumbs a:hover{color:var(--c-ink);text-decoration:underline}
.crumbs svg{width:14px;height:14px}
.cat-tools{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:8px;margin:14px 0 6px}
.c-search{display:flex;grid-column:1/-1;align-items:center;gap:8px;height:48px;padding-left:14px;overflow:hidden;border:1px solid var(--c-line);border-radius:12px;background:#fff;color:var(--c-muted)}
.c-search:focus-within{border-color:var(--brand);box-shadow:0 0 0 3px color-mix(in srgb,var(--brand) 18%,transparent)}
.c-search input{flex:1;min-width:0;height:100%;border:0;background:none;outline:none;color:var(--c-ink)}
.c-search input::placeholder{color:var(--c-muted)}
.c-search button{display:grid;place-items:center;flex:none;width:52px;height:100%;border:0;background:var(--brand);color:#fff}
.c-select{height:44px;padding:0 34px 0 12px;border:1px solid var(--c-line);border-radius:12px;background:#fff url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='8' fill='none' stroke='%236F655A' stroke-width='2'%3E%3Cpath d='m1 1 5 5 5-5'/%3E%3C/svg%3E") no-repeat right 12px center;font-weight:600;appearance:none}
.filters-btn{min-height:44px;border-color:var(--c-line)}
.view-tog,.filters{display:none}
.res-head{display:flex;flex-wrap:wrap;align-items:center;gap:8px 12px;margin:10px 0 12px}
#rcount{color:var(--c-muted);font-size:14px}
.active-f{display:flex;flex-wrap:wrap;gap:6px}
.fchip{display:inline-flex;align-items:center;gap:6px;height:30px;padding:0 8px 0 12px;border:1px solid var(--c-line);border-radius:999px;background:var(--paper);color:var(--c-ink);font-size:13px;font-weight:600}
.fchip:hover{border-color:var(--c-ink)}
.fchip svg{width:14px;height:14px}
.grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}
.grid .no-hits{grid-column:1/-1}
.no-hits .btn{margin-top:6px}
.as-list{grid-template-columns:minmax(0,1fr)}
.as-list .pc{flex-direction:row;flex-wrap:wrap}
.as-list .pc-hit{flex:1 1 240px;flex-direction:row}
.as-list .pc .ph{flex:none;width:112px;height:auto;aspect-ratio:1}
.as-list .pc-body{padding-bottom:12px}
.as-list .buy{flex:1 1 100%;padding-top:0}
.as-list .heart{left:66px;right:auto}
.f-group{display:grid;gap:2px}
.f-group h3{margin-bottom:6px;font-size:15px;font-weight:700}
.f-check{display:flex;align-items:center;gap:10px;min-height:34px;font-size:14.5px;cursor:pointer}
.f-check input{flex:none;width:18px;height:18px;margin:0;accent-color:var(--brand)}
.f-check small{color:var(--c-muted);font-size:13px}
.range{position:relative;height:30px;margin-top:2px}
.range::before{content:"";position:absolute;left:0;right:0;top:50%;height:3px;border-radius:2px;background:var(--c-ink);transform:translateY(-50%)}
.range input{position:absolute;inset:0;width:100%;height:100%;margin:0;background:none;pointer-events:none;-webkit-appearance:none;appearance:none}
.range input::-webkit-slider-thumb{width:18px;height:18px;border:0;border-radius:50%;background:var(--c-ink);pointer-events:auto;cursor:pointer;-webkit-appearance:none}
.range input::-moz-range-thumb{width:18px;height:18px;border:0;border-radius:50%;background:var(--c-ink);pointer-events:auto;cursor:pointer}
.range-txt{color:var(--c-muted);font-size:14px;font-variant-numeric:tabular-nums}
.help-card{display:grid;gap:8px;padding:22px 20px;border-radius:16px;background:var(--brand);color:#fff}
.help-card h3{font:600 21px/1.15 var(--serif)}
.help-card p{color:rgba(255,255,255,.84);font-size:14px}
.help-card .btn{justify-self:start;margin-top:6px;border-color:#fff;background:#fff;color:var(--c-ink)}
.fsheet{display:grid;gap:22px;padding:56px 20px 20px}
.fsheet h2{font:700 28px/1.1 var(--serif)}
.f-body{display:grid;gap:22px}

/* Hojas del catálogo */
.cat-sheet{background:var(--paper);color:var(--c-ink)}
.cat-sheet .sheet-ph{background:var(--sand)}
.cat-sheet .pc-badge{top:16px;left:16px;z-index:2}
.cat-sheet .sheet-cta .btn{border-radius:14px}
.ps-body{display:grid;gap:6px;padding:18px 20px 10px}
.ps-body h2{font:700 28px/1.1 var(--serif);letter-spacing:-.01em}
.ps-vars{display:flex;flex-wrap:wrap;gap:8px;margin:4px 0}
.ps-vars label{position:relative;display:grid;gap:1px;min-height:48px;align-content:center;padding:6px 14px;border:1px solid var(--c-line);border-radius:12px;background:var(--paper);cursor:pointer}
.ps-vars b{font-size:14px;font-weight:700}
.ps-vars small{color:var(--c-muted);font-size:13px;font-variant-numeric:tabular-nums}
.ps-vars input{position:absolute;opacity:0;pointer-events:none}
.ps-vars label:has(input:checked){border-color:var(--c-ink);box-shadow:inset 0 0 0 1px var(--c-ink)}
.ps-vars label:has(input:focus-visible){outline:2px solid var(--c-ink);outline-offset:2px}
.price.big{margin-top:6px;font-size:26px}
.price.big s{font-size:16px}
.ps-desc{margin-top:6px;color:var(--c-ink-2);line-height:1.55}
/* Compartir el producto: un link que en WhatsApp sale con su foto, nombre y precio. */
.ps-share{display:inline-flex;align-items:center;justify-self:start;gap:7px;min-height:40px;margin-top:2px;padding:0;border:0;background:none;color:var(--c-muted);font-size:14px;font-weight:600;text-decoration:underline;text-underline-offset:3px}
.ps-share svg{width:16px;height:16px}
.ps-foot{display:grid;gap:6px;padding:12px 20px calc(14px + env(safe-area-inset-bottom));border-top:1px solid var(--c-line)}
.ps-buy{display:flex;gap:10px}
.qty.big{flex:0 0 132px;height:52px;border-radius:14px}
.ps-buy .btn{flex:1;min-height:52px;border-radius:14px;font-size:16px;font-weight:700}
.ps-ask{display:inline-flex;align-items:center;justify-content:center;gap:8px;min-height:44px;color:var(--c-ink);font-weight:600;text-underline-offset:3px}
.ps-ask-btn{min-height:52px;border-radius:14px;font-size:16px;font-weight:700}
.sheet-info{padding:56px 20px 8px}
.sheet-info h2{font:700 26px/1.1 var(--serif)}
.menu{display:grid;padding:60px 20px 8px}
.menu a{display:block;padding:14px 0;border-bottom:1px solid var(--c-line);color:var(--c-ink);font:600 26px/1.1 var(--serif);text-decoration:none}
.menu-foot{display:grid;justify-items:start;gap:16px;padding:18px 20px calc(22px + env(safe-area-inset-bottom))}
.menu-foot .ec{height:28px}
.order{padding:56px 20px 8px}
.order h2{font:700 28px/1.1 var(--serif)}
.olines{margin:12px 0 0;padding:0;list-style:none}
.oline{display:grid;grid-template-columns:56px minmax(0,1fr) auto;align-items:center;gap:12px;padding:14px 0;border-bottom:1px solid var(--c-line)}
.oline .ph{width:56px;height:56px;border-radius:10px}
.oline-txt{display:grid;gap:1px;min-width:0;font-size:14px}
.oline-txt b{font-size:15px}
.oline-sub{color:var(--price);font-weight:700}
.oline-sub small{color:var(--c-muted);font-weight:500}
.qty.sm{flex:none;width:112px;height:38px}
.o-empty{display:grid;justify-items:start;gap:12px;padding:16px 0;color:var(--c-muted)}
.order-foot{display:grid;gap:10px;padding:14px 20px calc(16px + env(safe-area-inset-bottom));border-top:1px solid var(--c-line);background:var(--paper)}
.order-foot:has(#o-form[hidden]){display:none}
.total{display:flex;align-items:baseline;justify-content:space-between;font-size:17px;font-weight:700}
.total b{font-size:22px;font-weight:800;font-variant-numeric:tabular-nums}
.o-form{display:grid;gap:10px}
.o-fields{display:grid;gap:8px}
.o-fields label{display:grid;gap:4px;color:var(--c-ink-2);font-size:13px;font-weight:600}
.o-aviso{margin:0;padding:9px 12px;border-radius:10px;background:var(--warn-soft);color:var(--warn-ink);font-size:13px;line-height:1.4}
.o-modo{display:grid;grid-template-columns:1fr 1fr;gap:4px;padding:4px;border-radius:12px;background:var(--sand)}
.o-modo label{position:relative;display:flex;align-items:center;justify-content:center;gap:6px;min-height:44px;padding:0 8px;border-radius:9px;color:var(--c-ink-2);font-size:14px;font-weight:700;text-align:center;cursor:pointer}
.o-modo label svg{flex:none;width:18px;height:18px}
.o-modo input{position:absolute;opacity:0;pointer-events:none}
.o-modo label:has(input:checked){background:var(--paper);color:var(--c-ink);box-shadow:0 1px 3px rgba(33,28,22,.16)}
.o-modo label:has(input:focus-visible){outline:2px solid var(--c-ink);outline-offset:2px}
.o-linea{display:flex;justify-content:space-between;color:var(--c-ink-2);font-size:14px;font-variant-numeric:tabular-nums}
.o-form .btn-brand{min-height:52px;border-radius:14px;font-size:16px;font-weight:700}
.o-form .link{justify-self:center}
body:has(.sheet-bg) #toast{top:calc(16px + env(safe-area-inset-top));bottom:auto} /* con una hoja abierta el aviso no tapa sus botones */

/* No disponible */
.na{display:grid;place-items:center;min-height:100vh;padding:24px;text-align:center}
.na-box{display:grid;justify-items:center;gap:12px;max-width:340px;color:var(--muted)}
.na-icon{display:grid;place-items:center;width:64px;height:64px;border-radius:20px;background:var(--wash);color:var(--ink)}
.na-icon svg{width:30px;height:30px}
.na-box h1{color:var(--ink);font-size:24px;font-weight:800}
.na-box .ec{margin-top:24px}
.how{display:grid;align-content:center;gap:16px;padding:22px 20px;border-radius:14px;background:var(--night) var(--grain);color:#ECE4D6}
.how h3{color:#fff;font:600 24px/1.1 var(--serif);letter-spacing:-.01em}
.how ol{display:grid;gap:12px;margin:0;padding:0;list-style:none;counter-reset:paso}
.how li{display:grid;grid-template-columns:30px minmax(0,1fr);align-items:start;gap:10px;font-size:14px;line-height:1.4;counter-increment:paso}
.how li::before{content:counter(paso);display:grid;place-items:center;width:30px;height:30px;border-radius:50%;background:var(--accent);color:var(--on-accent);font:700 14px/1 var(--font)}
.how-btn{justify-self:start;border-color:var(--accent);background:var(--accent);color:var(--on-accent);font-weight:700}
.how-btn:hover{background:var(--accent);filter:brightness(1.07)}
.how.wide{gap:22px;padding:32px 36px}
.how.wide h3{font-size:36px}
.how.wide li{font-size:15px}
.how.wide li::before{width:38px;height:38px;font-size:16px}
.how.wide ol{grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:22px}
.how.wide li{grid-template-columns:minmax(0,1fr)}
.no-hits{display:grid;justify-items:center;gap:6px;padding:56px 24px;color:var(--c-muted);text-align:center}
.no-hits b{color:var(--c-ink);font-size:17px}
.proto-note{max-width:420px}
.cred-list{display:grid;gap:0;margin:14px 0 0;padding:0;list-style:none;font-size:14px}
.cred-list li{padding:10px 0;border-bottom:1px solid var(--line)}
.cred-list a{color:var(--muted)}
[hidden]{display:none!important}

/* Hoja inferior */
.sheet-bg{position:fixed;inset:0;z-index:60;display:flex;align-items:flex-end;justify-content:center;background:rgba(24,24,27,.5);animation:fade .25s ease-out}
.sheet{position:relative;display:flex;flex-direction:column;width:100%;max-width:560px;max-height:92vh;border-radius:var(--r-xl) var(--r-xl) 0 0;background:#fff;box-shadow:var(--sh-3);animation:rise .32s var(--ease)}
.sheet-scroll{overflow-y:auto;overscroll-behavior:contain}
.drawer .sheet-scroll{flex:1}
.sheet .grab{position:absolute;top:8px;left:50%;z-index:2;width:40px;height:4px;border-radius:2px;background:rgba(255,255,255,.85);transform:translateX(-50%)}
.sheet-close{position:absolute;top:14px;right:14px;z-index:3}
.sheet-ph{aspect-ratio:4/3;border-radius:var(--r-xl) var(--r-xl) 0 0}
.sheet-cta{padding:12px 20px calc(16px + env(safe-area-inset-bottom));border-top:1px solid var(--line)}
.sheet-cta .btn{width:100%;min-height:52px;border-radius:14px;font-size:16px;font-weight:700}
@keyframes fade{from{opacity:0}}
@keyframes rise{from{transform:translateY(100%)}}
@keyframes pop{from{opacity:0;transform:translateY(16px) scale(.98)}}
@keyframes slide{from{transform:translateX(100%)}}

@media(min-width:960px){
  html:has(.top){scroll-padding-top:84px}
  .top-in{gap:18px;max-width:1304px;height:84px;padding:0 32px}
  .menu-btn{display:none}
  .brandlink{flex:none;justify-content:flex-start;gap:12px}
  .brandlink .logo{width:58px;height:58px}
  .brandlink .logo.has-img{padding:4px}
  .bn small{font-size:15px}
  .bn b{font-size:30px}
  .bn i{margin-top:3px;font-size:11px}
  .nav{display:flex;flex:1;justify-content:center;gap:32px}
  .nav-a{position:relative;padding:8px 0;color:#E6DDCF;font-size:15px;font-weight:500;text-decoration:none}
  .nav-a:hover,.nav-a[aria-current=page]{color:#fff}
  .nav-a[aria-current=page]::after{content:"";position:absolute;left:0;right:0;bottom:0;height:2px;border-radius:2px;background:#fff}
  .find-btn{display:grid}
  .cart-txt{display:block}
  .hero{justify-content:center;min-height:min(800px,96vh);padding:150px max(32px,calc((100% - 1240px)/2)) 176px}
  .hero > img{object-position:72% 50%}
  .hero::before{background:linear-gradient(90deg,var(--night) 0%,rgba(21,18,13,.92) 30%,rgba(21,18,13,.55) 56%,rgba(21,18,13,.22) 82%),linear-gradient(0deg,var(--night) 0%,rgba(21,18,13,0) 26%),linear-gradient(180deg,rgba(21,18,13,.8) 0%,rgba(21,18,13,0) 24%)}
  .hero h1{max-width:7.4em;font-size:clamp(64px,5.8vw,84px);line-height:1}
  .hero-sub{font-size:18px}
  .hero-find{display:none}
  .hero .hero-cta{display:inline-flex;align-self:flex-start;margin-top:32px}
  .hero-note{position:absolute;top:22%;left:max(47%,calc(50% + 40px));display:block;max-width:8em;color:#EDE4D3;font:400 40px/1.15 var(--script);text-shadow:0 2px 3px rgba(0,0,0,.55),0 4px 24px rgba(0,0,0,.85);transform:rotate(-7deg)}
  .hero-note::before{content:"";position:absolute;inset:-34px -56px -20px -48px;z-index:-1;background:radial-gradient(closest-side,rgba(21,18,13,.7),rgba(21,18,13,0))}
  .note-arrow{display:block;width:56px;height:48px;margin:6px 0 0 34px;color:#EDE4D3}
  .stamp{position:absolute;right:max(40px,calc((100vw - 1240px)/2 + 40px));bottom:160px;display:block;width:140px;height:140px}
  .sellos{position:relative;z-index:1;display:grid;grid-template-columns:repeat(4,auto);justify-content:space-between;gap:28px;margin:-104px 0 0;padding:26px max(32px,calc((100% - 1240px)/2)) 74px;list-style:none;background:linear-gradient(180deg,rgba(21,18,13,0),var(--night) 30%);color:#E3DACB;font-size:14.5px;line-height:1.35}
  .sellos::before{content:"";position:absolute;top:26px;left:max(32px,calc((100% - 1240px)/2));right:max(32px,calc((100% - 1240px)/2));border-top:1px solid rgba(243,235,221,.14)}
  .sellos li{display:flex;align-items:center;gap:12px;padding-top:22px;white-space:nowrap} /* cada sello en una línea */
  .sellos svg{width:32px;height:32px;color:var(--accent);stroke-width:1.6}
  .counter .wave{top:-38px;height:40px}
  .circles{display:none}
  .planes{padding:44px 32px 10px}
  .side-note{position:absolute;z-index:2;top:26px;left:max(32px,calc((100vw - 1180px)/2 - 8px));display:block;max-width:5em;color:color-mix(in srgb,var(--accent) 62%,#3A2706);font:400 40px/1.05 var(--script);transform:rotate(-10deg)}
  .sec-title{font-size:48px}
  .sec-sub{font-size:18px}
  .plan-rail{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:20px;max-width:1240px;margin:0 auto;padding:32px 0 8px;overflow:visible}
  .plan{width:auto;min-height:180px}
  .plan-txt{padding:22px 18px 60px}
  .plan-txt b{font-size:26px}
  .plan-desc{font-size:14px}
  .favs{position:relative;padding:56px 32px 44px}
  .favs-head{display:grid;grid-template-columns:auto minmax(0,1fr) auto;align-items:center;column-gap:44px;max-width:1240px;margin:0 auto;padding:0}
  .script{max-width:4.4em;font:400 56px/.84 var(--script);letter-spacing:0;transform:rotate(-6deg);transform-origin:left center}
  .chips{display:flex;grid-column:2;grid-row:1;gap:26px;overflow-x:auto;-webkit-mask-image:linear-gradient(90deg,#000 calc(100% - 36px),transparent);mask-image:linear-gradient(90deg,#000 calc(100% - 36px),transparent);scrollbar-width:none}
  .chip{position:relative;flex:none;padding:10px 0 12px;border:0;background:none;color:var(--c-muted);font-size:14.5px;font-weight:600;white-space:nowrap}
  .chip:hover,.chip[aria-pressed=true]{color:var(--c-ink)}
  .chip[aria-pressed=true]::after{content:"";position:absolute;left:0;right:0;bottom:2px;height:2px;border-radius:2px;background:var(--c-ink)}
  .favs-act{grid-column:3}
  .rail-btn{display:grid;place-items:center;width:42px;height:42px;padding:0;border:1px solid var(--c-line);border-radius:50%;background:var(--paper);color:var(--c-ink)}
  .rail-btn:hover{border-color:var(--c-ink)}
  .rail-btn svg{width:18px;height:18px}
  .prow{gap:18px;max-width:1240px;margin:0 auto;padding:30px 4px 30px;scroll-padding-inline:0} /* aire para la sombra y la elevación */
  .deco{position:absolute;z-index:1;display:block;height:auto;filter:drop-shadow(0 14px 16px rgba(60,40,20,.3))}
  .deco-laurel{top:-62px;left:max(-64px,calc((100vw - 1440px)/2 - 64px));width:200px;transform:rotate(-28deg)}
  .deco-perejil{top:6px;right:max(-34px,calc((100vw - 1440px)/2 - 34px));width:230px;transform:rotate(-16deg)}
  .deco-laurel-b{right:max(-44px,calc((100vw - 1440px)/2 - 44px));bottom:-78px;width:190px;transform:scaleX(-1) rotate(-26deg)}
  .prow .pc{width:clamp(208px,calc((100% - 72px)/5),250px)}
  .prow .pc-desc{display:-webkit-box}
  .prow .pres{display:block}
  .band{align-items:center;min-height:500px;padding:96px 32px}
  .band::before{background:linear-gradient(90deg,rgba(21,18,13,.92) 0%,rgba(21,18,13,.6) 38%,rgba(21,18,13,.05) 75%)}
  .band .wave{height:40px}
  .band-tag{top:96px;right:max(48px,calc((100vw - 1240px)/2 + 48px));padding:14px 22px 16px;font-size:34px}
  .band h2{font-size:68px}
  .band-in p{font-size:18px}
  .visit{padding:64px 32px 96px}
  .visit .wave{top:-38px;height:40px}
  .v-shop{width:300px}
  .visit-note{font-size:38px}
  .v-in h2{font-size:clamp(52px,4.4vw,64px)}
  .v-sub{font-size:18px}
  .v-was{display:flex;flex-wrap:wrap;justify-content:center;gap:14px;margin-top:30px}
  .v-wa{min-width:280px}
  .v-info{grid-template-columns:repeat(3,minmax(0,1fr));margin-top:46px}
  .v-item{padding:4px 28px;border-top:0;border-left:1px solid var(--c-line)}
  .v-item:first-child{border-left:0}
  .foot{padding:64px 32px 34px}
  .foot-in{grid-template-columns:minmax(0,1.4fr) minmax(0,1fr) minmax(0,1fr);gap:48px}
  .foot-legal{grid-template-columns:minmax(0,1fr) auto;align-items:center}
  .cat-hero{padding:136px 32px 96px}
  .cat-hero h1{font-size:64px}
  .cat-hero p:not(.eyebrow){font-size:18px}
  .cat-wrap{margin-top:-44px;padding:26px 32px 72px}
  .cat-tools{grid-template-columns:minmax(0,1fr) auto auto;gap:12px;margin:16px 0 8px}
  .c-search{grid-column:auto}
  .filters-btn{display:none}
  .view-tog{display:flex;overflow:hidden;border:1px solid var(--c-line);border-radius:12px;background:#fff}
  .view-tog button{display:grid;place-items:center;width:44px;height:44px;padding:0;border:0;background:none;color:var(--c-muted)}
  .view-tog button[aria-pressed=true]{background:var(--brand);color:#fff}
  .cat-layout{display:grid;grid-template-columns:250px minmax(0,1fr);align-items:start;gap:30px}
  .filters{position:sticky;top:100px;display:grid;gap:18px}
  .f-card{display:grid;gap:18px;padding:20px;border-radius:16px;background:var(--paper);box-shadow:0 0 0 1px rgba(33,28,22,.06)}
  .f-card .f-group + .f-group{padding-top:16px;border-top:1px solid var(--c-line)}
  .grid{grid-template-columns:repeat(auto-fill,minmax(210px,1fr));gap:18px}
  .as-list{grid-template-columns:minmax(0,1fr)}
  .as-list .pc{flex-wrap:nowrap;align-items:center}
  .as-list .pc .ph{width:132px}
  .as-list .heart{left:86px}
  .as-list .buy{flex:0 0 210px;padding:12px 16px}
  .sheet .grab{display:none}
  .sheet-bg{align-items:center;padding:24px}
  .sheet{border-radius:var(--r-xl);animation:pop .3s var(--ease)}
  .sheet-bg:has(.drawer){align-items:stretch;justify-content:flex-end;padding:0}
  .sheet.drawer{max-width:440px;max-height:none;height:100%;border-radius:0;animation:slide .32s var(--ease)}
  #toast{bottom:32px}
}
@media(min-width:960px) and (max-width:1199px){
  .sellos{grid-template-columns:repeat(2,minmax(0,1fr));row-gap:0}
}

'''

ADMIN_PREVIEW_OLD = '''.preview{overflow:hidden;border:1px solid var(--line);border-radius:var(--r)}
.preview .cover{display:block;width:100%;height:auto;aspect-ratio:1600/754;margin:0;padding:0;border:0;border-radius:0;cursor:pointer}
.preview .cover::after{display:none}
.preview .pv-body{position:relative;display:flex;align-items:flex-end;gap:10px;min-width:0;padding:0 12px 12px;margin-top:-28px}
.preview .pv-body b{overflow:hidden;white-space:nowrap;text-overflow:ellipsis}
.pv-logo{position:relative;flex:none;padding:0;border:0;border-radius:26%;background:none;cursor:pointer}
.preview .logo{width:60px;height:60px;border:2px solid #fff;font-size:20px;box-shadow:var(--sh-1)}
.pv-cam{position:absolute;right:8px;bottom:8px;display:grid;place-items:center;width:32px;height:32px;border-radius:50%;background:rgba(24,24,27,.72);color:#fff}
.pv-cam svg{width:17px;height:17px}
.pv-logo .pv-cam{right:-6px;bottom:-6px;width:26px;height:26px;border:2px solid #fff}
.pv-logo .pv-cam svg{width:14px;height:14px}
.pv-empty{display:grid;place-items:center;height:100%;color:#fff;font-size:13px;font-weight:600}
'''
ADMIN_PREVIEW_NEW = '''.pv{position:relative;overflow:hidden;border:1px solid var(--line);border-radius:var(--r);background:#F7F1E6}
.pv-cover{position:relative;display:block;width:100%;aspect-ratio:1600/754;padding:0;border:0;background:#15120D;color:#fff;text-align:left;cursor:pointer;overflow:hidden}
.pv-cover > img{position:absolute;inset:0;width:100%;height:100%;object-fit:cover}
.pv-cover::after{content:"";position:absolute;inset:0;background:linear-gradient(90deg,rgba(21,18,13,.94),rgba(21,18,13,.55) 50%,rgba(21,18,13,.08))}
.pv-txt{position:absolute;left:14px;bottom:12px;z-index:1;display:grid;justify-items:start;gap:7px;max-width:70%}
.pv-t{font:600 16px/1.05 var(--serif);letter-spacing:-.01em}
.pv-t em{display:block;color:var(--accent);font-style:italic}
.pv-btn{padding:5px 9px;border-radius:5px;background:var(--accent);color:#231A0E;font-size:10.5px;font-weight:700}
.pv-logo{position:absolute;top:10px;left:10px;z-index:2;padding:0;border:0;border-radius:50%;background:none;cursor:pointer}
.pv .logo{width:32px;height:32px;border-radius:50%;font-size:12px}
.pv .logo.has-img{padding:2px;background:#fff}
.pv-cam{position:absolute;right:8px;bottom:8px;z-index:1;display:grid;place-items:center;width:32px;height:32px;border-radius:50%;background:rgba(24,24,27,.72);color:#fff}
.pv-cam svg{width:17px;height:17px}
.pv-logo .pv-cam{right:-7px;bottom:-7px;width:24px;height:24px;border:2px solid #fff}
.pv-logo .pv-cam svg{width:13px;height:13px}
.pv-empty{position:absolute;top:12px;right:12px;z-index:1;padding:4px 9px;border-radius:6px;background:rgba(255,255,255,.16);color:#fff;font-size:12px;font-weight:600}
.pv-buy{display:flex;align-items:center;justify-content:space-between;gap:10px;padding:10px 12px;color:#4A4036;font-size:13px}
.pv-add{display:grid;place-items:center;width:40px;height:34px;border-radius:9px;background:var(--brand);color:#fff}
.pv-add svg{width:18px;height:18px}
'''
ADMIN_EXTRA = '''.shop-pick{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:10px;margin-top:14px}
.shop-opt{position:relative;display:grid;justify-items:center;gap:6px;padding:12px 10px 10px;border:1px solid var(--line);border-radius:var(--r);background:#fff;color:#7A5A2A;cursor:pointer}
.shop-opt:hover{border-color:#A1A1AA}
.shop-opt:has(input:checked){border-color:#18181B;box-shadow:0 0 0 1px #18181B}
.shop-opt:has(input:focus-visible){outline:2px solid #18181B;outline-offset:2px}
.shop-opt input{position:absolute;opacity:0;pointer-events:none}
.shop-opt svg{width:100%;height:auto}
.shop-opt span{color:#18181B;font-size:13.5px;font-weight:600}
.shop-none{display:grid;place-items:center;width:100%;aspect-ratio:260/168;border:1px dashed var(--line);border-radius:8px;color:#A1A1AA;font-size:13px}

/* Colecciones, franjas y sellos */
.col-list{margin:16px 0 0;padding:0;list-style:none;border:1px solid var(--line);border-radius:var(--r-lg);background:#fff;overflow:hidden}
.col-list li{display:flex;align-items:center;gap:14px;padding:12px 14px;border-bottom:1px solid var(--line)}
.col-list li:last-child{border-bottom:0}
.col-list .cat-thumb{width:56px;height:56px}
.col-list .grow{display:grid;gap:2px}
.col-list .muted{font-size:13.5px}
.pick-search{margin:12px 0 8px;border:1px solid var(--line-2);background:#fff}
.pick-list{display:grid;max-height:440px;margin:0;padding:0;overflow-y:auto;list-style:none;border:1px solid var(--line);border-radius:var(--r)}
.pick-list li{border-bottom:1px solid var(--line)}
.pick-list li:last-child{border-bottom:0}
.pick-list label{display:flex;align-items:center;gap:12px;padding:8px 12px;cursor:pointer}
.pick-list label:hover{background:var(--wash)}
.pick-list .ph{flex:none;width:40px;height:40px;border-radius:8px}
.pick-list input{flex:none;width:18px;height:18px;margin:0;accent-color:var(--ink)}
.pick-list .muted{display:block;font-size:13px}
.sortable .grow{display:flex;flex-wrap:wrap;align-items:center;gap:4px 8px}
.chip-on{display:inline-flex;align-items:center;height:22px;padding:0 8px;border-radius:6px;background:var(--ok-soft);color:var(--ok);font-size:12px;font-weight:700;white-space:nowrap}
.banner-prev .ph{aspect-ratio:16/6;border-radius:var(--r)}
.sellos-ed{display:grid;gap:8px;margin-top:12px}
.sello-row{display:grid;grid-template-columns:150px minmax(0,1fr);gap:8px}
@media(max-width:640px){.sello-row{grid-template-columns:132px minmax(0,1fr)}.sortable .cat-thumb{display:none}}
'''


PANEL_BRAND = '''
/* ---------- Marca Agorá en el panel, el login y "no disponible" ---------- */
/* Tinta cálida del sello en lugar del zinc; el foco pasa del verde viejo al dorado oscuro (5,5:1 sobre blanco). */
body[data-surface=panel]{--ink:#1C1814;--ink-2:#3D362F;--muted:#6B6259;--faint:#776D63;--line:#ECE6DC;--line-2:#DCD3C6;--wash:#F6F2EA;--brand-2:#8E5F18;--gold:#E0A43A;--serif:'Playfair Display',Georgia,serif;background:var(--wash)}
body[data-surface=panel] .btn-dark:hover{background:#342C24}
body[data-surface=panel] .input:focus{box-shadow:0 0 0 3px rgba(142,95,24,.16)}
body[data-surface=panel] .switch{background:#D6CDBF}
body[data-surface=panel] .switch:checked{background:var(--ok)}
body[data-surface=panel] .table th{background:#FBF8F3}
body[data-surface=panel] .table tbody tr:hover td,body[data-surface=panel] .offer{background:#FDFBF7}
body[data-surface=panel] .savebar .btn:not(.btn-wa){border-color:#4A4038}
body[data-surface=panel] .shop-opt:hover{border-color:#B9AE9F}
body[data-surface=panel] .shop-opt:has(input:checked){border-color:var(--ink);box-shadow:0 0 0 1px var(--ink)}
body[data-surface=panel] .shop-opt:has(input:focus-visible){outline-color:var(--brand-2)}
body[data-surface=panel] .shop-opt span{color:var(--ink)}
body[data-surface=panel] .shop-none{color:var(--faint)}
/* Títulos de página en la serif del logo; todo lo funcional sigue en Onest. */
body[data-surface=panel] :is(.page-head h1,.login-box h1,.na-box h1){font-family:var(--serif);font-weight:600;letter-spacing:-.005em}
body[data-surface=panel] .page-head h1{font-size:34px;line-height:1.1}
body[data-surface=panel] .login-box h1{font-size:30px;line-height:1.1}
/* Selección del menú en tinta con el ícono en el dorado del sello (8:1). */
body[data-surface=panel] .side a[aria-current=page] svg{color:var(--gold)}
body[data-surface=panel] .login{background:var(--wash)}
body[data-surface=panel] .login-box{box-shadow:0 1px 2px rgba(28,24,20,.05),0 24px 48px -32px rgba(28,24,20,.35)}
body[data-surface=panel] .na{background:var(--wash)}
body[data-surface=panel] .na-box{max-width:360px}
body[data-surface=panel] .na-icon{background:#fff;box-shadow:0 0 0 1px var(--line)}
body[data-surface=panel] .na-box h1{font-size:30px;line-height:1.15}
body[data-surface=panel] .na-box .ec{height:30px;margin-top:28px}
@media(max-width:860px){
  body[data-surface=panel] .page-head h1{font-size:30px}
  body[data-surface=panel] .mtop a{display:inline-flex;align-items:center;min-height:44px;padding:0 4px} /* "Salir" medía 17 px de alto */
}
'''
