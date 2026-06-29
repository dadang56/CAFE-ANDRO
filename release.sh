#!/bin/bash
echo "=== Memulai Proses Build APK ==="
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug

if [ $? -eq 0 ]; then
  echo "=== Menyalin file APK ==="
  mkdir -p apk
  cp app/build/outputs/apk/debug/app-debug.apk apk/Dapoer_Lavana.apk
  
  echo "=== Menyimpan dan Mengunggah ke GitHub ==="
  git add apk/Dapoer_Lavana.apk
  git commit -m "Update APK untuk rilis versi baru"
  git push origin main
  echo "=== Proses Rilis Selesai & Sukses! ==="
else
  echo "=== Build gagal! Silakan periksa error di atas. ==="
fi
