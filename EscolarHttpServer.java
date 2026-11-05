import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Capa HTTP ligera para el Sistema de Control Escolar.
 * Reutiliza las clases Java existentes (AcademicService, NotificationService, FinanceService)
 * sin modificar la lógica interna ni requerir Spring Boot o Maven.
 */
public class EscolarHttpServer {

    private static final int PORT = 8081;
    private static final String NOMINAS_URL = "http://127.0.0.1:8000";

    private final AcademicService academicService;
    private final NotificationService notificationService;
    private final FinanceService financeService;
    private final HttpClient httpClient;

    public EscolarHttpServer() {
        EventBus eventBus = new EventBus();
        this.academicService = new AcademicService();
        this.notificationService = new NotificationService(eventBus);
        this.financeService = new FinanceService(eventBus);
        this.httpClient = HttpClient.newHttpClient();
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // 1. Endpoint: Consultar asignaciones académicas (para Nóminas)
        server.createContext("/api/academic/assignments", new AcademicHandler(academicService));

        // 2. Endpoint: Enviar notificación (para Nóminas)
        server.createContext("/api/notifications/send", new NotificationHandler(notificationService));

        // 3. Endpoint proxy/cliente: Consultar empleado docente en Nóminas
        server.createContext("/api/nominas/docente", new NominasDocenteHandler(httpClient));

        // 4. Endpoint proxy/cliente: Consultar contrato docente en Nóminas
        server.createContext("/api/nominas/contrato", new NominasContratoHandler(httpClient));

        // 5. Endpoint de Facturación: Timbrado CFDI Profact v2.0
        server.createContext("/api/finance/timbrar", new ProfactTimbradoHandler());

        server.setExecutor(null); // default executor
        server.start();
        System.out.println("============================================================");
        System.out.println("🚀 Servidor HTTP de Control Escolar ACTIVO en puerto " + PORT);
        System.out.println("   - GET  http://localhost:" + PORT + "/api/academic/assignments");
        System.out.println("   - POST http://localhost:" + PORT + "/api/notifications/send");
        System.out.println("   - GET  http://localhost:" + PORT + "/api/nominas/docente?query=...");
        System.out.println("   - GET  http://localhost:" + PORT + "/api/nominas/contrato?empleado_id=...");
        System.out.println("   - POST http://localhost:" + PORT + "/api/finance/timbrar");
        System.out.println("============================================================");
    }

    // ==========================================
    // HANDLERS HTTP
    // ==========================================

    /**
     * Handler para AcademicService.
     * Devuelve las asignaciones docentes en formato JSON.
     */
    static class AcademicHandler implements HttpHandler {
        private final AcademicService academicService;

