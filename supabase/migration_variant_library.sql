-- Migrasi AMAN (tidak menghapus data apa pun) untuk fitur PUSTAKA VARIAN.
--
-- Bedanya dengan migration_menu_variants.sql (varian per-produk, kolom
-- menu_items.variants): di sini satu varian dibuat SEKALI lalu diterapkan ke
-- banyak produk -- jadi "Topping" tidak perlu diisi ulang di 10 menu.
--
--   variant_groups     : pustaka varian (mis. "Level Pedas", "Olahan Telor")
--   menu_item_variants : produk mana memakai varian mana (relasi banyak-ke-banyak)
--
-- Kolom menu_items.variants dari migrasi lama TIDAK dihapus dan tetap dibaca
-- sebagai cadangan untuk produk yang variannya diisi manual per-produk.
--
-- Cara pakai: buka Supabase Dashboard -> SQL Editor -> paste seluruh isi file ini -> Run.

-- 1. Pustaka varian
CREATE TABLE IF NOT EXISTS public.variant_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,  -- wajib dipilih pembeli
    multiple BOOLEAN NOT NULL DEFAULT FALSE,  -- TRUE = boleh pilih >1 (checkbox), FALSE = maks 1 (radio)
    is_active BOOLEAN NOT NULL DEFAULT TRUE,  -- toggle di tab "Varian"; non-aktif = tidak ditawarkan ke pembeli
    options TEXT NOT NULL DEFAULT '[]',       -- JSON List<VariantOption>: [{"name":"Pedas","price":0.0}]
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

ALTER TABLE public.variant_groups ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Varian dapat dibaca siapa saja" ON public.variant_groups;
CREATE POLICY "Varian dapat dibaca siapa saja" ON public.variant_groups FOR SELECT USING (true);
DROP POLICY IF EXISTS "Varian dapat dikelola siapa saja" ON public.variant_groups;
CREATE POLICY "Varian dapat dikelola siapa saja" ON public.variant_groups FOR ALL USING (true);

-- 2. Penerapan varian ke produk
CREATE TABLE IF NOT EXISTS public.menu_item_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_item_id UUID NOT NULL REFERENCES public.menu_items(id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES public.variant_groups(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    -- Satu produk tidak boleh memakai varian yang sama dua kali.
    UNIQUE (menu_item_id, group_id)
);

CREATE INDEX IF NOT EXISTS idx_menu_item_variants_menu ON public.menu_item_variants(menu_item_id);
CREATE INDEX IF NOT EXISTS idx_menu_item_variants_group ON public.menu_item_variants(group_id);

ALTER TABLE public.menu_item_variants ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Penerapan varian dapat dibaca siapa saja" ON public.menu_item_variants;
CREATE POLICY "Penerapan varian dapat dibaca siapa saja" ON public.menu_item_variants FOR SELECT USING (true);
DROP POLICY IF EXISTS "Penerapan varian dapat dikelola siapa saja" ON public.menu_item_variants;
CREATE POLICY "Penerapan varian dapat dikelola siapa saja" ON public.menu_item_variants FOR ALL USING (true);
