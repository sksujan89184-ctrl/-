# MayaAgentApp - Android AI Assistant

## Overview

MayaAgentApp is an Android application that implements "Maya," a personal AI assistant with speaker-verification gated actions. The app features voice-based biometric authentication, PDF creation from gallery images, and a live action log. The core focus is on privacy-first design where sensitive operations like speaker verification happen entirely on-device.

Key capabilities:
- On-device speaker verification using TFLite models
- Voice enrollment and verification workflow
- PDF generation and merging from selected images
- Secure storage of voice embeddings using encrypted preferences
- Webhook integration with HMAC-SHA256 signing
- Text-to-speech via server-side synthesis

## User Preferences

Preferred communication style: Simple, everyday language.

## System Architecture

### Mobile Application Architecture
- **Platform**: Native Android (Kotlin)
- **Build System**: Gradle with wrapper support
- **CI/CD**: Codemagic for automated APK builds
- **Main Entry Point**: `MainActivity.kt` handles core UI and workflows

### Speaker Verification System
- **Approach**: On-device TFLite inference for privacy (no raw audio sent off-device)
- **Model Location**: `app/src/main/assets/voice_model.tflite`
- **Fallback**: Deterministic embedding from averaged log-mel energies when model unavailable
- **Storage**: Voice embeddings stored in `EncryptedSharedPreferences` with keys like `embed_<templateId>`
- **Flow**: Enrollment captures ~3 seconds of speech; verification uses ~2 seconds and compares via cosine similarity

### Security Design
- Voice templates never leave the device
- Embeddings encrypted via Android Keystore
- Webhook payloads signed with HMAC-SHA256
- TTS handled server-side to avoid exposing API secrets on device
- Deletion operations require voice confirmation with speaker-ID verification

### PDF Generation
- Uses Android's native `PdfDocument` API
- Merges selected images from device gallery

### Audio Processing
- `SpeakerVerifier.kt` handles audio preprocessing and TFLite inference
- `AudioFeatures` provides log-mel energy computation (testable on JVM)

## External Dependencies

### Build & CI Services
- **Codemagic**: Automated release APK builds via `codemagic.yaml`
- **Required Secrets**: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

### Android Platform APIs
- `SpeechRecognizer`: Voice input capture
- `PdfDocument`: PDF creation
- `EncryptedSharedPreferences`: Secure embedding storage
- Android Keystore: Key management for HMAC signing

### Machine Learning
- **TensorFlow Lite**: On-device speaker embedding inference
- Model expects preprocessed audio/log-mel input and outputs fixed-size embeddings (e.g., 128-d)

### Server-Side Integration
- **Webhook Helper**: Sends signed JSON payloads to external endpoints
- **TTS Helper**: Calls server-side endpoint for Edge-TTS synthesis (audio returned to device)

### Model Conversion (Development)
- Python script template for PyTorch → TFLite conversion
- Dependencies: torch, numpy, tensorflow