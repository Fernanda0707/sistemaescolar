/* DEMO académica de licenciamiento B2B. No es autorización segura de producción. */
(function () {
    'use strict';
    const COMPANY_ID = 'escuela-demo-durango';
    const STORAGE_KEY = 'licencia_empresa_demo_v1_' + COMPANY_ID;
    const FEATURES = Object.freeze({
        dashboard:'Panel principal', students:'Directorio de alumnos', courses:'Cursos y horarios',
        grades:'Consulta de Kárdex', attendance:'Control de asistencia',
        enrollment:'Inscripciones', finance:'Estado de cuenta y cobros',
        notifications:'Notificaciones', weather:'Mapa del clima',
        exportGrades:'Exportación de Kárdex CSV', weatherLayers:'Capas avanzadas del clima',
        nominas:'Integración con Nóminas', timbrar:'Timbrado CFDI'
    });
    const BRONZE = ['dashboard','students','courses','grades','attendance'];
    const SILVER = [...BRONZE,'enrollment','finance','notifications','weather','exportGrades','weatherLayers'];
    const GOLDEN = [...SILVER,'nominas','timbrar'];
    const PLANS = Object.freeze({
        bronze:{name:'Bronce',features:BRONZE,credits:0},
        silver:{name:'Silver',features:SILVER,credits:120},
        golden:{name:'Golden',features:GOLDEN,credits:350}
    });
    const DURATIONS = Object.freeze({
        minutos:{label:'15 minutos',ms:15*60*1000},
        horas:{label:'2 horas',ms:2*60*60*1000},
        dias:{label:'7 días',ms:7*24*60*60*1000}
    });
    const PREMIUM = Object.freeze({grades:{feature:'exportGrades',label:'Exportar Kárdex',cost:8},weather:{feature:'weatherLayers',label:'Capas del clima',cost:4}});
    let currentUser = null;
    let license = null;

    function fresh() { return {company:COMPANY_ID,plan:'bronze',expires:0,credits:0,passes:{grades:0,weather:0},history:[]}; }
    function load() {
        let data = null;
        try { data = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null'); } catch (_) {}
        const d = fresh();
        if (data && data.company === COMPANY_ID) {
            d.plan = Object.hasOwn(PLANS,data.plan) ? data.plan : 'bronze';
            d.expires = Number.isSafeInteger(data.expires) && data.expires>0 ? data.expires : 0;
            d.credits = Number.isSafeInteger(data.credits) ? Math.max(0,Math.min(50000,data.credits)) : 0;
            for(const id of Object.keys(PREMIUM)) {
                const value = Number(data.passes?.[id]);
                d.passes[id] = Number.isSafeInteger(value) && value>0 ? value : 0;
            }
            d.history = Array.isArray(data.history) ? data.history.filter(x=>x && typeof x.text==='string').slice(-40) : [];
        }
        return d;
    }
    function save() {
        try { localStorage.setItem(STORAGE_KEY,JSON.stringify(license)); return true; }
        catch (_) { alert('No se pudo guardar la licencia DEMO.'); return false; }
    }
    function note(text) {
        license.history.push({text,at:new Date().toLocaleString('es-MX')});
        license.history = license.history.slice(-40);
    }
    function isDeveloper() { return currentUser?.rol === 'DESARROLLADOR'; }
    function active() { return license && license.expires>Date.now(); }
    function enabled(feature) { return !!(active() && PLANS[license.plan].features.includes(feature)); }
    function canOpen(tab) {
        if (!currentUser) return false;
        if (tab === 'memberships') return true;
        if (isDeveloper()) return false;
        return enabled(tab);
    }
    function apply(plan,duration) {
        if (!isDeveloper()) return alert('Solo el proveedor del software puede asignar licencias.');
        if (!Object.hasOwn(PLANS,plan) || !Object.hasOwn(DURATIONS,duration)) return;
        const value = PLANS[plan];
        if (!confirm('DEMO proveedor: activar licencia '+value.name+' a '+COMPANY_ID+' durante '+DURATIONS[duration].label+'? No se cobra dinero.')) return;
        const before = JSON.stringify(license);
        license.plan = plan;
        license.expires = Date.now()+DURATIONS[duration].ms;
        license.credits = value.credits;
        license.passes = {grades:0,weather:0};
        note('Proveedor asignó '+value.name+' por '+DURATIONS[duration].label+' con '+value.credits+' créditos empresariales (DEMO)');
        if (!save()) { license = JSON.parse(before); return; }
        render();
        alert('Licencia de empresa activada. Cierra sesión y entra con un usuario de la escuela para probar.');
    }
    function buyPass(module,duration) {
        if (isDeveloper()) return alert('Este módulo de pruebas corresponde a la escuela, no al proveedor.');
        const info = PREMIUM[module], d = DURATIONS[duration];
        if (!info || !d || !enabled(info.feature)) return alert('Tu licencia empresarial no incluye esta función.');
        const cost = duration==='minutos' ? 15 : duration==='horas' ? 60 : 150;
        if (license.credits<cost) return alert('La empresa no tiene créditos suficientes.');
        if (!confirm('Consumir '+cost+' créditos de la empresa para habilitar '+info.label+' por '+d.label+'?'))return;
        const before = JSON.stringify(license);
        license.credits-=cost;
        license.passes[module]=Math.max(Date.now(),license.passes[module])+d.ms;
        note('Paquete empresarial '+info.label+': '+d.label+' (-'+cost+' créditos)');
        if(!save()) {license=JSON.parse(before);return;}
        render();
        alert('Paquete de empresa activado.');
    }
    function check(module,cost) {
        const info = PREMIUM[module];
        if (!info || !currentUser || isDeveloper() || !enabled(info.feature)) {
            alert('La licencia de la escuela no autoriza esta función.'); return false;
        }
        if (module==='grades' && !['ALUMNO','PROFESOR','ADMINISTRACION'].includes(currentUser.rol)) {
            alert('Este rol no tiene permiso para exportar calificaciones.'); return false;
        }
        if (license.passes[module]>Date.now()) return true;
        if (cost!==info.cost || license.credits<cost) { alert('La empresa necesita más créditos o un paquete activo.'); return false; }
        const before=JSON.stringify(license);
        license.credits-=cost;
        note(info.label+': -'+cost+' créditos compartidos');
        if(!save()) {license=JSON.parse(before);return false;}
        render();return true;
    }
    function authorize(feature) {
        if (!enabled(feature)) {
            alert('La licencia empresarial actual no incluye '+(FEATURES[feature]||feature)+'.'); return false;
        }
        return true;
    }
    function render() {
        if (!license) return;
        const status=document.getElementById('membership-current');
        const credits=document.getElementById('membership-balance');
        if(!status||!credits)return;
        credits.textContent=license.credits+' créditos de la empresa';
        status.textContent=active()?PLANS[license.plan].name:'Sin licencia activa';
        const plans=document.getElementById('membership-plans');
        plans.replaceChildren();
        for(const [id,plan] of Object.entries(PLANS)) {
            const card=document.createElement('div');card.className='p-4 rounded-xl border border-slate-600 bg-slate-900/70 space-y-2';
            const h=document.createElement('h4');h.className='text-xl font-bold';h.textContent=plan.name;
            const p=document.createElement('p');p.className='text-xs text-slate-300';
            p.textContent=plan.features.length+' funciones incluidas · '+plan.credits+' créditos empresariales';
            const ul=document.createElement('ul');ul.className='text-xs text-slate-400 space-y-1';
            for (const feat of Object.keys(FEATURES)) {
                const li=document.createElement('li');li.textContent=(plan.features.includes(feat)?'✓ ':'✕ ')+FEATURES[feat];ul.append(li);
            }
            card.append(h,p,ul);
            if(isDeveloper()) {
                for (const [duration,d] of Object.entries(DURATIONS)) {
                    const b=document.createElement('button');
                    b.className='mr-1 mb-1 bg-indigo-600 hover:bg-indigo-500 rounded-lg px-2 py-2 text-xs';
                    b.textContent='Asignar '+d.label;
                    b.addEventListener('click',()=>apply(id,duration));card.append(b);
                }
            }
            plans.append(card);
        }
        const message=document.getElementById('company-license-summary');
        if(message) message.textContent='Empresa: '+COMPANY_ID+' · Vigencia: '+(active()?new Date(license.expires).toLocaleString('es-MX'):'vencida o sin activar')+
            ' · Plan '+PLANS[license.plan].name+' · '+(isDeveloper()?'Vista del proveedor (asignación de licencias)':'Vista de la empresa (solo consulta)');
        const list=document.getElementById('membership-packages');
        list.replaceChildren();
        if(!active()) {
            const p=document.createElement('p');p.className='text-amber-300 text-sm';p.textContent='Sin licencia activa: el proveedor debe activar un paquete empresarial.';list.append(p);
        } else if(isDeveloper()) {
            list.textContent='Los empleados de la empresa pueden activar paquetes por tiempo con el saldo compartido, solamente en los módulos incluidos en su licencia.';
        } else {
            for(const [module,info] of Object.entries(PREMIUM)) {
                if (!enabled(info.feature) || (module==='grades' && !['ALUMNO','PROFESOR','ADMINISTRACION'].includes(currentUser.rol)))continue;
                const card=document.createElement('div');card.className='p-4 rounded-xl bg-slate-900/70 border border-slate-600';
                const h=document.createElement('h4');h.className='font-semibold mb-2';h.textContent=info.label;
                const sub=document.createElement('p');sub.className='text-xs text-slate-400 mb-3';
                sub.textContent=license.passes[module]>Date.now()?'Activo hasta '+new Date(license.passes[module]).toLocaleString('es-MX'):'Sin pase temporal';
                card.append(h,sub);
                for(const [id,d] of Object.entries(DURATIONS)){
                    const cost=id==='minutos'?15:id==='horas'?60:150;
                    const b=document.createElement('button');b.className='bg-emerald-700 hover:bg-emerald-600 px-3 py-2 rounded-lg text-xs mr-2 mb-2';
                    b.textContent=d.label+' · '+cost+' créditos';
                    b.addEventListener('click',()=>buyPass(module,id));card.append(b);
                }
                list.append(card);
            }
        }
        const history=document.getElementById('membership-history');
        history.replaceChildren();
        if(!license.history.length)history.textContent='Aún no hay movimientos empresariales.';
        for(const entry of license.history.slice().reverse()) {
            const p=document.createElement('p');p.className='text-xs text-slate-300 p-2 border-b border-slate-700';
            p.textContent=entry.at+' · '+entry.text;history.append(p);
        }
        for(const [key,dom] of [['grades','grade-premium-status'],['weather','weather-premium-status']]) {
            const node=document.getElementById(dom);
            if(node)node.textContent=license.passes[key]>Date.now()?'Pase empresarial activo hasta '+new Date(license.passes[key]).toLocaleString('es-MX'):'Sin pase temporal de empresa';
        }
    }
    function login(u) {currentUser=u;license=load();render();}
    window.AccessDemo=Object.freeze({login,render,check,authorize,enabled,canOpen,isDeveloper});
})();