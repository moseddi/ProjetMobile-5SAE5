@echo off
title HangOut - Tests Module Binome (Evenements ^& Description IA)
cls
echo ==============================================================================
echo   TESTS DU MODULE DE MA BINOME : EVENEMENTS ^& DESCRIPTION IA
echo   Auteur : Binome Evenements
echo ==============================================================================
echo.
echo [1/2] Lancement des Tests Java Backend EventService (Spring Boot Mockito)...
cd /d "C:\Users\seddi\Downloads\Flutter\Flutter\backend"
mvn test -Dtest=EventServiceTest
echo.
echo [2/2] Lancement des Tests Java Backend EventDescriptionService (AI Groq Client)...
mvn test -Dtest=EventDescriptionServiceTest
echo.
echo ==============================================================================
echo   FIN DES TESTS DU MODULE EVENEMENTS
echo ==============================================================================
pause
