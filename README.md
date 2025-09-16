# TravelMate Backend - Configuración

## Requisitos Previos

1. **Java 21** instalado
2. **PostgreSQL** instalado y ejecutándose
3. **Maven** instalado (o usar el wrapper incluido)

## Configuración de la Base de Datos

1. Crear una base de datos PostgreSQL:
```sql
CREATE DATABASE travelmate_db;
CREATE USER postgres WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE travelmate_db TO postgres;
```

2. Actualizar las credenciales en `src/main/resources/application.properties` si es necesario:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/travelmate_db
spring.datasource.username=postgres
spring.datasource.password=password
```

## Configuración de Google OAuth2

1. Ir a [Google Cloud Console](https://console.cloud.google.com/)
2. Crear un nuevo proyecto o seleccionar uno existente
3. Habilitar la API de Google+ y OAuth2
4. Crear credenciales OAuth2:
   - Tipo: Aplicación web
   - Orígenes autorizados: `http://localhost:3000`
   - URIs de redirección autorizados: `http://localhost:3000/auth/google/callback`
5. Copiar el Client ID y Client Secret
6. Configurar las variables de entorno o actualizar `application.properties`:
```properties
spring.security.oauth2.client.registration.google.client-id=TU_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=TU_GOOGLE_CLIENT_SECRET
```

## Ejecutar la Aplicación

1. Navegar al directorio del backend:
```bash
cd TravelMate-Backend
```

2. Ejecutar la aplicación:
```bash
./mvnw spring-boot:run
```

O en Windows:
```bash
mvnw.cmd spring-boot:run
```

La aplicación estará disponible en `http://localhost:8080`

## Endpoints Disponibles

### Autenticación
- `POST /api/auth/login` - Iniciar sesión
- `POST /api/auth/register` - Registrarse
- `GET /api/auth/me` - Obtener usuario actual
- `POST /api/auth/logout` - Cerrar sesión

### OAuth2
- `GET /oauth2/authorization/google` - Iniciar OAuth2 con Google
- `GET /api/oauth2/success` - Callback de éxito OAuth2

## Estructura de la Base de Datos

La aplicación usa **Code First** con JPA/Hibernate. Las tablas se crean automáticamente basándose en los modelos:

### Tabla `users`
- `id` (BIGINT, PRIMARY KEY, AUTO_INCREMENT)
- `name` (VARCHAR(50), NOT NULL)
- `email` (VARCHAR(100), NOT NULL, UNIQUE)
- `password` (VARCHAR, NOT NULL)
- `google_id` (VARCHAR, UNIQUE)
- `profile_picture_url` (VARCHAR)
- `provider` (VARCHAR, NOT NULL) - 'LOCAL' o 'GOOGLE'
- `email_verified` (BOOLEAN, NOT NULL, DEFAULT false)
- `created_at` (TIMESTAMP, NOT NULL)
- `updated_at` (TIMESTAMP)

## Configuración de CORS

El backend está configurado para aceptar peticiones desde `http://localhost:3000` (frontend Next.js).

## JWT

- **Secret**: Configurado en `application.properties`
- **Expiración**: 24 horas (86400000 ms)
- **Algoritmo**: HMAC SHA-256

## Logs

Los logs de SQL están habilitados para desarrollo. Para producción, deshabilitar:
```properties
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
```
