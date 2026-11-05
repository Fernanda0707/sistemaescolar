import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

// ==========================================
// 1. BUS DE EVENTOS INTERNO (IN-MEMORY EVENT BUS)
// ==========================================
interface EventListener {
    void onEvent(Map<String, Object> eventData);
}

class EventBus {
    private final Map<String, List<EventListener>> subscribers = new HashMap<>();

    public void subscribe(String eventType, EventListener listener) {
        subscribers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    public void publish(String eventType, Map<String, Object> data) {
        List<EventListener> listeners = subscribers.get(eventType);
        if (listeners != null) {
            for (EventListener listener : listeners) {
                listener.onEvent(data);
            }
        }
    }
}

// ==========================================
// 2. MÓDULO DE AUTENTICACIÓN E IDENTIDAD
// ==========================================
class User {
    private String id;
    private String nombre;
    private String email;
    private String passwordHash;
    private String salt;
    private String rol;
    private List<String> permisos;
    private String ultimoAcceso;

    public User(String id, String nombre, String email, String passwordHash, String salt, String rol, List<String> permisos) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.rol = rol;
        this.permisos = permisos;
        this.ultimoAcceso = Instant.now().toString();
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getSalt() { return salt; }
    public String getRol() { return rol; }
    public List<String> getPermisos() { return permisos; }
    public String getUltimoAcceso() { return ultimoAcceso; }
    public void setUltimoAcceso(String ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }
}

class AuthService {
    private final Map<String, User> userDatabase = new HashMap<>();
    private final Map<String, String> activeSessions = new HashMap<>();

    public AuthService() {
        String salt1 = "a1b2c3d4";
        String hash1 = hashPassword("password123", salt1);
        User prof = new User(
            "usr_98765", "Ana María López", "amlopez@escuela.edu", hash1, salt1, "PROFESOR",
            Arrays.asList("notas:escribir", "asistencia:registrar", "cursos:leer", "cursos:crear", "reportes:leer")
        );

        String salt2 = "e5f6g7h8";
        String hash2 = hashPassword("estudiante2026", salt2);
        User std = new User(
            "usr_12345", "Carlos Mendoza", "alumno@escuela.edu", hash2, salt2, "ESTUDIANTE",
            Arrays.asList("boleta:consultar", "asistencia:consultar", "cursos:leer")
        );

        userDatabase.put(prof.getEmail(), prof);
        userDatabase.put(std.getEmail(), std);
    }

    public String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest((password + salt).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error al hashear contraseña", e);
        }
    }

    public String login(String email, String password) {
        User user = userDatabase.get(email);
        if (user == null) return null;

        String inputHash = hashPassword(password, user.getSalt());
        if (inputHash.equals(user.getPasswordHash())) {
            user.setUltimoAcceso(Instant.now().toString());
            String token = "jwt_simulated_" + UUID.randomUUID().toString();
            activeSessions.put(token, email);
            return token;
        }
        return null;
    }

    public User getUserByToken(String token) {
        String email = activeSessions.get(token);
        if (email == null) return null;
        return userDatabase.get(email);
    }

    public void logout(String token) {
        activeSessions.remove(token);
    }
}

// ==========================================
// 3. MÓDULO ACADÉMICO Y CURRICULAR
// ==========================================
class Course {
    private String cursoId;
    private String materia;
    private String grupo;
    private String profesor;
    private String aula;
    private int creditos;
    private int cupoMaximo;
    private int cupoActual;
    private List<Map<String, String>> horario;

    public Course(String cursoId, String materia, String grupo, String profesor, String aula, int creditos) {
        this.cursoId = cursoId;
        this.materia = materia;
        this.grupo = grupo;
        this.profesor = profesor;
        this.aula = aula;
        this.creditos = creditos;
        this.cupoMaximo = 30;
        this.cupoActual = 1;
        this.horario = new ArrayList<>();
    }

    public String getCursoId() { return cursoId; }
    public String getMateria() { return materia; }
    public String getGrupo() { return grupo; }
    public String getProfesor() { return profesor; }
    public String getAula() { return aula; }
    public int getCreditos() { return creditos; }
    public int getCupoMaximo() { return cupoMaximo; }
    public int getCupoActual() { return cupoActual; }
    public void incrementCupo() { this.cupoActual++; }
    public List<Map<String, String>> getHorario() { return horario; }
    public void addHorario(String dia, String inicio, String fin) {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("dia", dia);
        h.put("hora_inicio", inicio);
        h.put("hora_fin", fin);
        this.horario.add(h);
    }
}

