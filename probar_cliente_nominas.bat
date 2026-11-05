@echo off
setlocal
cd /d "%~dp0"
echo Ejecutando prueba de integracion: Control Escolar -> Nominas API...
java TestIntegracionNominas
pause
