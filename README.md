# SIGIP-Design-Service

Gestión de diseños y aprobaciones para SIGIP.

- Java 25
- Spring Boot 3.5.6
- Spring Cloud 2025.0.0
- Configuración: application.properties
- Puerto local: 8096
- Base de datos: sigip_design

Configura `DB_USERNAME` y `DB_PASSWORD` en el entorno local si no utilizas las credenciales por defecto. Requiere MySQL y Eureka (puerto 8761).

Endpoint de salud: `GET /api/design/health` (consulta la ruta del controlador para el path exacto).