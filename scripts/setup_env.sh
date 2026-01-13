#!/bin/bash
# Generate a debug keystore if it doesn't exist
if [ ! -f debug.keystore ]; then
    keytool -genkey -v -keystore debug.keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US"
fi

# Convert keystore to Base64
base64 -w 0 debug.keystore > keystore_base64.txt

echo "=================================================="
echo "          Environment Variable Values"
echo "=================================================="
echo "KEYSTORE_BASE64: $(cat keystore_base64.txt)"
echo "KEYSTORE_PASSWORD: android"
echo "KEY_ALIAS: androiddebugkey"
echo "KEY_PASSWORD: android"
echo "=================================================="
