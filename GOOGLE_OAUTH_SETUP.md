# Configuración de Google OAuth2

## Pasos para configurar Google OAuth2:

### 1. Ir a Google Cloud Console
- Visita: https://console.cloud.google.com/
- Inicia sesión con tu cuenta de Google

### 2. Crear o seleccionar un proyecto
- Crea un nuevo proyecto o selecciona uno existente
- Anota el ID del proyecto

### 3. Habilitar APIs necesarias
- Ve a "APIs y servicios" > "Biblioteca"
- Busca y habilita:
  - Google+ API
  - Google OAuth2 API

### 4. Crear credenciales OAuth2
- Ve a "APIs y servicios" > "Credenciales"
- Haz clic en "Crear credenciales" > "ID de cliente OAuth 2.0"
- Selecciona "Aplicación web"

### 5. Configurar la aplicación web
- **Nombre**: TravelMate
- **Orígenes autorizados**: 
  - `http://localhost:3000`
  - `http://localhost:8080`
- **URIs de redirección autorizados**:
  - `http://localhost:8080/login/oauth2/code/google`

### 6. Obtener las credenciales
- Copia el **Client ID** y **Client Secret**
- Guárdalos de forma segura

### 7. Configurar variables de entorno
Crea un archivo `.env` en la raíz del proyecto backend:

```bash
GOOGLE_CLIENT_ID=tu_client_id_aqui
GOOGLE_CLIENT_SECRET=tu_client_secret_aqui
```

### 8. O actualizar application.properties
```properties
spring.security.oauth2.client.registration.google.client-id=tu_client_id_aqui
spring.security.oauth2.client.registration.google.client-secret=tu_client_secret_aqui
```

## Flujo de autenticación:

1. Usuario hace clic en "Entrar con Google"
2. Se redirige a Google para autenticación
3. Google redirige de vuelta a: `http://localhost:8080/login/oauth2/code/google`
4. Spring Security procesa la respuesta
5. Se crea/actualiza el usuario en la base de datos
6. Se redirige a: `http://localhost:3000/auth/callback?token=JWT_TOKEN`
7. El frontend procesa el token y redirige al dashboard

## URLs importantes:

- **Iniciar OAuth2**: `http://localhost:8080/oauth2/authorization/google`
- **Callback del backend**: `http://localhost:8080/login/oauth2/code/google`
- **Callback del frontend**: `http://localhost:3000/auth/callback`

## Solución de problemas:

### Error 400: "Bad Request"
- Verifica que las URIs de redirección estén configuradas correctamente
- Asegúrate de que el Client ID y Secret sean correctos

### Error de CORS
- Verifica que `http://localhost:3000` esté en los orígenes autorizados
- Revisa la configuración de CORS en SecurityConfig

### Usuario no se crea
- Verifica que la base de datos esté ejecutándose
- Revisa los logs de la aplicación para errores
