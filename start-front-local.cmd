@echo off
set VITE_API_BASE_URL=
cd /d "%~dp0front"
npm run dev -- --host 0.0.0.0 --port 3000
