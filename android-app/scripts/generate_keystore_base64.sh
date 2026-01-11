#!/bin/bash
# Usage: ./generate_keystore_base64.sh path/to/keystore.jks
set -e
if [ -z "$1" ]; then
  echo "Usage: $0 path/to/keystore.jks" >&2
  exit 2
fi
KS_PATH="$1"
if [ ! -f "$KS_PATH" ]; then
  echo "Keystore not found: $KS_PATH" >&2
  exit 3
fi
BASE64=$(base64 -w 0 "$KS_PATH")
echo "Export the following to Codemagic as an encrypted env var KEYSTORE_BASE64 (do NOT commit the keystore):"
echo
echo "KEYSTORE_BASE64=$BASE64"
echo
echo "Example (local):"
echo "export KEYSTORE_BASE64=\"$BASE64\""
