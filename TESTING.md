Manual verification and testing

Overview
- The repo includes a scaffolded on-device speaker verification flow.
- To fully use model inference, place a TFLite model at: `app/src/main/assets/voice_model.tflite`.

Manual smoke test (device/emulator)
1. Build & install the app from Android Studio or via Gradle:

   ./gradlew :app:installDebug

2. Grant microphone permission when prompted.
3. Use the app buttons:
   - "Enroll": speak for ~3 seconds to enroll the default `owner` template.
   - "Verify": speak for ~2 seconds to test verification; the app will speak back the result.
   - "Make PDF" / other actions are gated by verification.

Notes for developers
- `SpeakerVerifier` will attempt TFLite inference if `voice_model.tflite` exists; otherwise it uses a deterministic fallback embedding computed from averaged log-mel energies.
- Voice templates are stored encrypted using `EncryptedSharedPreferences` under the key `embed_<templateId>`.

Running unit tests (JVM)
- From the repository root run:

  ./gradlew :app:testDebugUnitTest

This will execute the pure-JVM tests for `AudioFeatures` (no Android device required).

Adding a real model
- Use a model that takes preprocessed audio or log-mel input consistent with the `SpeakerVerifier` preprocessing.
- Replace the placeholder model path and adjust input shaping in `SpeakerVerifier` accordingly.

