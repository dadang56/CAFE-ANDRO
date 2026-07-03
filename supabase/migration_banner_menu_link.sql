-- Migrasi AMAN (tidak menghapus data apa pun) untuk fitur:
-- Koneksi banner promo ke menu (tap banner -> lihat menu terhubung)
--
-- Cara pakai: buka Supabase Dashboard -> SQL Editor -> paste seluruh isi file ini -> Run.

ALTER TABLE public.banners
  ADD COLUMN IF NOT EXISTS linked_menu_item_id UUID REFERENCES public.menu_items(id) ON DELETE SET NULL;
