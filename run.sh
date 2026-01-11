#!/bin/bash
echo "=================================================="
echo "        Maya Agent Android App Project"
echo "=================================================="
echo "Status: Project is ready for Android Studio."
echo ""
echo "Project Structure:"
find app -maxdepth 2 -not -path '*/.*'
echo ""
echo "Build command:"
echo "./gradlew assembleDebug"
echo "=================================================="
