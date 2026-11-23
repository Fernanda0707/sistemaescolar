# Health Status Dashboard — microservicio independiente

Esta tarea agrega un servicio independiente del Sistema Escolar:

- **Servidor escolar Java:** `http://localhost:8081/`
- **Monitor independiente Python:** `http://localhost:8090/`

El monitor **no se ejecuta dentro de `index.html` ni del puerto 8081**; sirve su propia página web y API `GET /api/status`.

## Cuatro servicios supervisados

| Servicio | Endpoint de comprobación | DOWN cuando |
|---|---|---|
| Sistema Escolar | `GET http://127.0.0.1:8081/` | El servidor Java no responde |
| Académico | `GET http://127.0.0.1:8081/api/academic/assignments` | Error HTTP o timeout |
| Clima | `GET http://127.0.0.1:8081/api/weather/current` | Falla Java o OpenWeatherMap / falta API key |
| Nóminas | `GET http://127.0.0.1:8081/api/nominas/docente?query=salud` | No responde el servicio externo conectado al puerto 8000 |

**Nota:** son cuatro funcionalidades/endpoints observados, no necesariamente cuatro procesos independientes. Clima y Nóminas son proxies o dependencias desde Java. Si el servicio de Nóminas de tu compañero no está iniciado, aparecerá DOWN correctamente.

## Pasos de instalación en Windows

1. Instala Java 11+ y Python 3.9+.
2. Haz `git switch feature/health-status-microservicio` y `git pull origin feature/health-status-microservicio`.
3. En la terminal **1** (carpeta raíz del proyecto), configura tu API key de OpenWeatherMap si deseas monitorear Clima:

```powershell
$env:OPENWEATHER_API_KEY = "TU_API_KEY_PRIVADA"
javac -encoding UTF-8 Main.java ProfactClient.java WeatherHandler.java ServiceMetrics.java EscolarHttpServer.java
java EscolarHttpServer
```

4. En una **segunda terminal** PowerShell de la misma carpeta:

```powershell
python monitor_service.py
```

Si Python en Windows usa el lanzador `py`, ejecuta `py monitor_service.py` en su lugar.

5. Abre **http://localhost:8090/** para ver el dashboard independiente.
6. Para verificar el JSON: **http://localhost:8090/api/status**.
7. Para comprobar solicitudes reales contadas por Java: **http://localhost:8081/api/health/metrics** (solo localhost).

## Datos del dashboard

- Nombre, estado **UP/DOWN**, latencia en ms de cada comprobación.
- **Requests reales:** número de solicitudes HTTP observadas por Java desde el arranque, para cuatro áreas instrumentadas (web, académico, clima y proxy Nóminas).
- **Checks:** solicitudes de verificación hechas por el monitor desde su arranque.
- **Uptime observado:** porcentaje de checks exitosos desde el inicio del monitor, **no** un SLA histórico.
- **Tiempo activo:** tiempo desde que inició el microservicio de monitoreo, no necesariamente el uptime de los servicios supervisados.
- **Gráfica de latencia:** historial reciente de hasta 40 comprobaciones por servicio.
- **Gráfica de solicitudes:** conteo de requests del backend por servicio.
- Actualización automática cada 10 segundos. El estado inicial depende de las primeras verificaciones; datos y gráficas se reinician al apagar el monitor, pues están en memoria.

## Pruebas sugeridas para exponer

1. Con Java y el monitor iniciados, visualizar **Sistema Escolar = UP** y **Académico = UP**.
2. Iniciar el servidor de Nóminas del compañero para verificar que cambie a **UP**; detenerlo para comprobar **DOWN**.
3. Sin clave de OpenWeatherMap, Clima aparecerá **DOWN**; configurarla en Java y reiniciarlo para que vuelva a **UP**.
4. Repetir varias solicitudes a `http://localhost:8081/api/academic/assignments` y comprobar que el contador de requests académicos aumente.
5. Observar la gráfica de latencia cambiar después de varias actualizaciones.
6. Apagar Java, observar los servicios en **DOWN** y volver a iniciar Java; el monitor seguirá funcionando en 8090.

## Arquitectura

```text
Navegador
    |
    +--> http://localhost:8090/  (Python monitor_service.py)
                |
                +-- GET /api/status
                    |-- GET 8081/                        --> Escolar
                    |-- GET 8081/api/academic/assignments --> Académico
                    |-- GET 8081/api/weather/current      --> Clima / OpenWeatherMap
                    |-- GET 8081/api/nominas/docente      --> Nóminas:8000
                    +-- GET 8081/api/health/metrics        --> Contadores reales de Java
```

En Java se agregó `ServiceMetrics.java`, que registra cantidades de solicitudes, errores HTTP 5xx y tiempo promedio de respuesta del servidor. La ruta de métricas solo acepta conexiones desde el propio equipo y no expone credenciales.

**Seguridad y alcance:** prototipo local de observabilidad para la clase. No debes exponer el puerto 8090 a Internet sin autenticación, HTTPS y medidas contra sondeos abusivos. Las URL monitoreadas están fijas, evitando solicitar direcciones arbitrarias desde el navegador. No se realizan acciones POST, cobros ni timbrados.
