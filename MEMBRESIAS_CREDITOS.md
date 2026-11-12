# Membresías, créditos, autorización y paquetes por tiempo

## Alcance

Implementación **académica y local**, sin cobros reales. Conserva las funciones básicas de Kárdex y Mapa del clima. El acceso a funciones *opcionales* se controla desde `AccessDemo.js` mediante perfil RBAC, saldo de créditos, vigencia y una bitácora local por usuario.

**Aviso de seguridad:** la cuenta, los roles, los créditos y las membresías de esta versión se gestionan en el navegador; **NO** son autorización segura para producción. `localStorage` y JavaScript pueden modificarse desde DevTools. Para usar dinero real, créditos comprados o datos académicos reales se requiere login del lado del servidor, sesiones seguras, autorización en cada endpoint, saldo en base de datos y transacciones atómicas.

## 3 membresías (valores de demostración)

| Membresía | Precio ficticio | Créditos asignados |
|---|---:|---:|
| Básica | $49 MXN DEMO | 60 |
| Plus | $99 MXN DEMO | 150 |
| Premium | $199 MXN DEMO | 350 |

Pulsar «Activar» solo simula la compra y añade créditos al saldo del usuario actual. **No se captura tarjeta ni se solicita pago a proveedor alguno.**

## Paquetes de tiempo (comprados con créditos)

Cada paquete habilita durante el periodo indicado la función extra de **uno** de los dos módulos, sin descontar nuevos créditos por uso. Si existe un paquete activo, se extiende desde su fecha de caducidad. El servidor no procesa ni controla estos paquetes por ahora.

| Duración | Coste |
|---|---:|
| 15 minutos | 15 créditos |
| 2 horas | 60 créditos |
| 7 días | 150 créditos |

## Los dos módulos

1. **Kárdex y Notas:** ver calificaciones es gratuito; **exportar CSV** del Kárdex de demostración cuesta 8 créditos por uso o se permite gratis durante un paquete activo. ALUMNO, PROFESOR y ADMINISTRACION pueden acceder a la exportación, FINANZAS no.
2. **Mapa del clima:** mapa base y condiciones actuales gratuitos; activar cada capa meteorológica (nubes, precipitación o temperatura) cuesta 4 créditos por activación o es ilimitado durante un paquete de Clima activo. Cualquier rol autenticado del sistema puede acceder.

Si no hay saldo suficiente, la operación se deniega y se muestra una notificación. El saldo, membresía, expiración e historial se guardan por ID de usuario en `localStorage` de este navegador. Si se abre desde otro navegador, no se sincroniza.

## Pruebas

1. Actualizar la rama `feature/membresias-creditos-tiempo`.
2. Ejecutar en PowerShell (si quieres que también funcione el clima): `$env:OPENWEATHER_API_KEY = "TU_CLAVE_PRIVADA"`.
3. Compilar: `javac -encoding UTF-8 Main.java ProfactClient.java WeatherHandler.java EscolarHttpServer.java`.
4. Iniciar: `java EscolarHttpServer`.
5. Abrir **http://localhost:8081/** (no abrir `index.html` con doble clic).
6. Iniciar sesión con un usuario de prueba.
7. Abrir **Membresías y Créditos**, activar la membresía Básica y comprobar +60 créditos.
8. Ir a **Kárdex y Notas** y pulsar «Exportar CSV (8 créditos)»: debe descargar el archivo de ejemplo y bajar el saldo 8 créditos. Repetir con 0 créditos debe bloquear.
9. Ir a **Mapa del clima** y elegir Nubes: debe consumir 4 créditos por activación. Seleccionar «Solo mapa base» no consume créditos.
10. Comprar un paquete de 15 minutos para Clima; seleccionar capas dentro de la vigencia no descuenta nuevos créditos. Fuera de la vigencia vuelve a descontar créditos.
11. Iniciar sesión como FINANZAS: la exportación de Kárdex no debe estar disponible ni podrá comprar su pase; Clima y Membresías sí estarán disponibles.
12. Cerrar sesión, entrar con otro usuario y comprobar que sus saldos estén separados.

## Próxima etapa para seguridad real

- Autenticación de servidor: usuarios y roles en BD, contraseñas hasheadas y sesiones firmadas.
- Persistencia del saldo, membresías, paquetes y movimientos en BD por usuario.
- Endpoints de autorización y compra que comprueben rol, saldo, expiración y cambios concurrentes.
- Idempotencia y transacciones SQL al descontar créditos.
- Validar compras reales por webhook firmado del procesador de pago, no por datos del navegador.
- Auditoría y pruebas automatizadas de acceso no autorizado, saldo insuficiente y expiración.

**No presentar esta maqueta como una solución financiera segura o una pasarela real.**