        public AcademicHandler(AcademicService academicService) {
            this.academicService = academicService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\": \"Método no permitido. Use GET.\"}");
                return;
            }

            Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getQuery());
            String filtroProfesor = queryParams.get("profesor");

            Collection<Course> cursos = academicService.getAllCourses();
            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            for (Course c : cursos) {
                if (filtroProfesor != null && !filtroProfesor.isBlank()) {
                    if (!c.getProfesor().toLowerCase().contains(filtroProfesor.toLowerCase())) {
                        continue;
                    }
                }

                if (!first) json.append(",");
                first = false;

                json.append("{")
                    .append("\"curso_id\":\"").append(escapeJson(c.getCursoId())).append("\",")
                    .append("\"materia\":\"").append(escapeJson(c.getMateria())).append("\",")
                    .append("\"grupo\":\"").append(escapeJson(c.getGrupo())).append("\",")
                    .append("\"profesor\":\"").append(escapeJson(c.getProfesor())).append("\",")
                    .append("\"aula\":\"").append(escapeJson(c.getAula())).append("\",")
                    .append("\"creditos\":").append(c.getCreditos()).append(",")
                    .append("\"cupo_actual\":").append(c.getCupoActual()).append(",")
                    .append("\"cupo_maximo\":").append(c.getCupoMaximo()).append(",")
                    .append("\"horario\":[");

                boolean firstH = true;
                for (Map<String, String> h : c.getHorario()) {
                    if (!firstH) json.append(",");
                    firstH = false;
                    json.append("{")
                        .append("\"dia\":\"").append(escapeJson(h.get("dia"))).append("\",")
                        .append("\"hora_inicio\":\"").append(escapeJson(h.get("hora_inicio"))).append("\",")
                        .append("\"hora_fin\":\"").append(escapeJson(h.get("hora_fin"))).append("\"")
                        .append("}");
                }
                json.append("]}");
            }
            json.append("]");

            sendJsonResponse(exchange, 200, json.toString());
        }
    }

    /**
     * Handler para NotificationService.
     * Recibe destinatario, canal, titulo y mensaje en JSON y llama al servicio Java.
     */
    static class NotificationHandler implements HttpHandler {
        private final NotificationService notificationService;

        public NotificationHandler(NotificationService notificationService) {
            this.notificationService = notificationService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\": \"Método no permitido. Use POST.\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> data = parseSimpleJson(body);

            String destinatario = data.getOrDefault("destinatario", "");
            String canal = data.getOrDefault("canal", "EMAIL").toUpperCase();
            String titulo = data.getOrDefault("titulo", "Notificación de Nóminas");
            String mensaje = data.getOrDefault("mensaje", "");

            if (destinatario.isBlank() || mensaje.isBlank()) {
                sendJsonResponse(exchange, 400, "{\"error\": \"Faltan campos obligatorios: 'destinatario' y 'mensaje'.\"}");
                return;
            }

            notificationService.sendNotification(destinatario, canal, titulo, mensaje);

            String responseJson = "{"
                + "\"status\":\"exito\","
                + "\"mensaje\":\"Notificación procesada y enviada por Control Escolar\","
                + "\"destinatario\":\"" + escapeJson(destinatario) + "\","
                + "\"canal\":\"" + escapeJson(canal) + "\","
                + "\"titulo\":\"" + escapeJson(titulo) + "\","
                + "\"fecha\":\"" + java.time.Instant.now().toString() + "\""
                + "}";

            sendJsonResponse(exchange, 200, responseJson);
        }
    }

    /**
     * Handler consumidor: Control Escolar consulta el expediente del docente en Nóminas
     */
    static class NominasDocenteHandler implements HttpHandler {
        private final HttpClient httpClient;

        public NominasDocenteHandler(HttpClient httpClient) {
            this.httpClient = httpClient;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getQuery());
            String query = queryParams.getOrDefault("query", "");

            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(NOMINAS_URL + "/api/external/docente?query=" + java.net.URLEncoder.encode(query, StandardCharsets.UTF_8)))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                sendJsonResponse(exchange, response.statusCode(), response.body());
            } catch (Exception e) {
                sendJsonResponse(exchange, 502, "{\"error\": \"No se pudo conectar con el sistema de Nóminas: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handler consumidor: Control Escolar consulta el contrato del docente en Nóminas
     */
    static class NominasContratoHandler implements HttpHandler {
        private final HttpClient httpClient;

        public NominasContratoHandler(HttpClient httpClient) {
            this.httpClient = httpClient;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getQuery());
            String empleadoId = queryParams.getOrDefault("empleado_id", "");

            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(NOMINAS_URL + "/api/external/contrato?empleado_id=" + java.net.URLEncoder.encode(empleadoId, StandardCharsets.UTF_8)))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                sendJsonResponse(exchange, response.statusCode(), response.body());
            } catch (Exception e) {
                sendJsonResponse(exchange, 502, "{\"error\": \"No se pudo conectar con el sistema de Nóminas: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handler para timbrar CFDI mediante Profact v2.0
     */
    static class ProfactTimbradoHandler implements HttpHandler {
        private final ProfactClient profactClient = new ProfactClient();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Metodo no permitido. Use POST.\"}");
                return;
            }
            // No aceptar credenciales desde el navegador: solo variables de entorno del servidor.
            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseSimpleJson(body);
                String xmlBase64 = data.getOrDefault("xmlBase64", "");
                ProfactClient.Result result = profactClient.timbrar(xmlBase64);
                sendJsonResponse(exchange, result.httpStatus, result.body);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendJsonResponse(exchange, 503, "{\"error\":\"Solicitud interrumpida\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 502, "{\"error\":\"No fue posible comunicarse con Profact\"}");
            }
        }
    }

    // ==========================================
    // MÉTODOS DE UTILIDAD HTTP Y JSON
    // ==========================================

    private static void setCORSHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isBlank()) return map;
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                map.put(java.net.URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            } else if (pair.length == 1) {
                map.put(java.net.URLDecoder.decode(pair[0], StandardCharsets.UTF_8), "");
            }
        }
        return map;
    }

    private static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.isBlank()) return map;
        String clean = json.trim();
        if (clean.startsWith("{")) clean = clean.substring(1);
        if (clean.endsWith("}")) clean = clean.substring(0, clean.length() - 1);

        String[] tokens = clean.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String token : tokens) {
            String[] pair = token.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            if (pair.length == 2) {
                String key = pair[0].trim().replace("\"", "");
                String val = pair[1].trim().replace("\"", "");
                map.put(key, val);
            }
        }
        return map;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ==========================================
    // MÉTODO MAIN PARA INICIAR EL SERVIDOR
    // ==========================================
    public static void main(String[] args) {
        try {
            EscolarHttpServer server = new EscolarHttpServer();
            server.start();
        } catch (Exception e) {
            System.err.println("❌ Error al iniciar servidor HTTP escolar: " + e.getMessage());
            e.printStackTrace();
        }
    }
}