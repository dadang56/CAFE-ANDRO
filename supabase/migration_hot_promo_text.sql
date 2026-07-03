-- Migrasi AMAN (tidak menghapus data apa pun) untuk fitur:
-- Judul & subjudul "Hot Promo" di beranda pelanggan bisa diedit dari Admin.
-- Mengikuti pola yang sudah ada di settings.latest_version_code: kolom
-- "value" numerik tidak dipakai, "description" menyimpan teks yang tampil.
--
-- Cara pakai: buka Supabase Dashboard -> SQL Editor -> paste seluruh isi file ini -> Run.

INSERT INTO public.settings (key, value, description)
VALUES ('hot_promo_title', 0, 'Hot Promo')
ON CONFLICT (key) DO NOTHING;

INSERT INTO public.settings (key, value, description)
VALUES ('hot_promo_subtitle', 0, 'Penawaran spesial untukmu')
ON CONFLICT (key) DO NOTHING;