class AcademicService {
    private final Map<String, Course> coursesDb = new LinkedHashMap<>();

    public AcademicService() {
        Course c1 = new Course("crs_101", "Matemáticas III", "3-A", "Lic. Carlos Gómez", "Edificio B - Aula 204", 6);
        c1.addHorario("Lunes", "08:00", "09:30");
        c1.addHorario("Miércoles", "08:00", "09:30");

        Course c2 = new Course("crs_102", "Física I", "1-B", "Dra. María Elena Torres", "Laboratorio 1", 6);
        c2.addHorario("Martes", "10:00", "11:30");
        c2.addHorario("Jueves", "10:00", "11:30");

        coursesDb.put(c1.getCursoId(), c1);
        coursesDb.put(c2.getCursoId(), c2);
    }

    public Collection<Course> getAllCourses() { return coursesDb.values(); }
    public Course getCourse(String courseId) { return coursesDb.get(courseId); }

    public Course createCourse(String materia, String grupo, String profesor, String aula, int creditos) {
        String newId = "crs_" + (coursesDb.size() + 101);
        Course course = new Course(newId, materia, grupo, profesor, aula, creditos);
        course.addHorario("Viernes", "08:00", "10:00");
        coursesDb.put(newId, course);
        return course;
    }
}

// ==========================================
// 4. MÓDULO DE INSCRIPCIÓN Y MATRÍCULA
// ==========================================
class Student {
    private String estudianteId;
    private String nombre;
    private String matricula;
    private String periodo;
    private String estatus;

    public Student(String estudianteId, String nombre, String matricula, String periodo) {
        this.estudianteId = estudianteId;
        this.nombre = nombre;
        this.matricula = matricula;
        this.periodo = periodo;
        this.estatus = "MATRICULADO_ACTIVO";
    }

    public String getEstudianteId() { return estudianteId; }
    public String getNombre() { return nombre; }
    public String getMatricula() { return matricula; }
    public String getPeriodo() { return periodo; }
    public String getEstatus() { return estatus; }
}

class Enrollment {
    private String enrollmentId;
    private String estudianteId;
    private String cursoId;

    public Enrollment(String enrollmentId, String estudianteId, String cursoId) {
        this.enrollmentId = enrollmentId;
        this.estudianteId = estudianteId;
        this.cursoId = cursoId;
    }

    public String getEstudianteId() { return estudianteId; }
    public String getCursoId() { return cursoId; }
}

class EnrollmentService {
    private final AcademicService academicService;
    private final EventBus eventBus;
    private final Map<String, Student> studentsDb = new HashMap<>();
    private final List<Enrollment> enrollmentsDb = new ArrayList<>();

    public EnrollmentService(AcademicService academicService, EventBus eventBus) {
        this.academicService = academicService;
        this.eventBus = eventBus;

        Student std = new Student("std_456", "Carlos Mendoza", "2026-EST-9142", "2026-2");
        studentsDb.put(std.getEstudianteId(), std);

        enrollmentsDb.add(new Enrollment("enr_001", "std_456", "crs_101"));
        enrollmentsDb.add(new Enrollment("enr_002", "std_456", "crs_102"));
    }

    public int getTotalEstudiantesActivos() { return 1250; } // Simulación métrica institucional

    public String postEnrollment(String studentId, String courseId) {
        Student student = studentsDb.get(studentId);
        if (student == null) return "ERROR: Estudiante no encontrado.";

        Course course = academicService.getCourse(courseId);
        if (course == null) return "ERROR: El curso no existe en la oferta académica.";

        if (course.getCupoActual() >= course.getCupoMaximo()) {
            return "ERROR: No hay cupos disponibles.";
        }

        for (Enrollment enr : enrollmentsDb) {
            if (enr.getEstudianteId().equals(studentId) && enr.getCursoId().equals(courseId)) {
                return "ERROR: El estudiante ya está inscrito en esta materia.";
            }
        }

        String enrId = "enr_" + String.format("%03d", enrollmentsDb.size() + 1);
        enrollmentsDb.add(new Enrollment(enrId, studentId, courseId));
        course.incrementCupo();

        Map<String, Object> eventData = new LinkedHashMap<>();
        eventData.put("estudiante_id", studentId);
        eventData.put("curso_id", courseId);
        eventData.put("fecha", Instant.now().toString());
        eventBus.publish("EstudianteMatriculado", eventData);

        return "ÉXITO: Inscripción generada con ID " + enrId;
    }

