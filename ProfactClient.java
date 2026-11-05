import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;

/** Cliente para Profact con simulación local, QA real y producción protegida. */
public final class ProfactClient {
    private static final String QA_URL = "https://pruebas.timbracfdi33.mx:1444/api/v2/Timbrado/TimbraCFDI";
    private static final String PROD_URL = "https://timbracfdi33.mx:1444/api/v2/Timbrado/TimbraCFDI";
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public static final class Result {
        public final int httpStatus;
        public final String body;
        Result(int httpStatus, String body) { this.httpStatus = httpStatus; this.body = body; }
    }

    public Result timbrar(String base64) throws IOException, InterruptedException {
        if (base64 == null || base64.isBlank()) {
            return new Result(400, "{\"error\":\"xmlBase64 es obligatorio\"}");
        }
        String clean = base64.replace("\r", "").replace("\n", "").trim();
        try {
            byte[] decoded = Base64.getDecoder().decode(clean);
            if (decoded.length == 0 || decoded.length > 3_000_000) {
                return new Result(400, "{\"error\":\"XML fuera del limite de 3 MB\"}");
            }
        } catch (IllegalArgumentException ex) {
            return new Result(400, "{\"error\":\"xmlBase64 no es Base64 valido\"}");
        }

        String mode = env("PROFACT_MODE", "mock").trim().toLowerCase(Locale.ROOT);
        if ("mock".equals(mode)) {
            return new Result(200, "{\"modo\":\"SIMULACION_LOCAL\",\"simulado\":true,\"timbradoReal\":false,\"uuid\":null,\"mensaje\":\"Solicitud aceptada para simulacion; no se genero CFDI\"}");
        }
        if (!"qa".equals(mode) && !"production".equals(mode)) {
            return new Result(503, "{\"error\":\"PROFACT_MODE debe ser mock, qa o production\"}");
        }
        if ("production".equals(mode) && !"true".equalsIgnoreCase(env("PROFACT_ENABLE_PRODUCTION", "false"))) {
            return new Result(503, "{\"error\":\"Produccion deshabilitada en este servidor\"}");
        }
        String token = env("production".equals(mode) ? "PROFACT_PROD_TOKEN" : "PROFACT_QA_TOKEN", "");
        if (token.isBlank()) {
            return new Result(503, "{\"error\":\"Falta token de Profact en variables del servidor\"}");
        }
        String url = "production".equals(mode) ? PROD_URL : QA_URL;
        // Campo de la documentación oficial v2.
        String json = "{\"XmlComprobanteBase64\":\"" + clean + "\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(35))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        // Nunca convertir una respuesta fallida en una factura timbrada.
        return new Result(response.statusCode(), response.body());
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }
}
