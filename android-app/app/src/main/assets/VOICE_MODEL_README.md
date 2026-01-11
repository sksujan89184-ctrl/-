Speaker model placement

- Place your TensorFlow Lite speaker identification/embedding model here with filename:
  `voice_model.tflite`.

- The scaffold `SpeakerVerifier.kt` expects `voice_model.tflite` in assets. Replace
  preprocessing and inference logic in `SpeakerVerifier.kt` to match your model's
  input/output signatures.

- Security notes:
  - Do NOT upload raw voice templates off-device.
  - Store any enrolled embeddings encrypted using the Android Keystore.

- Suggested models:
  - A speaker-embedding TFLite model that outputs a fixed-size embedding (e.g., 128-d).
  - If you need an example model, prepare a conversion from a PyTorch/TensorFlow
    speaker-embedding model to TFLite with float32 or quantized inputs.
