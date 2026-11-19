# Licenciamiento empresarial B2B — Sistema Escolar

## Modelo comercial

El **cliente** es una escuela o empresa que compra una licencia del desarrollador. Alumnos, profesores, administrativos y personal de finanzas **no compran membresías individuales**. Comparten el plan contratado por su institución, pero conservan restricciones RBAC por rol.

La aplicación demuestra dos reglas de autorización:

1. La licencia empresarial debe estar vigente e incluir la funcionalidad.
2. El rol del usuario también debe permitir utilizarla.

Los tres planes son:

| Funcionalidad | Bronce | Silver | Golden |
|---|:---:|:---:|:---:|
| Dashboard | ✓ | ✓ | ✓ |
| Directorio de alumnos | ✓ | ✓ | ✓ |
| Cursos y horarios | ✓ | ✓ | ✓ |
| Consulta de Kárdex | ✓ | ✓ | ✓ |
| Control de asistencia | ✓ | ✓ | ✓ |
| Inscripciones | — | ✓ | ✓ |
| Estado de cuenta / Finanzas | — | ✓ | ✓ |
| Notificaciones | — | ✓ | ✓ |
| Mapa del clima | — | ✓ | ✓ |
| Exportación CSV del Kárdex | — | ✓ | ✓ |
| Capas meteorológicas | — | ✓ | ✓ |
| Servicios de Nóminas | — | — | ✓ |
| Timbrado CFDI | — | — | ✓ |

**Silver tiene dos funciones menos que Golden:** Nóminas y Timbrado CFDI. Bronce solo ofrece lo fundamental.

## Sin créditos

El mecanismo anterior de compra y descuento de créditos fue eliminado por petición de la desarrolladora. Tampoco se compran pases adicionales para los módulos. Si el plan empresarial contratado incluye una función, se puede utilizar durante la vigencia de la licencia sin cargos internos por operación.

## Paquetes de vigencia

El proveedor puede asignar cualquiera de los tres planes por **15 minutos, 2 horas o 7 días** (duraciones cortas para demostración). Cuando vence la licencia, los módulos contratados se ocultan/bloquean; queda disponible **Licencia de la empresa** para consultar el estado.

## Pruebas de clase

1. En PowerShell, actualizar la rama `feature/solo-paquetes-empresariales`.
2. Compilar con `javac -encoding UTF-8 Main.java ProfactClient.java WeatherHandler.java EscolarHttpServer.java`.
3. Ejecutar `java EscolarHttpServer` e ingresar a `http://localhost:8081/`.
4. Iniciar sesión como **Desarrollador / Proveedor** (cuenta académica `proveedor@sistema.demo`, contraseña `proveedor2026`).
5. Abrir **Licencia de la empresa**, asignar **Bronce** durante 15 minutos. Cerrar sesión y entrar como Administración: solo deben estar los cinco módulos básicos.
6. Repetir con Silver. Administración tendrá inscripciones, finanzas, notificaciones y clima, pero no Nóminas ni Timbrado CFDI.
7. Repetir con Golden. Se habilitan Nóminas y Timbrado CFDI para los roles autorizados. Confirmar que el alumno no tenga permisos de facturación.
8. Probar la exportación CSV del Kárdex y las capas del clima en Silver y Golden: ya no descuentan créditos.
9. Esperar el vencimiento del plan de 15 minutos y verificar que la licencia se muestre como vencida.

## Limitaciones de este prototipo

Se trata de una **demostración académica**. Las licencias se guardan en `localStorage` de un solo navegador y los accesos se controlan con JavaScript. La autenticación actual es de prueba, las credenciales están en el frontend y **los endpoints de Java no verifican de forma segura la licencia**. No se debe vender ni desplegar como sistema de licencias real sin una base de datos para empresas, licencias y usuarios, sesiones seguras y comprobaciones por operación en el servidor.

Para producción, cada organización necesitaría un identificador de empresa, licencias asociadas a ella, vigencia verificable, permisos asignados por plan, facturación de la licencia y validación de roles del lado del servidor.
