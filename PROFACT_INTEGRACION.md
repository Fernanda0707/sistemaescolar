# Integración Profact: simulación, QA y producción

Este proyecto usa Java (sin Spring) e incluye el endpoint `POST /api/finance/timbrar` en el servidor escolar (puerto 8081).

## Entornos

| PROFACT_MODE | Resultado |
|---|---|
| `mock` (predeterminado) | No realiza conexiones externas ni emite CFDI; responde `simulado: true`, `uuid: null`. |
| `qa` | Envía el XML a la API REST oficial de pruebas de Profact. |
| `production` | Envía el XML a la API REST oficial de producción, solo cuando `PROFACT_ENABLE_PRODUCTION=true`. |

**No hay claves reales en el repositorio.** Configurar variables de entorno antes de arrancar Java:

- `PROFACT_MODE`: `mock`, `qa` o `production`.
- `PROFACT_QA_TOKEN`: token de pruebas para QA.
- `PROFACT_PROD_TOKEN`: token del integrador de producción.
- `PROFACT_INTERNAL_API_KEY`: secreto privado del servidor requerido para invocar el endpoint en QA y producción.
- `PROFACT_ENABLE_PRODUCTION`: `true` para habilitar producción explícitamente; de lo contrario estará bloqueada.

### Compilar y arrancar (Java 11+)

Desde la raíz del repositorio:

```bat
javac Main.java ProfactClient.java EscolarHttpServer.java
java EscolarHttpServer
```

Para simulación, sin configurar variables, el modo predeterminado es `mock`.

Ejemplo en PowerShell para QA (usar valores privados propios):

```powershell
$env:PROFACT_MODE = "qa"
$env:PROFACT_QA_TOKEN = "REEMPLAZAR_EN_EL_EQUIPO"
$env:PROFACT_INTERNAL_API_KEY = "REEMPLAZAR_CON_SECRETO_LARGO"
java EscolarHttpServer
```

Para producción, cambiar `PROFACT_MODE` a `production`, establecer `PROFACT_PROD_TOKEN` y habilitar `PROFACT_ENABLE_PRODUCTION=true` **solo tras validar CFDI y permisos**.

### Solicitud

El endpoint local acepta JSON con un único campo `xmlBase64`; este debe ser el XML CFDI 4.0 preparado y codificado en Base64.

```json
{"xmlBase64":"BASE64_DEL_XML_CFDI"}
```

En QA y producción, añadir la cabecera `X-Internal-Api-Key` con el secreto interno configurado en el servidor. Nunca colocar esta clave en JavaScript del navegador ni en repositorios.

El cliente envía `XmlComprobanteBase64` a la API oficial mediante Bearer. Se propaga el código HTTP real de Profact. Un HTTP 200 no basta para confirmar un CFDI: la aplicación debe validar la estructura de respuesta y la existencia de UUID/XML timbrado antes de marcarlo como emitido.

### Seguridad y límites

- Simulación **no produce documentos fiscales** y devuelve `uuid: null`.
- No publicar `PROFACT_*_TOKEN`, certificados `.key`, contraseñas ni `PROFACT_INTERNAL_API_KEY`.
- El endpoint HTTP local actual **no cifra el transporte**: para entornos reales, aislarlo en red privada, protegerlo detrás de un proxy HTTPS y agregar autenticación de usuarios y autorización por rol.
- **No ejecutar producción** en entornos compartidos sin autenticación y auditoría adicionales.
- En caso de timeout, verificar el estado del CFDI con Profact **antes de reintentar**, para evitar duplicados.
- Persistencia de folios, XML, UUID, cancelaciones, estados e idempotencia quedan pendientes de integrar en base de datos; no se debe considerar el módulo un ciclo fiscal completo.

### Documentación oficial

- Timbrado: https://developers.timbracfdi33.mx/reference/timbrado_timbracfdi
- Producción: https://developers.timbracfdi33.mx/docs/pase-a-producci%C3%B3n
- Emisores y CSD: https://developers.timbracfdi33.mx/docs/registro-de-emisores-1

**Importante:** la conectividad con cuentas reales no se ha comprobado hasta disponer de credenciales de QA/producción e información fiscal del emisor.
