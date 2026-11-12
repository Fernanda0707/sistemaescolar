import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proxy de OpenWeatherMap para no exponer OPENWEATHER_API_KEY en el navegador.
 * Ubicación predeterminada: Durango, Durango, México.
 */
public final class WeatherHandler implements HttpHandler {
    private static final double LAT = 24.0277, LON = -104.6532;
    private static final String BASE = "https://api.openweathermap.org/data/2.5/weather";
    private static final Map<String, String> LAYERS = Map.of(
            "nubes", "clouds_new",
            "lluvia", "precipitation_new",
            "temperatura", "temp_new");
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8)).build();
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private static final class Cached {
        final byte[] bytes;
        final String contentType;
        final long expiresAt;
        Cached(byte[] bytes, String contentType, long expiresAt) {
            this.bytes = bytes;
            this.contentType = contentType;
            this.expiresAt = expiresAt;
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            error(exchange, 405, "Solo se permite GET");
            return;
        }
        String key = System.getenv("OPENWEATHER_API_KEY");
        if (key == null || key.isBlank()) {
            error(exchange, 503, "Configura OPENWEATHER_API_KEY en el servidor");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String upstream;
        String cacheKey;
        boolean tile = path.startsWith("/api/weather/tiles/");
        if ("/api/weather/current".equals(path)) {
            upstream = BASE + "?lat=" + LAT + "&lon=" + LON +
                    "&units=metric&lang=es&appid=" + key;
            cacheKey = "current";
        } else if (tile) {
            String[] p = path.substring("/api/weather/tiles/".length()).split("/");
            if (p.length != 4 || !LAYERS.containsKey(p[0]) || !p[3].endsWith(".png")) {
                error(exchange, 400, "Capa o coordenadas invalidas");
                return;
            }
            try {
                int z = Integer.parseInt(p[1]);
                int x = Integer.parseInt(p[2]);
                int y = Integer.parseInt(p[3].substring(0, p[3].length() - 4));
                if (z < 0 || z > 12 || x < 0 || y < 0 ||
                        x >= (1 << z) || y >= (1 << z)) throw new NumberFormatException();
                upstream = "https://tile.openweathermap.org/map/" +
                        LAYERS.get(p[0]) + "/" + z + "/" + x + "/" + y +
                        ".png?appid=" + key;
                cacheKey = path;
            } catch (NumberFormatException ex) {
                error(exchange, 400, "Coordenadas de mapa invalidas");
                return;
            }
        } else {
            error(exchange, 404, "Ruta de clima no encontrada");
            return;
        }

        Cached previous = cache.get(cacheKey);
        if (previous != null && previous.expiresAt > System.currentTimeMillis()) {
            send(exchange, 200, previous.contentType, previous.bytes);
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(upstream))
                    .timeout(Duration.ofSeconds(12)).GET().build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            if (status != 200) {
                error(exchange, status == 401 ? 502 : 503,
                        status == 401 ? "OpenWeatherMap rechazo la API key" :
                                "OpenWeatherMap no esta disponible (HTTP " + status + ")");
                return;
            }
            String type = tile ? "image/png" : "application/json; charset=utf-8";
            byte[] bytes = response.body();
            if (bytes.length > 2_000_000) {
                error(exchange, 502, "Respuesta del clima demasiado grande");
                return;
            }
            cache.put(cacheKey, new Cached(bytes, type,
                    System.currentTimeMillis() + (tile ? 600_000 : 300_000)));
            send(exchange, 200, type, bytes);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            error(exchange, 503, "Consulta interrumpida");
        } catch (Exception ex) {
            error(exchange, 502, "Error al consultar OpenWeatherMap");
        }
    }

    private static void error(HttpExchange e, int status, String message) throws IOException {
        byte[] msg = ("{\"error\":\"" + message.replace("\"", "") + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        send(e, status, "application/json; charset=utf-8", msg);
    }
    private static void send(HttpExchange e, int status, String type, byte[] data) throws IOException {
        e.getResponseHeaders().set("Content-Type", type);
        e.getResponseHeaders().set("Cache-Control", "private, max-age=120");
        e.sendResponseHeaders(status, data.length);
        try (java.io.OutputStream out = e.getResponseBody()) { out.write(data); }
    }
}
