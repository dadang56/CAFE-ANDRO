-- Migrasi AMAN (tidak menghapus data apa pun) untuk fitur:
-- Status buka/tutup warung (default: buka)
--
-- Cara pakai: buka Supabase Dashboard -> SQL Editor -> paste seluruh isi file ini -> Run.

INSERT INTO public.settings (key, value, description)
VALUES ('restaurant_open', 1, 'Set 1 = warung buka (pelanggan bisa memesan), 0 = warung tutup (tampilan pelanggan diganti layar Tutup)')
ON CONFLICT (key) DO NOTHING;
