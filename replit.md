# Maya Agent Android App

## Overview
This is a native Android application built with Kotlin and Gradle. The app appears to be an AI agent assistant with audio processing, speaker verification, and persona management capabilities.

## Project Status
- **Type:** Native Android Application
- **Build System:** Gradle 7.6
- **Language:** Kotlin 1.9.0
- **Target SDK:** 33 (Android 13)
- **Minimum SDK:** 24 (Android 7.0)

## Project Structure
```
android-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/maya/
│   │   │   ├── MainActivity.kt
│   │   │   ├── AudioFeatures.kt
│   │   │   ├── AudioPreprocessor.kt
│   │   │   ├── AudioRecorder.kt
│   │   │   ├── PersonaManager.kt
│   │   │   ├── SpeakerVerifier.kt
│   │   │   ├── TTSHelper.kt
│   │   │   ├── WebhookHelper.kt
│   │   │   └── crew/Agent.kt
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── gradle/
├── scripts/
├── build.gradle
├── settings.gradle
└── gradlew
```

## Building the App

### Requirements
The Android SDK is required to build this app, which is not available in the standard Replit environment.

### Build Options

1. **Android Studio (Recommended)**
   - Download from: https://developer.android.com/studio
   - Open the `android-app` folder as a project
   - Let Gradle sync and build

2. **Codemagic CI/CD**
   - The project includes `codemagic.yaml` for cloud builds
   - Connect the repository to Codemagic

3. **Command Line** (requires Android SDK)
   ```bash
   cd android-app
   ./gradlew assembleDebug
   ```

## Key Dependencies
- AndroidX Core KTX 1.9.0
- AppCompat 1.6.1
- Material Design 1.9.0
- OkHttp 4.10.0 (for networking)
- Security Crypto 1.1.0-alpha06

## Recent Changes
- 2026-01-11: Initial import and Replit environment setup
