import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Cliente de prueba en Java: Sistema de Control Escolar consumiendo Nóminas.
 * Demuestra los 2 servicios consumidos de Nóminas mediante su API REST:
 *   1. EmpleadoService: Consulta de expediente y validación del docente.
 *   2. ContratoService: Consulta de contrato laboral docente y vigencia.
 */
public class TestIntegracionNominas {

    private static final String NOMINAS_URL = "http://127.0.0.1:8000";

    public static void main(String[] args) {
        String nominasUrl = (args.length > 0) ? args[0].replaceAll("/+$", "") : NOMINAS_URL;
        HttpClient client = HttpClient.newHttpClient();

        System.out.println("==================================================================");
        System.out.println(" PRUEBA DE INTEGRACIÓN: CONTROL ESCOLAR -> NÓMINAS (" + nominasUrl + ")");
        System.out.println("==================================================================");

        // 1. Consumir Servicio de Empleados (docente Carlos Gómez)
        System.out.println("\n--- [1. Consultando Expediente Docente en Nóminas (EmpleadoService)] ---");
        try {
            String query = "Carlos";
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(nominasUrl + "/api/external/docente?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8)))
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            System.out.println("Status Code: " + resp.statusCode());
            System.out.println("Respuesta JSON recibida de Nóminas:");
            System.out.println(resp.body());

        } catch (Exception e) {
            System.err.println("❌ Error al consultar EmpleadoService en Nóminas: " + e.getMessage());
        }

        // 2. Consumir Servicio de Contratos (empleado ID 1)
        System.out.println("\n--- [2. Consultando Contrato Laboral del Docente en Nóminas (ContratoService)] ---");
        try {
            int empleadoId = 1;
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(nominasUrl + "/api/external/contrato?empleado_id=" + empleadoId))
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            System.out.println("Status Code: " + resp.statusCode());
            System.out.println("Respuesta JSON recibida de Nóminas:");
            System.out.println(resp.body());

        } catch (Exception e) {
            System.err.println("❌ Error al consultar ContratoService en Nóminas: " + e.getMessage());
        }

        System.out.println("\n==================================================================");
        System.out.println(" FIN DE LA PRUEBA DE INTEGRACIÓN");
        System.out.println("==================================================================");
    }
}
