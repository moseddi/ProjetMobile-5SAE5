@echo off
title HangOut - Tests Mon Module (Avis ^& IA RAG)
cls
echo ==============================================================================
echo   TESTS DE MON MODULE : AVIS, INTELLIGENCE RAG ^& DETECTION D'ANOMALIES
echo   Auteur : Seddik (Module Avis ^& Intelligence Artificielle)
echo ==============================================================================
echo.
echo [1/2] Lancement des Tests IA ^& RAG (Python Pytest)...
python -m pytest C:\Users\seddi\Downloads\Flutter\Flutter\ai\tests -v
echo.
echo [2/2] Lancement des Tests Java Backend (Spring Boot ReviewService)...
cd /d "C:\Users\seddi\Downloads\Flutter\Flutter\backend"
mvn test -Dtest=ReviewServiceTest
echo.
echo ==============================================================================
echo   FIN DES TESTS DE MON MODULE
echo ==============================================================================
pause
