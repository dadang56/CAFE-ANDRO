# Panduan Fitur Auto-Update (In-App Update)

Aplikasi Dapoer Lavana punya fitur auto-update bawaan: saat Anda merilis versi baru,
pengguna akan **otomatis melihat dialog "Pembaruan Tersedia"** ketika membuka aplikasi,
lalu bisa mengunduh & memasang APK terbaru langsung dari dalam aplikasi.

## Kenapa dulu tidak muncul?
Nilai `latest_version_code` di tabel `settings` masih **2.0**, sedangkan `versionCode`
aplikasi sudah **12**. Karena syaratnya `latest_version_code > versionCode terpasang`,
`2 > 12` = salah → dialog tidak pernah muncul. Sekarang mekanismenya sudah dirapikan
dan didokumentasikan.

## Cara kerja
Saat app dibuka (`MainActivity`):
1. Membaca tabel `settings` key `latest_version_code`.
2. Jika nilainya **lebih besar** dari `versionCode` app yang terpasang → tampilkan dialog update.
3. Deskripsi baris `latest_version_code` dipakai sebagai **Catatan Rilis** di dialog.
4. Key `force_update` = `1` → update **wajib** (dialog tidak bisa ditutup). `0` → ada tombol **"Nanti"**.
5. Tombol **"Perbarui Sekarang"** mengunduh APK dari URL di tabel `banners` (title = `app_apk_url`,
   kolom `image_url` = link APK), lalu membuka installer Android (izin `REQUEST_INSTALL_PACKAGES` sudah ada).

## Langkah merilis update baru
1. **Naikkan versi** di `app/build.gradle.kts`:
   ```kotlin
   versionCode = 13          // WAJIB dinaikkan tiap rilis
   versionName = "10.0.3"    // untuk tampilan
   ```
2. **Build APK**: `./gradlew assembleRelease` (atau `assembleDebug` untuk uji).
3. **Upload APK** ke hosting yang menyajikan file langsung, mis:
   - Supabase Storage (bucket publik) → salin Public URL, atau
   - GitHub (folder `apk/` di repo) → pakai link `raw.githubusercontent.com/...`, atau
   - hosting lain apa pun yang mengembalikan file `.apk`.
4. **Update Supabase** (SQL Editor atau Table Editor):
   ```sql
   -- 1) set versi terbaru = versionCode baru (harus > versi terpasang)
   UPDATE public.settings
     SET value = 13,
         description = 'Perbaikan bug, desain baru, Google Maps, dll.'  -- jadi Catatan Rilis
     WHERE key = 'latest_version_code';

   -- 2) opsional: paksa update
   UPDATE public.settings SET value = 0 WHERE key = 'force_update';  -- 1 = wajib

   -- 3) set/nyalakan URL APK (dibaca dari tabel banners)
   INSERT INTO public.banners (title, image_url)
     VALUES ('app_apk_url', 'https://<link-langsung-ke-APK-Anda>.apk')
   ON CONFLICT DO NOTHING;
   -- atau jika baris app_apk_url sudah ada:
   UPDATE public.banners
     SET image_url = 'https://<link-langsung-ke-APK-Anda>.apk'
     WHERE title = 'app_apk_url';
   ```
5. Selesai. Pengguna yang membuka app akan langsung melihat dialog update.

## Uji cepat tanpa rilis
Untuk memastikan dialog muncul: set sementara `latest_version_code` ke angka lebih besar
dari `versionCode` app di HP uji (mis. `999`), buka app → dialog harus muncul.
Kembalikan ke nilai sebenarnya setelah uji.

## Catatan
- Pastikan bucket/URL APK **publik** dan mengembalikan file `.apk` mentah (bukan halaman HTML).
- Untuk keamanan, batasi API Key Google Maps di Google Cloud Console (package `com.lavana.dapoer` + SHA-1).