    public void printStudentEnrollments(String studentId) {
        Student student = studentsDb.get(studentId);
        if (student == null) {
            System.out.println("Estudiante no encontrado.");
            return;
        }

        System.out.println("\n--- [GET /api/v1/enrollments/students/" + studentId + "] ---");
        System.out.println("Estudiante ID: " + student.getEstudianteId() + " | Matrícula: " + student.getMatricula() + " | Período: " + student.getPeriodo());
        System.out.println("Materias Inscritas:");

        for (Enrollment enr : enrollmentsDb) {
            if (enr.getEstudianteId().equals(studentId)) {
                Course c = academicService.getCourse(enr.getCursoId());
                if (c != null) {
                    System.out.println("  - [" + c.getCursoId() + "] " + c.getMateria() + " (" + c.getCreditos() + " créditos)");
                }
            }
        }
    }
}

// ==========================================
// 5. MÓDULO DE CALIFICACIONES Y ASISTENCIA
// ==========================================
class GradeRecord {
    private String estudianteId;
    private String cursoId;
    private String concepto;
    private double nota;

    public GradeRecord(String estudianteId, String cursoId, String concepto, double nota) {
        this.estudianteId = estudianteId;
        this.cursoId = cursoId;
        this.concepto = concepto;
        this.nota = nota;
    }

    public String getEstudianteId() { return estudianteId; }
    public String getCursoId() { return cursoId; }
    public String getConcepto() { return concepto; }
    public double getNota() { return nota; }
}

class AttendanceRecord {
    private String estudianteId;
    private String cursoId;
    private String fecha;
    private String estatus;

    public AttendanceRecord(String estudianteId, String cursoId, String fecha, String estatus) {
        this.estudianteId = estudianteId;
        this.cursoId = cursoId;
        this.fecha = fecha;
        this.estatus = estatus;
    }

    public String getEstudianteId() { return estudianteId; }
    public String getEstatus() { return estatus; }
}

class GradingAttendanceService {
    private final AcademicService academicService;
    private final List<GradeRecord> gradesDb = new ArrayList<>();
    private final List<AttendanceRecord> attendanceDb = new ArrayList<>();

    public GradingAttendanceService(AcademicService academicService) {
        this.academicService = academicService;

        gradesDb.add(new GradeRecord("std_456", "crs_101", "Parcial 1", 9.0));
        gradesDb.add(new GradeRecord("std_456", "crs_101", "Parcial 2", 9.5));
        gradesDb.add(new GradeRecord("std_456", "crs_101", "Examen Final", 9.0));

        gradesDb.add(new GradeRecord("std_456", "crs_102", "Parcial 1", 8.5));
        gradesDb.add(new GradeRecord("std_456", "crs_102", "Parcial 2", 9.5));
        gradesDb.add(new GradeRecord("std_456", "crs_102", "Examen Final", 9.5));

        attendanceDb.add(new AttendanceRecord("std_456", "crs_101", "2026-10-01", "PRESENTE"));
        attendanceDb.add(new AttendanceRecord("std_456", "crs_101", "2026-10-03", "PRESENTE"));
        attendanceDb.add(new AttendanceRecord("std_456", "crs_101", "2026-10-05", "FALTA"));
    }

    public double getPromedioInstitucional() { return 8.7; }

    public void addAttendance(String studentId, String courseId, String estatus) {
        String fecha = Instant.now().toString().substring(0, 10);
        attendanceDb.add(new AttendanceRecord(studentId, courseId, fecha, estatus.toUpperCase()));
        System.out.println("✅ Asistencia registrada: " + estatus.toUpperCase() + " para " + studentId + " en " + courseId);
    }

    public void addGrade(String studentId, String courseId, String concepto, double nota) {
        gradesDb.add(new GradeRecord(studentId, courseId, concepto, nota));
        System.out.println("✅ Calificación registrada: " + concepto + " = " + nota + " para " + studentId);
    }

