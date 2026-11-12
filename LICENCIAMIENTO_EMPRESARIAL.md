# Licenciamiento empresarial B2B — Sistema Escolar

## Corrección del modelo de negocio

La escuela/empresa es **cliente del desarrollador del software**. La empresa contrata **una licencia única**, no cada alumno, profesor o empleado por separado. Los usuarios de la escuela siguen teniendo roles RBAC. Para abrir una función se requieren **las dos autorizaciones**:

`Rol del empleado permite la acción` **Y** `Licencia de la empresa permite la funcionalidad`.

La aplicación incluye un rol adicional de **DESARROLLADOR** para la demostración de ventas. No es una cuenta de la escuela. Solo este perfil asigna paquetes empresariales y vigencias.

## Catálogo de planes

| Funcionalidad | Bronce | Silver | Golden |
|---|:---:|:---:|:---:|
| Dashboard | ✓ | ✓ | ✓ |
| Directorio de alumnos | ✓ | ✓ | ✓ |
| Cursos y horarios | ✓ | ✓ | ✓ |
| Consultar Kárdex | ✓ | ✓ | ✓ |
| Control de asistencia | ✓ | ✓ | ✓ |
| Inscripciones | — | ✓ | ✓ |
| Finanzas / Estado de cuenta | — | ✓ | ✓ |
| Notificaciones | — | ✓ | ✓ |
| Mapa del clima | — | ✓ | ✓ |
| Exportación CSV de Kárdex | — | ✓ | ✓ |
| Capas meteorológicas avanzadas | — | ✓ | ✓ |
| Servicios de Nóminas API | — | — | ✓ |
| Timbrado CFDI | — | — | ✓ |

**Silver tiene exactamente 2 funcionalidades menos que Golden:** Nóminas API y Timbrado CFDI. Bronce tiene solo 5 funciones esenciales.

Los roles internos continúan restringiendo acceso. Por ejemplo, tener Golden **no autoriza** a un alumno a timbrar CFDI ni a entrar a Nóminas.

## Vigencias de la licencia empresarial

El vendedor/desarrollador puede demostrar la venta de cualquiera de los tres planes por:
- 15 minutos
- 2 horas
- 7 días

Pasado el vencimiento de licencia, los módulos del plan quedan deshabilitados; queda disponible la pantalla de consulta de licencia para indicar que debe renovarse.

La simulación asigna **una bolsa de créditos a la empresa** al activar la licencia: Bronce 0, Silver 120, Golden 350. Los usuarios no compran una membresía personal. En Silver/Golden, la empresa puede usar los créditos compartidos para funciones adicionales en dos módulos:

- Kárdex: exportar CSV cuesta 8 créditos por acción, o un pase de 15 min, 2 horas o 7 días (15/60/150 créditos).
- Clima: activar una capa de mapa cuesta 4 créditos por acción, o el mismo tipo de pase de tiempo (15/60/150 créditos).

Los pases son **por empresa y por módulo**, no por empleado. En Bronce estas funciones no están incluidas y no se habilitan por tener créditos.

## Cómo demostrarlo en la computadora

1. Actualizar rama `feature/licencias-empresariales-bronce-silver-golden`.
2. En PowerShell (opcional, para mapa de clima): `$env:OPENWEATHER_API_KEY = "TU_CLAVE_PRIVADA"`.
3. Compilar `javac -encoding UTF-8 Main.java ProfactClient.java WeatherHandler.java EscolarHttpServer.java` e iniciar `java EscolarHttpServer`.
4. Abrir **http://localhost:8081/**.
5. Desde la pantalla de login, seleccionar **Desarrollador / Proveedor del software (DEMO)**. Las credenciales de prueba son `proveedor@sistema.demo` / `proveedor2026`.
6. Entrar a **Licencia de la empresa**, elegir **Bronce** y pulsar **Asignar 15 minutos**. Cerrar sesión.
7. Entrar como Administración: verás solamente los módulos básicos; inscripciones, finanzas, clima y notificaciones quedarán ocultos.
8. Cerrar sesión, entrar como Desarrollador y asignar **Silver** por 2 horas. Volver a iniciar sesión como Administración. Se habilitarán inscripciones, finanzas, clima, exportación, etc. Nóminas y Timbrar CFDI seguirán sin mostrarse.
9. Repetir con **Golden** por 7 días. Administración verá Nóminas y el botón de Timbrar CFDI (cuando corresponda a un cargo pagado). Probar acceso a exportación CSV y capas del clima consumiendo la bolsa de créditos corporativos.
10. Probar una cuenta de Alumno con Golden: no verá Nóminas ni podrá timbrar CFDI por restricciones RBAC.

## Naturaleza DEMO y limitaciones importantes

Todo este módulo es una **maqueta académica**, no un producto comercial seguro. Los planes, roles, saldos, expiraciones y compras se almacenan en `localStorage` y se ejecutan en JavaScript del navegador. Otros navegadores no compartirán el estado, y alguien podría manipularlo en las herramientas de desarrollo. Los endpoints del backend **no están protegidos por una autorización empresarial verificable**. Esto significa que la limitación del menú y las validaciones actuales **no son control de acceso suficiente para producción**.

Para vender realmente el sistema a una escuela se requiere:
- Tabla `empresas` o tenants y cuentas de usuario asociadas a empresa.
- Tabla `licencias` vinculada a `empresa_id` con plan, estado, fecha de inicio y vencimiento.
- Catálogo de `funcionalidades` y tabla de permisos de cada plan.
- Saldo corporativo en una BD y transacciones atómicas para no gastar el mismo crédito simultáneamente.
- API de autorización **en el servidor** para cada endpoint, con roles y licencias verificadas.
- Proceso de compra/renovación validado por el proveedor de pagos, sin confiar en datos del cliente.
- Control de sesiones seguro, logs de auditoría, vencimientos y facturación de licencias.

## Aclaración de planos de negocio

La facturación **Profact CFDI** corresponde a las operaciones escolares, y solo se habilita como una función del plan Golden. La venta de licencias empresariales es un proceso distinto, gestionado por el proveedor del sistema.
