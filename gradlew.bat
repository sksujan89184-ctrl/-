@echo off
REM Minimal gradlew.bat fallback: use system gradle if wrapper jar missing
if exist gradle\wrapper\gradle-wrapper.jar (
  java -jar gradle\wrapper\gradle-wrapper.jar %*
) else (
  where gradle >nul 2>nul
  if errorlevel 1 (
    echo Gradle wrapper not found and gradle not installed.
    exit /b 1
  ) else (
    gradle %*
  )
)