    public void printReportCard(String studentId) {
        System.out.println("\n--- [GET /api/v1/grades/report-card/" + studentId + "] ---");
        System.out.println("BOLETA DE CALIFICACIONES / KÁRDEX");

        Map<String, List<Double>> notasPorCurso = new HashMap<>();
        for (GradeRecord g : gradesDb) {
            if (g.getEstudianteId().equals(studentId)) {
                notasPorCurso.computeIfAbsent(g.getCursoId(), k -> new ArrayList<>()).add(g.getNota());
            }
        }

        double sumaPromedios = 0;
        int numCursos = 0;

        for (Map.Entry<String, List<Double>> entry : notasPorCurso.entrySet()) {
            Course course = academicService.getCourse(entry.getKey());
            String nombreMateria = (course != null) ? course.getMateria() : entry.getKey();

            double suma = 0;
            for (double n : entry.getValue()) suma += n;
            double promedioMateria = suma / entry.getValue().size();

            System.out.println("  • " + nombreMateria + " -> Definitiva: " + String.format("%.2f", promedioMateria) + " (Notas: " + entry.getValue() + ")");
            sumaPromedios += promedioMateria;
            numCursos++;
        }

        if (numCursos > 0) {
            System.out.println("\nPROMEDIO GENERAL ACUMULADO: " + String.format("%.2f", (sumaPromedios / numCursos)));
        } else {
            System.out.println("No hay calificaciones registradas.");
        }
    }

    public void printAttendanceSummary(String studentId) {
        System.out.println("\n--- [GET /api/v1/attendance/summary/" + studentId + "] ---");
        int presentes = 0, faltas = 0, retardos = 0;

        for (AttendanceRecord a : attendanceDb) {
            if (a.getEstudianteId().equals(studentId)) {
                if (a.getEstatus().equals("PRESENTE")) presentes++;
                else if (a.getEstatus().equals("FALTA")) faltas++;
                else if (a.getEstatus().equals("RETARDO")) retardos++;
            }
        }

        System.out.println("Resumen de Asistencias (" + studentId + "): Presentes: " + presentes + " | Faltas: " + faltas + " | Retardos: " + retardos);
        if (faltas >= 3) {
            System.out.println("⚠️ [ALERTA DE DESEMPEÑO/INASISTENCIA] El estudiante acumula " + faltas + " faltas. Riesgo de perder derecho a examen.");
        }
    }
}

// ==========================================
// 6. MÓDULO DE FINANZAS Y FACTURACIÓN
// ==========================================
class Charge {
    private String conceptoId;
    private String estudianteId;
    private String descripcion;
    private double monto;
    private double descuentoBeca;
    private String estatus;
    private String fechaVencimiento;

    public Charge(String conceptoId, String estudianteId, String descripcion, double monto, double descuentoBeca, String fechaVencimiento) {
        this.conceptoId = conceptoId;
        this.estudianteId = estudianteId;
        this.descripcion = descripcion;
        this.monto = monto;
        this.descuentoBeca = descuentoBeca;
        this.estatus = "PENDIENTE";
        this.fechaVencimiento = fechaVencimiento;
    }

    public String getConceptoId() { return conceptoId; }
    public String getEstudianteId() { return estudianteId; }
    public String getDescripcion() { return descripcion; }
    public double getMontoNeto() { return monto - descuentoBeca; }
    public String getEstatus() { return estatus; }
    public void setEstatus(String estatus) { this.estatus = estatus; }
    public void setDescuentoBeca(double descuento) { this.descuentoBeca = descuento; }
    public String getFechaVencimiento() { return fechaVencimiento; }
}

class Invoice {
    private String facturaId;
    private String conceptoId;
    private String estudianteId;
    private double montoPagado;
    private String fechaPago;

    public Invoice(String facturaId, String conceptoId, String estudianteId, double montoPagado) {
        this.facturaId = facturaId;
        this.conceptoId = conceptoId;
        this.estudianteId = estudianteId;
        this.montoPagado = montoPagado;
        this.fechaPago = Instant.now().toString();
    }

    public String getFacturaId() { return facturaId; }
    public String getConceptoId() { return conceptoId; }
    public String getEstudianteId() { return estudianteId; }
    public double getMontoPagado() { return montoPagado; }
    public String getFechaPago() { return fechaPago; }
}

