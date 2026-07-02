-- Migrasi AMAN (tidak menghapus data apa pun) untuk fitur:
-- Nama pemesan pada struk (resi & struk dapur)
--
-- Cara pakai: buka Supabase Dashboard -> SQL Editor -> paste seluruh isi file ini -> Run.

ALTER TABLE public.orders
  ADD COLUMN IF NOT EXISTS customer_name TEXT;
