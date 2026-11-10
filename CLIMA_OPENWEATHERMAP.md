# Mapa del clima con OpenWeatherMap

Se añadió una pestaña **Mapa del clima** a todos los roles en `index.html`, con mapa Leaflet/OpenStreetMap y capas de OpenWeatherMap para nubes, precipitación y temperatura, más indicadores de temperatura, sensación térmica, humedad y viento de **Durango, Durango, México** (lat 24.0277, lon -104.6532).

## Obtener una API key
1. Regístrate o inicia sesión en https://home.openweathermap.org/users/sign_up
2. Consulta tu clave en https://home.openweathermap.org/api_keys y revisa las condiciones y límites del plan.
3. En PowerShell, desde la carpeta del proyecto, configura la clave **en la terminal donde arrancarás Java**:

```powershell
$env:OPENWEATHER_API_KEY = "TU_CLAVE_DE_OPENWEATHERMAP"
javac -encoding UTF-8 Main.java ProfactClient.java WeatherHandler.java EscolarHttpServer.java
java EscolarHttpServer
```

4. Abre `index.html` (idealmente con Live Server), inicia sesión y selecciona **Mapa del clima**.
5. Elige nubes, precipitación, temperatura o mapa base, y pulsa **Actualizar clima**.

## Endpoints del proyecto
- `GET http://localhost:8081/api/weather/current`: JSON de condiciones actuales.
- `GET http://localhost:8081/api/weather/tiles/nubes/6/13/28.png`: ejemplo ilustrativo de ruta de tiles; Leaflet calcula las coordenadas correctas para el mapa.

El backend Java consulta los endpoints de OpenWeatherMap:
- https://api.openweathermap.org/data/2.5/weather
- https://tile.openweathermap.org/map/{layer}/{z}/{x}/{y}.png

**La clave no se almacena en el repositorio ni en JavaScript.** El servidor hace las solicitudes por ti, valida las capas aceptadas y limita la frecuencia mediante caché (5 min datos, 10 min mapas).

## Diagnóstico
- Si aparece `Configura OPENWEATHER_API_KEY`: faltó establecer la variable en la terminal del servidor. Reinicia Java después de establecerla.
- Si aparece `OpenWeatherMap rechazo la API key`: verifica la clave y si ya fue activada.
- Si los datos cargan pero las capas no: revisa el plan de OpenWeatherMap, la consola del navegador y el backend.
- Si falla la conexión a `localhost:8081`: compila con `WeatherHandler.java` e inicia `EscolarHttpServer`.
- Se requiere conexión a Internet para Leaflet, OpenStreetMap y las consultas a OpenWeatherMap.

El mapa comienza en el centro aproximado de Durango. **No usa geolocalización GPS del usuario.**

Fuentes:
- https://openweathermap.org/api/current
- https://openweathermap.org/api/weathermaps
- https://leafletjs.com/


## Corrección del mapa base con error 403

Si aparecen mosaicos con `403 Access blocked` y la leyenda sobre la política de OpenStreetMap, **no abras `index.html` haciendo doble clic** desde el Explorador de Windows (`file://`). Esto puede causar solicitudes de mapas sin el encabezado Referer esperado por el proveedor.

Ahora el servidor Java publica la interfaz directamente en **http://localhost:8081/**. Con `OPENWEATHER_API_KEY` configurada, compila y ejecuta:

```powershell
javac -encoding UTF-8 Main.java ProfactClient.java WeatherHandler.java EscolarHttpServer.java
java EscolarHttpServer
```

Después visita **http://localhost:8081/** en Edge o Chrome. Pulsa `Ctrl+F5` y vuelve a **Mapa del clima**. La ruta es distinta de `D:\...\index.html`, lo cual es intencional. No desactives Referer, no uses trucos para eludir bloqueos ni descargues masivamente mosaicos.

Si persiste el 403, el servidor de mapas puede haber bloqueado temporalmente tu origen/red. Debe cambiarse a un proveedor de mapas base autorizado (con su propia cuenta/API key) en lugar de intentar eludir la política. El API de OpenWeatherMap que devuelve los indicadores meteorológicos es independiente del mapa base.

Política oficial: https://operations.osmfoundation.org/policies/tiles/