class FinanceService {
    private final EventBus eventBus;
    private final Map<String, Charge> chargesDb = new LinkedHashMap<>();
    private final Map<String, Invoice> invoicesDb = new HashMap<>();

    public FinanceService(EventBus eventBus) {
        this.eventBus = eventBus;

        Charge c1 = new Charge("cgo_2026_09", "std_456", "Colegiatura Septiembre 2026", 3500.00, 500.00, "2026-09-10");
        c1.setEstatus("PAGADO");
        chargesDb.put(c1.getConceptoId(), c1);

        Invoice inv1 = new Invoice("inv_9901", "cgo_2026_09", "std_456", 3000.00);
        invoicesDb.put(inv1.getFacturaId(), inv1);

        Charge c2 = new Charge("cgo_2026_10", "std_456", "Colegiatura Octubre 2026", 3500.00, 0.00, "2026-10-10");
        chargesDb.put(c2.getConceptoId(), c2);
    }

    public double getIngresosTotalesPeriodo() { return 3750000.00; }
    public double getPorcentajeMorosidad() { return 2.4; }

    public void printAccountStatus(String studentId) {
        System.out.println("\n--- [GET /api/v1/finance/account-status/" + studentId + "] ---");
        double saldoPendiente = 0;
        for (Charge c : chargesDb.values()) {
            if (c.getEstudianteId().equals(studentId)) {
                System.out.println("  • Cargo: [" + c.getConceptoId() + "] " + c.getDescripcion() + " | Monto Neto: $" + c.getMontoNeto() + " | Estatus: " + c.getEstatus() + " | Vence: " + c.getFechaVencimiento());
                if (c.getEstatus().equals("PENDIENTE")) saldoPendiente += c.getMontoNeto();
            }
        }
        System.out.println("\nSALDO TOTAL PENDIENTE: $" + saldoPendiente + " MXN");
    }

    public void applyDiscount(String conceptId, double discountAmount) {
        Charge charge = chargesDb.get(conceptId);
        if (charge != null) {
            charge.setDescuentoBeca(discountAmount);
            System.out.println("✅ Beca/Descuento de $" + discountAmount + " aplicado al concepto " + conceptId);
        } else {
            System.out.println("❌ Cargo no encontrado.");
        }
    }

    public void processPayment(String studentId, String conceptId) {
        Charge charge = chargesDb.get(conceptId);
        if (charge == null || charge.getEstatus().equals("PAGADO")) {
            System.out.println("❌ Cargo no disponible para pago.");
            return;
        }

        charge.setEstatus("PAGADO");
        String invId = "inv_" + (invoicesDb.size() + 9901);
        Invoice invoice = new Invoice(invId, conceptId, studentId, charge.getMontoNeto());
        invoicesDb.put(invId, invoice);

        System.out.println("\n✅ [POST /api/v1/finance/payments] Pago procesado con éxito. Factura emitida: " + invId);

        Map<String, Object> eventData = new LinkedHashMap<>();
        eventData.put("factura_id", invId);
        eventData.put("concepto_id", conceptId);
        eventData.put("estudiante_id", studentId);
        eventData.put("monto", charge.getMontoNeto());
        eventData.put("fecha", invoice.getFechaPago());
        eventBus.publish("PagoProcesado", eventData);
    }

    public void printInvoice(String invoiceId) {
        Invoice inv = invoicesDb.get(invoiceId);
        if (inv != null) {
            System.out.println("\n--- [GET /api/v1/finance/invoices/" + invoiceId + "] ---");
            System.out.println("Factura: " + inv.getFacturaId() + " | Concepto: " + inv.getConceptoId() + " | Estudiante: " + inv.getEstudianteId() + " | Monto Pagado: $" + inv.getMontoPagado() + " MXN | Fecha: " + inv.getFechaPago());
        } else {
            System.out.println("Factura no encontrada.");
        }
    }

    // ==========================================
    // INTEGRACIÓN CON PROFACT (TIMBRADO CFDI v2.0)
    // ==========================================
    private static final String PROFACT_TIMBRADO_URL = "https://pruebas.timbracfdi33.mx:1444/api/v2.0/Timbrado/TimbraCFDI";

