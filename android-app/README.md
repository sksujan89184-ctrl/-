# MayaAgentApp (Android)

This repository provides a minimal Android app skeleton for `Maya` — an assistant with speaker-verification gated actions, a PDF maker, and a Live Action Log. It includes a `system_prompt.json` you can use to seed your assistant's system prompt.

Quick notes:
- Speaker verification is a placeholder in `MainActivity.kt` — you must replace it with a production on-device speaker-ID model (do NOT send raw audio off-device).
- PDF merging uses Android `PdfDocument` and merges selected images from the gallery.
- Deletion flow shows how to capture a voice confirmation phrase using `SpeechRecognizer` (implementers should harden this with a proper speaker-ID check on confirmation).

Codemagic
- `codemagic.yaml` is included to build an APK (release). Add these encrypted environment variables in Codemagic settings:
  - `KEYSTORE_BASE64` (base64 of your keystore `.jks`)
  - `KEYSTORE_PASSWORD`
  - `KEY_ALIAS`
  - `KEY_PASSWORD`
- The workflow runs `./gradlew assembleRelease` and stores the artifact at `app/build/outputs/apk/release/`.
 
Gradle / Codemagic notes:
- A minimal `gradlew` fallback script is included which will use the system `gradle` if the wrapper JAR isn't available. Codemagic images usually have Gradle installed; if you prefer the wrapper, add the Gradle wrapper JAR under `gradle/wrapper/`.
- To generate the base64 keystore for Codemagic, use `scripts/generate_keystore_base64.sh path/to/keystore.jks` and paste the output into the `KEYSTORE_BASE64` encrypted variable.
Files of interest:
- `android-app/app/src/main/java/com/example/maya/MainActivity.kt` - core UI and workflows
- `android-app/system_prompt.json` - final system prompt for Maya
- `android-app/codemagic.yaml` - Codemagic build config

Speaker verification scaffold:
- `android-app/app/src/main/java/com/example/maya/SpeakerVerifier.kt` includes a TFLite scaffold showing where to load a model and how to integrate enrollment/verification logic. It is a placeholder and must be implemented to match your chosen model.
- Place your model at `android-app/app/src/main/assets/voice_model.tflite` or update the path in `SpeakerVerifier.kt`.

Enrollment & verification flows:
- Use the `Enroll Voice` and `Verify Voice` buttons in the app. The app records a short sample and stores an embedding in encrypted preferences (placeholder).
- `SpeakerVerifier.kt` expects a TFLite model that accepts preprocessed audio and returns a fixed-size embedding. The scaffold saves embeddings in `EncryptedSharedPreferences` and compares by cosine similarity with a placeholder threshold of `0.75`.

Webhook & TTS helpers:
- `WebhookHelper.kt` demonstrates sending an HMAC-SHA256-signed JSON payload to a webhook (use your base64 key). Keep keys in Android Keystore and provide the base64 secret via Codemagic or secure storage.
- `TTSHelper.kt` shows a safe pattern: call your server-side TTS endpoint which performs Edge-TTS synthesis and returns audio. Avoid calling TTS provider APIs with secrets directly from the device.

Next steps I can do for you (choose):
- Add Gradle wrapper files and versions so Codemagic runs without extra setup.
- Implement a sample on-device speaker verification integration (requires chosen model/library).
- Add automated tests and CI checks.

Run locally (basic):
1. Open the project in Android Studio: `android-app` folder.
2. Provide required permissions at runtime and test on a device/emulator.

Let me know which next step you want me to take. If you want, I can add the Gradle wrapper now so Codemagic can run immediately.