import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/** Conteos de solicitudes HTTP reales desde el arranque del servidor Java. */
public final class ServiceMetrics {
    private static final String[] NAMES = {"escolar", "academico", "clima", "nominas"};
    private static final Map<String, LongAdder> REQUESTS = new ConcurrentHashMap<>();
    private static final Map<String, LongAdder> FAILURES = new ConcurrentHashMap<>();
    private static final Map<String, LongAdder> LATENCY_MS = new ConcurrentHashMap<>();
    private static final long STARTED_AT = System.currentTimeMillis();

    private ServiceMetrics() {}

    public static Filter forService(String name) {
        if (!isKnown(name)) throw new IllegalArgumentException("Servicio no registrado");
        return new Filter() {
            @Override public String description() { return "Contador de solicitudes " + name; }
            @Override public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
                long started = System.nanoTime();
                REQUESTS.computeIfAbsent(name, k -> new LongAdder()).increment();
                try { chain.doFilter(exchange); }
                finally {
                    int status = exchange.getResponseCode();
                    if (status >= 500 || status < 0) {
                        FAILURES.computeIfAbsent(name, k -> new LongAdder()).increment();
                    }
                    LATENCY_MS.computeIfAbsent(name, k -> new LongAdder())
                            .add(Math.max(0, (System.nanoTime() - started) / 1_000_000));
                }
            }
        };
    }

    private static boolean isKnown(String name) {
        for (String known : NAMES) if (known.equals(name)) return true;
        return false;
    }

    /** Solo localhost, sin tokens ni datos de usuarios. No expone endpoints privados. */
    public static void respond(HttpExchange exchange) throws IOException {
        if (!exchange.getRemoteAddress().getAddress().isLoopbackAddress()) {
            write(exchange, 403, "{\"error\":\"Solo consultas locales\"}");
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            write(exchange, 405, "{\"error\":\"Solo GET\"}");
            return;
        }
        StringBuilder json = new StringBuilder("{\"since_ms\":").append(STARTED_AT)
                .append(",\"services\":{");
        for (int i = 0; i < NAMES.length; i++) {
            String name = NAMES[i];
            long requests = sum(REQUESTS, name);
            if (i > 0) json.append(',');
            json.append('"').append(name).append("\":{\"requests\":").append(requests)
                .append(",\"failures\":").append(sum(FAILURES,name))
                .append(",\"avg_ms\":").append(requests == 0 ? 0 : sum(LATENCY_MS,name) / requests)
                .append('}');
        }
        json.append("}}");
        write(exchange, 200, json.toString());
    }

    private static long sum(Map<String, LongAdder> entries, String key) {
        LongAdder count = entries.get(key);
        return count == null ? 0 : count.sum();
    }
    private static void write(HttpExchange e, int status, String payload) throws IOException {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        e.getResponseHeaders().set("Cache-Control", "no-store");
        e.sendResponseHeaders(status,bytes.length);
        try (java.io.OutputStream out = e.getResponseBody()) { out.write(bytes); }
    }
}
