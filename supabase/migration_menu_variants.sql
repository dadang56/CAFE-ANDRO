-- Migrasi AMAN (tidak menghapus data apa pun) untuk fitur:
-- Varian produk per-menu (mis. Level Pedas, Topping) yang dipilih pembeli saat pesan.
--
-- Kolom "variants" menyimpan JSON List<VariantGroup>, mis:
--   [{"name":"Level Pedas","required":true,"multiple":false,
--     "options":[{"name":"Tidak Pedas","price":0.0},{"name":"Pedas","price":0.0}]},
--    {"name":"Topping","required":false,"multiple":true,
--     "options":[{"name":"Bawang Goreng","price":2000.0}]}]
-- NULL / kosong = produk tanpa varian (perilaku lama, tetap jalan).
--
-- Cara pakai: buka Supabase Dashboard -> SQL Editor -> paste seluruh isi file ini -> Run.

ALTER TABLE public.menu_items
  ADD COLUMN IF NOT EXISTS variants TEXT;