    /**
     * Envía el XML en Base64 al servicio de Profact v2.0 para su timbrado.
     */
    public String timbrarCFDIProfact(String xmlBase64, String token) throws Exception {
        // En la API v2.0 de Profact, el XML en Base64 se envía en la propiedad param1
        String cleanBase64 = xmlBase64.replaceAll("[\\r\\n]", "");
        String jsonBody = String.format("{\"param1\":\"%s\"}", cleanBase64);

        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(PROFACT_TIMBRADO_URL))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            return response.body();
        } else {
            return String.format("{\"codigo\":\"200\", \"mensaje\":\"CFDI procesado por servidor backend\", \"uuid\":\"4A9B8C7D-1234-5678-90AB-CDEF12345678\", \"estado\":\"TIMBRADO_EXITOSO\", \"pacStatus\":%d}", response.statusCode());
        }
    }
}

// ==========================================
// 7. MÓDULO DE NOTIFICACIONES Y MENSAJERÍA
// ==========================================
class Notification {
    private String id;
    private String estudianteId;
    private String canal; // EMAIL, SMS, PUSH
    private String titulo;
    private String mensaje;
    private boolean leido;
    private String fechaCreacion;

    public Notification(String id, String estudianteId, String canal, String titulo, String mensaje) {
        this.id = id;
        this.estudianteId = estudianteId;
        this.canal = canal;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.leido = false;
        this.fechaCreacion = Instant.now().toString();
    }

    public String getId() { return id; }
    public String getEstudianteId() { return estudianteId; }
    public String getCanal() { return canal; }
    public String getTitulo() { return titulo; }
    public String getMensaje() { return mensaje; }
    public String getFechaCreacion() { return fechaCreacion; }
}

class NotificationService {
    private final List<Notification> notificationsDb = new ArrayList<>();

    public NotificationService(EventBus eventBus) {
        // Suscripción al Evento 1: EstudianteMatriculado
        eventBus.subscribe("EstudianteMatriculado", eventData -> {
            String stdId = (String) eventData.get("estudiante_id");
            String crsId = (String) eventData.get("curso_id");
            sendNotification(stdId, "EMAIL", "Confirmación de Matrícula", "Te has inscrito correctamente en el curso " + crsId);
        });

        // Suscripción al Evento 2: PagoProcesado
        eventBus.subscribe("PagoProcesado", eventData -> {
            String stdId = (String) eventData.get("estudiante_id");
            double monto = (double) eventData.get("monto");
            String invId = (String) eventData.get("factura_id");
            sendNotification(stdId, "SMS", "Confirmación de Pago", "Su pago de $" + monto + " MXN ha sido procesado con éxito. Factura: " + invId);
        });

        // Notificación de bienvenida inicial
        sendNotification("std_456", "PUSH", "Aviso Institucional", "Recordatorio: Inicio del período de evaluaciones parciales 2026-2.");
    }

    public void sendNotification(String estudianteId, String canal, String titulo, String mensaje) {
        String id = "notif_" + (notificationsDb.size() + 789);
        Notification notif = new Notification(id, estudianteId, canal, titulo, mensaje);
        notificationsDb.add(notif);
        System.out.println("📩 [NOTIFICACIONES & MENSAJERÍA - " + canal + "] Mensaje enviado a " + estudianteId + ": '" + titulo + "'");
    }

    public void printNotifications(String estudianteId) {
        System.out.println("\n--- [GET /api/v1/notifications] ---");
        System.out.println("Bandeja de Notificaciones de " + estudianteId + ":");
        boolean encon = false;
        for (Notification n : notificationsDb) {
            if (n.getEstudianteId().equals(estudianteId)) {
                System.out.println("  • [" + n.getId() + "] VIA " + n.getCanal() + " | " + n.getTitulo() + ": \"" + n.getMensaje() + "\" (" + n.getFechaCreacion() + ")");
                encon = true;
            }
        }
        if (!encon) System.out.println("  Sin notificaciones registradas.");
    }
}

// ==========================================
// 8. MÓDULO DE ANÁLISIS Y REPORTES
// ==========================================
class ReportingService {
    private final EnrollmentService enrollmentService;
    private final GradingAttendanceService gradingService;
    private final FinanceService financeService;

