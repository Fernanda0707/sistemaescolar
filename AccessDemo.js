/* Membresías y créditos DEMO. NO procesa dinero ni es autorización de servidor. */
(function () {
    'use strict';
    const plans = Object.freeze([
        { id: 'basica', name: 'Básica', price: 49, credits: 60 },
        { id: 'plus', name: 'Plus', price: 99, credits: 150 },
        { id: 'premium', name: 'Premium', price: 199, credits: 350 }
    ]);
    const durations = Object.freeze([
        { id: 'minutos', name: '15 minutos', duration: 15 * 60 * 1000, credits: 15 },
        { id: 'horas', name: '2 horas', duration: 2 * 60 * 60 * 1000, credits: 60 },
        { id: 'dias', name: '7 días', duration: 7 * 24 * 60 * 60 * 1000, credits: 150 }
    ]);
    const modules = Object.freeze({grades: 'Kárdex (exportación CSV)', weather: 'Clima (capas avanzadas)'});
    let user = null;
    let store = null;

    function storageKey() { return 'sistema_escolar_acceso_demo_v1_' + user.id; }
    function cleanStored(data) {
        if (!data || typeof data !== 'object') return {plan:'ninguna',credits:0,passes:{},history:[]};
        const passes = {};
        for (const key of Object.keys(modules)) {
            const value = Number(data.passes && data.passes[key]);
            passes[key] = Number.isSafeInteger(value) && value > 0 ? value : 0;
        }
        return {
            plan: plans.some(p=>p.id===data.plan) ? data.plan : 'ninguna',
            credits: Number.isSafeInteger(data.credits) ? Math.min(100000,Math.max(0,data.credits)) : 0,
            passes, history:Array.isArray(data.history) ? data.history.slice(-30).filter(v=>v && typeof v.description==='string') : []
        };
    }
    function persist() {
        try { localStorage.setItem(storageKey(),JSON.stringify(store)); }
        catch (_) { notify('No se pudo guardar el saldo en este navegador.'); return false; }
        return true;
    }
    function notify(message) {
        if (typeof showToast === 'function') showToast(message);
        else window.alert(message);
    }
    function log(description) {
        store.history.push({description, date:new Date().toLocaleString('es-MX')});
        store.history = store.history.slice(-30);
    }
    function login(nextUser) {
        user = nextUser && nextUser.id ? nextUser : null;
        if (!user) { store = null; return; }
        try { store = cleanStored(JSON.parse(localStorage.getItem(storageKey()) || 'null')); }
        catch (_) { store = cleanStored(null); }
        render();
    }
    function eligible() { return !!user && !!store && ['ALUMNO','PROFESOR','FINANZAS','ADMINISTRACION'].includes(user.rol); }
    function check(module, cost) {
        if (!eligible() || !Object.hasOwn(modules,module)) { notify('No tienes permiso para esta función.'); return false; }
        if (Number(store.passes[module]) > Date.now()) return true;
        if (!Number.isSafeInteger(cost) || cost < 1 || store.credits < cost) {
            notify('Necesitas créditos o un paquete por tiempo activo. Ve a Membresías.');
            return false;
        }
        const old = JSON.stringify(store);
        store.credits -= cost;
        log('Función ' + modules[module] + ': -' + cost + ' créditos');
        if (!persist()) { store = JSON.parse(old); return false; }
        render();
        return true;
    }
    function buyPlan(id) {
        if (!eligible()) return notify('Inicia sesión para activar una membresía.');
        const plan = plans.find(p => p.id === id);
        if (!plan) return;
        if (!confirm('Simular compra de membresía ' + plan.name + ' por $' + plan.price + ' MXN ficticios y recibir ' + plan.credits + ' créditos? NO se cobrará dinero.')) return;
        if (store.credits + plan.credits > 100000) return notify('Límite de créditos de demostración alcanzado.');
        const old = JSON.stringify(store);
        store.plan = plan.id;
        store.credits += plan.credits;
        log('Membresía ' + plan.name + ' (compra DEMO): +' + plan.credits + ' créditos');
        if (!persist()) { store = JSON.parse(old); return; }
        render();
        notify('Membresía DEMO activada. Saldo: ' + store.credits + ' créditos.');
    }
    function buyPass(module, id) {
        if (!eligible() || !Object.hasOwn(modules,module)) return notify('Acceso no autorizado.');
        if (module === 'grades' && !['ALUMNO','PROFESOR','ADMINISTRACION'].includes(user.rol))
            return notify('Tu rol no tiene permiso para exportar Kárdex.');
        const packageData = durations.find(d=>d.id===id);
        if (!packageData) return;
        if (store.credits < packageData.credits) return notify('Créditos insuficientes; activa una membresía.');
        if (!confirm('Activar acceso a ' + modules[module] + ' durante ' + packageData.name + ' por ' + packageData.credits + ' créditos DEMO?')) return;
        const old = JSON.stringify(store);
        store.credits -= packageData.credits;
        const now = Date.now();
        store.passes[module] = Math.max(now, store.passes[module] || 0) + packageData.duration;
        log('Paquete ' + packageData.name + ' para ' + modules[module] + ': -' + packageData.credits + ' créditos');
        if (!persist()) { store = JSON.parse(old); return; }
        render();
        notify('Paquete de tiempo activado en ' + modules[module] + '.');
    }
    function render() {
        const pane = document.getElementById('membership-content');
        if (!pane || !eligible()) return;
        document.getElementById('membership-balance').textContent = store.credits.toLocaleString('es-MX') + ' créditos';
        document.getElementById('membership-current').textContent = plans.find(p=>p.id===store.plan)?.name || 'Sin membresía';
        const planList = document.getElementById('membership-plans');
        planList.replaceChildren();
        for (const plan of plans) {
            const card = document.createElement('div');
            card.className = 'rounded-xl border border-slate-600 bg-slate-900/70 p-4 space-y-2';
            const title = document.createElement('h4');
            title.className = 'font-bold text-lg text-white';
            title.textContent = plan.name;
            const desc = document.createElement('p');
            desc.className = 'text-sm text-slate-300';
            desc.textContent = '$' + plan.price + ' MXN DEMO → ' + plan.credits + ' créditos';
            const btn = document.createElement('button');
            btn.className = 'bg-indigo-600 hover:bg-indigo-500 rounded-lg py-2 px-4 text-white text-sm font-semibold';
            btn.textContent = 'Activar ' + plan.name + ' (simulado)';
            btn.addEventListener('click',()=>buyPlan(plan.id));
            card.append(title,desc,btn);
            planList.appendChild(card);
        }
        const packageList = document.getElementById('membership-packages');
        packageList.replaceChildren();
        for (const [key,label] of Object.entries(modules)) {
            if (key === 'grades' && !['ALUMNO','PROFESOR','ADMINISTRACION'].includes(user.rol)) continue;
            const box = document.createElement('div');
            box.className = 'bg-slate-900/70 border border-slate-600 rounded-xl p-4 space-y-3';
            const h = document.createElement('h4');
            h.className = 'font-bold text-white';
            h.textContent = label;
            const expiry = document.createElement('p');
            expiry.className = 'text-xs text-slate-400';
            expiry.textContent = store.passes[key] > Date.now() ?
                'Acceso activo hasta ' + new Date(store.passes[key]).toLocaleString('es-MX') :
                'Sin paquete activo (puedes usar créditos por operación)';
            box.append(h,expiry);
            for (const pack of durations) {
                const btn = document.createElement('button');
                btn.className = 'mr-2 mb-2 px-3 py-2 bg-emerald-700 hover:bg-emerald-600 rounded-lg text-xs text-white';
                btn.textContent = pack.name + ' · ' + pack.credits + ' créditos';
                btn.addEventListener('click',()=>buyPass(key,pack.id));
                box.appendChild(btn);
            }
            packageList.appendChild(box);
        }
        const history = document.getElementById('membership-history');
        history.replaceChildren();
        if (!store.history.length) history.textContent = 'Todavía no hay operaciones.';
        store.history.slice().reverse().forEach(item => {
            const p = document.createElement('p');
            p.className = 'p-2 border-b border-slate-700 text-xs text-slate-300';
            p.textContent = item.date + ' · ' + item.description;
            history.appendChild(p);
        });
        for (const [module,domId] of [['grades','grade-premium-status'],['weather','weather-premium-status']]) {
            const target = document.getElementById(domId);
            if (target) target.textContent = store.passes[module] > Date.now() ?
                'Paquete activo hasta ' + new Date(store.passes[module]).toLocaleString('es-MX') :
                'Sin paquete activo · se descuentan créditos por operación';
        }
    }
    window.AccessDemo = Object.freeze({login,check,render});
})();