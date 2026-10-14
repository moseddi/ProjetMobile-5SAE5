@echo off
title HangOut RAG API
cd /d "C:\Users\seddi\Downloads\Flutter\Flutter\ai\rag"

:: Activate conda base environment
call conda activate base

:: Start the FastAPI server
echo [HangOut AI] Starting RAG API on http://localhost:8000 ...
python rag_api.py