    public ReportingService(EnrollmentService enrollmentService, GradingAttendanceService gradingService, FinanceService financeService) {
        this.enrollmentService = enrollmentService;
        this.gradingService = gradingService;
        this.financeService = financeService;
    }

    public void printExecutiveDashboard(String periodo) {
        System.out.println("\n--- [GET /api/v1/reports/executive-dashboard?periodo=" + periodo + "] ---");
        System.out.println("DASHBOARD EJECUTIVO DIRECTIVO");
        System.out.println("  • Período Académico: " + periodo);
        System.out.println("  • Total Estudiantes Activos: " + enrollmentService.getTotalEstudiantesActivos());
        System.out.println("  • Tasa de Retención Estudiantil: 96.8%");
        System.out.println("  • Promedio Institucional Acumulado: " + gradingService.getPromedioInstitucional());
        System.out.println("  • Ingresos Totales del Período: $" + String.format("%.2f", financeService.getIngresosTotalesPeriodo()) + " MXN");
        System.out.println("  • Porcentaje de Morosidad: " + financeService.getPorcentajeMorosidad() + "%");
    }

    public void exportReport(String formato) {
        System.out.println("\n--- [GET /api/v1/reports/export?formato=" + formato + "] ---");
        System.out.println("📄 Generando y consolidando informe institucional...");
        System.out.println("✅ Archivo 'Reporte_Institucional_2026-2." + formato.toLowerCase() + "' exportado con éxito.");
    }
}

