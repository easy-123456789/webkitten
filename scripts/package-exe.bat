@echo off
powershell -ExecutionPolicy Bypass -File "%~dp0package-exe.ps1" %*
