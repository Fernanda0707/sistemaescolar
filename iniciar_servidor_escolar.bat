@echo off
setlocal
cd /d "%~dp0"
echo Compilando servidor escolar...
javac Main.java EscolarHttpServer.java TestIntegracionNominas.java
echo Iniciando Servidor HTTP Ligero de Control Escolar en el puerto 8081...
java EscolarHttpServer
pause