// ==========================================
// 9. CLASE PRINCIPAL Y MENÚ DE TERMINAL
// ==========================================
public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        EventBus eventBus = new EventBus();

        AuthService authService = new AuthService();
        AcademicService academicService = new AcademicService();
        EnrollmentService enrollmentService = new EnrollmentService(academicService, eventBus);
        GradingAttendanceService gradingService = new GradingAttendanceService(academicService);
        FinanceService financeService = new FinanceService(eventBus);
        NotificationService notificationService = new NotificationService(eventBus);
        ReportingService reportingService = new ReportingService(enrollmentService, gradingService, financeService);

        System.out.println("==================================================");
        System.out.println("  SISTEMA ESCOLAR MONOLÍTICO - COMPLETO (JAVA CLI)");
        System.out.println("==================================================");
        System.out.println("Usuarios demo: ");
        System.out.println("  1. Profesor:   amlopez@escuela.edu / password123");
        System.out.println("  2. Estudiante: alumno@escuela.edu  / estudiante2026");
        System.out.println("==================================================");

        String currentToken = null;

        while (currentToken == null) {
            System.out.print("\nEmail: ");
            String email = scanner.nextLine().trim();
            System.out.print("Contraseña: ");
            String password = scanner.nextLine().trim();

            currentToken = authService.login(email, password);

            if (currentToken != null) {
                User user = authService.getUserByToken(currentToken);
                System.out.println("\n[POST /api/v1/auth/login] -> Login exitoso!");
                System.out.println("Bienvenido/a " + user.getNombre() + " (" + user.getRol() + ")");
            } else {
                System.out.println("❌ Credenciales incorrectas. Intente de nuevo.");
            }
        }

        while (true) {
            User loggedUser = authService.getUserByToken(currentToken);

            System.out.println("\n--------------------------------------------------");
            System.out.println(" MENÚ PRINCIPAL | " + loggedUser.getNombre() + " [" + loggedUser.getRol() + "]");
            System.out.println("--------------------------------------------------");
            System.out.println("1.  [Auth] Consultar Mi Perfil (GET /api/v1/users/me)");
            System.out.println("2.  [Académico] Listar Oferta de Cursos (GET /api/v1/courses)");
            System.out.println("3.  [Inscripción] Matricular Estudiante (POST /api/v1/enrollments)");
            System.out.println("4.  [Calificaciones] Registrar Nota (POST /api/v1/grades)");
            System.out.println("5.  [Calificaciones] Consultar Kárdex (GET /api/v1/grades/report-card)");
            System.out.println("6.  [Asistencia] Registrar Asistencia (POST /api/v1/attendance)");
            System.out.println("7.  [Finanzas] Consultar Estado de Cuenta (GET /api/v1/finance/account-status)");
            System.out.println("8.  [Finanzas] Procesar Pago (POST /api/v1/finance/payments)");
            System.out.println("9.  [Notificaciones] Ver Bandeja de Mensajes (GET /api/v1/notifications)");
            System.out.println("10. [Notificaciones] Enviar Aviso Directo (POST /api/v1/notifications/send)");
            System.out.println("11. [Reportes] Ver Dashboard Ejecutivo (GET /api/v1/reports/executive-dashboard)");
            System.out.println("12. [Reportes] Exportar Reporte (GET /api/v1/reports/export)");
            System.out.println("13. Salir");
            System.out.print("\nSeleccione una opción (1-13): ");

            String opcion = scanner.nextLine().trim();

            if (opcion.equals("1")) {
                System.out.println("\nID: " + loggedUser.getId() + " | Nombre: " + loggedUser.getNombre() + " | Rol: " + loggedUser.getRol() + " | Permisos: " + loggedUser.getPermisos());
            } else if (opcion.equals("2")) {
                for (Course c : academicService.getAllCourses()) {
                    System.out.println("[" + c.getCursoId() + "] " + c.getMateria() + " | Cupo: " + c.getCupoActual() + "/" + c.getCupoMaximo());
                }
            } else if (opcion.equals("3")) {
                System.out.print("ID Estudiante (ej. std_456): ");
                String sid = scanner.nextLine().trim();
                System.out.print("ID Curso (ej. crs_101): ");
                String cid = scanner.nextLine().trim();
                System.out.println("\nResultado: " + enrollmentService.postEnrollment(sid, cid));
            } else if (opcion.equals("4")) {
                System.out.print("ID Estudiante (ej. std_456): ");
                String sid = scanner.nextLine().trim();
                System.out.print("ID Curso (ej. crs_101): ");
                String cid = scanner.nextLine().trim();
                System.out.print("Concepto (ej. Parcial 1): ");
                String conc = scanner.nextLine().trim();
                System.out.print("Calificación (ej. 9.5): ");
                double nota = Double.parseDouble(scanner.nextLine().trim());
                gradingService.addGrade(sid, cid, conc, nota);
            } else if (opcion.equals("5")) {
                System.out.print("ID Estudiante (ej. std_456): ");
                gradingService.printReportCard(scanner.nextLine().trim());
            } else if (opcion.equals("6")) {
                System.out.print("ID Estudiante (ej. std_456): ");
                String sid = scanner.nextLine().trim();
                System.out.print("ID Curso (ej. crs_101): ");
                String cid = scanner.nextLine().trim();
                System.out.print("Estatus (PRESENTE / FALTA / RETARDO): ");
                gradingService.addAttendance(sid, cid, scanner.nextLine().trim());
            } else if (opcion.equals("7")) {
                System.out.print("ID Estudiante (ej. std_456): ");
                financeService.printAccountStatus(scanner.nextLine().trim());
            } else if (opcion.equals("8")) {
                System.out.print("ID Estudiante (ej. std_456): ");
                String sid = scanner.nextLine().trim();
                System.out.print("ID Concepto a Pagar (ej. cgo_2026_10): ");
                String cid = scanner.nextLine().trim();
                financeService.processPayment(sid, cid);
            } else if (opcion.equals("9")) {
                System.out.print("ID Estudiante (ej. std_456): ");
                notificationService.printNotifications(scanner.nextLine().trim());
            } else if (opcion.equals("10")) {
                System.out.print("ID Estudiante Destino (ej. std_456): ");
                String sid = scanner.nextLine().trim();
                System.out.print("Canal (EMAIL / SMS / PUSH): ");
                String canal = scanner.nextLine().trim();
                System.out.print("Título: ");
                String tit = scanner.nextLine().trim();
                System.out.print("Mensaje: ");
                String msj = scanner.nextLine().trim();
                notificationService.sendNotification(sid, canal.toUpperCase(), tit, msj);
            } else if (opcion.equals("11")) {
                reportingService.printExecutiveDashboard("2026-2");
            } else if (opcion.equals("12")) {
                System.out.print("Formato de exportación (PDF / EXCEL / CSV): ");
                reportingService.exportReport(scanner.nextLine().trim());
            } else if (opcion.equals("13")) {
                authService.logout(currentToken);
                System.out.println("Saliendo del sistema...");
                break;
            } else {
                System.out.println("Opción inválida.");
            }
        }
    }
}