/* Licenciamiento empresarial B2B, simulacion academica. No es seguridad de backend. */
(function () {
    'use strict';

    const COMPANY_ID = 'escuela-demo-durango';
    const STORAGE_KEY = 'licencia_empresa_demo_v2_' + COMPANY_ID;
    const FEATURES = Object.freeze({
        dashboard:'Panel principal', students:'Directorio de alumnos', courses:'Cursos y horarios',
        grades:'Consulta de Kardex', attendance:'Control de asistencia',
        enrollment:'Inscripciones', finance:'Estado de cuenta y cobros',
        notifications:'Notificaciones', weather:'Mapa del clima',
        exportGrades:'Exportar Kardex CSV', weatherLayers:'Capas meteorologicas',
        nominas:'Servicios de Nominas', timbrar:'Timbrado CFDI'
    });
    const BRONZE = ['dashboard','students','courses','grades','attendance'];
    const SILVER = [...BRONZE,'enrollment','finance','notifications','weather','exportGrades','weatherLayers'];
    const GOLDEN = [...SILVER,'nominas','timbrar'];
    const PLANS = Object.freeze({
        bronze:{name:'Bronce',features:BRONZE},
        silver:{name:'Silver',features:SILVER},
        golden:{name:'Golden',features:GOLDEN}
    });
    // Vigencias cortas para probar el vencimiento durante la demostracion.
    const DURATIONS = Object.freeze({
        minutos:{name:'15 minutos',ms:15*60*1000},
        horas:{name:'2 horas',ms:2*60*60*1000},
        dias:{name:'7 dias',ms:7*24*60*60*1000}
    });
    let user = null;
    let license = null;

    function fresh() {
        return {company:COMPANY_ID,plan:'bronze',expires:0,history:[]};
    }
    function load() {
        const result = fresh();
        try {
            const stored = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null');
            if (stored && stored.company === COMPANY_ID) {
                if (Object.hasOwn(PLANS, stored.plan)) result.plan = stored.plan;
                if (Number.isSafeInteger(stored.expires) && stored.expires > 0) result.expires=stored.expires;
                if (Array.isArray(stored.history)) {
                    result.history=stored.history.filter(x=>x && typeof x.text==='string').slice(-40);
                }
            }
        } catch (_) {}
        return result;
    }
    function save() {
        try {
            localStorage.setItem(STORAGE_KEY,JSON.stringify(license));
            return true;
        } catch (_) {
            alert('No se pudo guardar la licencia empresarial en este navegador.');
            return false;
        }
    }
    function isDeveloper() { return user?.rol === 'DESARROLLADOR'; }
    function active() { return !!license && license.expires>Date.now(); }
    function enabled(feature) {
        return !!(active() && PLANS[license.plan].features.includes(feature));
    }
    function canOpen(tab) {
        if (!user) return false;
        if (tab==='memberships') return true;
        if (isDeveloper()) return false;
        return enabled(tab);
    }
    function authorize(feature) {
        if (!enabled(feature)) {
            alert('La licencia de la escuela no incluye ' + (FEATURES[feature] || feature) + '.');
            return false;
        }
        return true;
    }
    // Compatibilidad con el codigo existente de Kardex y Clima:
    // se permite la funcion incluida en el plan, sin cobro de creditos.
    function check(module) {
        if (!user || isDeveloper()) return false;
        const feature = module==='grades' ? 'exportGrades' : module==='weather' ? 'weatherLayers' : null;
        if (!feature || !authorize(feature)) return false;
        if (module==='grades' && !['ALUMNO','PROFESOR','ADMINISTRACION'].includes(user.rol)) {
            alert('Tu rol no autoriza la exportacion del Kardex.');
            return false;
        }
        return true;
    }
    function assign(plan,durationId) {
        if (!isDeveloper()) return alert('Solo el desarrollador puede asignar licencias a empresas.');
        const p=PLANS[plan],duration=DURATIONS[durationId];
        if (!p || !duration) return;
        if (!confirm('Asignar licencia '+p.name+' a '+COMPANY_ID+' durante '+duration.name+'? Es una simulacion sin cobro real.')) return;
        const previous = JSON.stringify(license);
        license.plan=plan;
        license.expires=Date.now()+duration.ms;
        license.history.push({
            text:'Proveedor asigno '+p.name+' por '+duration.name,
            at:new Date().toLocaleString('es-MX')
        });
        license.history=license.history.slice(-40);
        if(!save()) { license=JSON.parse(previous); return; }
        render();
        alert('Licencia '+p.name+' asignada. Cierra sesion y entra como empleado para comprobar los modulos.');
    }
    function render() {
        const planName=document.getElementById('membership-current');
        const plans=document.getElementById('membership-plans');
        if (!planName || !plans || !license) return;
        planName.textContent=active() ? PLANS[license.plan].name : 'Sin licencia activa';
        const summary=document.getElementById('company-license-summary');
        if(summary) summary.textContent='Empresa: '+COMPANY_ID+' | Vigencia: '+
            (active() ? new Date(license.expires).toLocaleString('es-MX') : 'Vencida / no activada')+
            (isDeveloper() ? ' | Vista del proveedor' : ' | Vista de la escuela');
        plans.replaceChildren();
        for (const [id,plan] of Object.entries(PLANS)) {
            const card=document.createElement('div');
            card.className='p-4 rounded-xl border border-slate-600 bg-slate-900/70 space-y-3';
            const h=document.createElement('h4');
            h.className='font-bold text-xl text-white';
            h.textContent=plan.name;
            const p=document.createElement('p');
            p.className='text-xs text-slate-300';
            p.textContent=plan.features.length+' funciones incluidas';
            const ul=document.createElement('ul');
            ul.className='text-xs text-slate-400 space-y-1';
            Object.entries(FEATURES).forEach(([feature,label])=>{
                const li=document.createElement('li');
                li.textContent=(plan.features.includes(feature)?'✓ ':'✕ ')+label;
                ul.append(li);
            });
            card.append(h,p,ul);
            if (isDeveloper()) {
                for (const [durationId,duration] of Object.entries(DURATIONS)) {
                    const btn=document.createElement('button');
                    btn.className='bg-indigo-600 hover:bg-indigo-500 rounded-lg px-3 py-2 mr-2 mb-1 text-xs font-semibold';
                    btn.textContent='Asignar '+duration.name;
                    btn.addEventListener('click',()=>assign(id,durationId));
                    card.append(btn);
                }
            }
            plans.append(card);
        }
        const history=document.getElementById('membership-history');
        if (history) {
            history.replaceChildren();
            if (!license.history.length) history.textContent='Aun no se han asignado licencias.';
            license.history.slice().reverse().forEach(entry=>{
                const p=document.createElement('p');
                p.className='text-xs text-slate-300 p-2 border-b border-slate-700';
                p.textContent=entry.at+' | '+entry.text;
                history.append(p);
            });
        }
    }
    function login(next) {
        user=next && next.id ? next : null;
        license=load();
        render();
    }
    window.AccessDemo=Object.freeze({login,render,isDeveloper,enabled,authorize,canOpen,check});
})();