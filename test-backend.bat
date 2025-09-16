@echo off
echo Probando el backend de TravelMate...
echo.

echo 1. Verificando que el backend esté ejecutándose...
curl -s http://localhost:8080/api/test/hello
echo.
echo.

echo 2. Verificando la URL de OAuth2...
curl -s http://localhost:8080/api/test/oauth2-url
echo.
echo.

echo 3. Probando el endpoint de login...
curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"email\":\"test@test.com\",\"password\":\"password123\"}"
echo.
echo.

echo Pruebas completadas.
pause
