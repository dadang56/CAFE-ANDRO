-- Migrasi AMAN (tidak menghapus data apa pun) untuk fitur:
-- 1. Chat Admin <-> Pelanggan per pesanan
-- 2. Foto bukti + jarak konfirmasi pengantaran driver
--
-- Cara pakai: buka Supabase Dashboard -> SQL Editor -> paste seluruh isi file ini -> Run.

-- 1. Kolom baru di tabel orders untuk bukti pengantaran driver
ALTER TABLE public.orders
  ADD COLUMN IF NOT EXISTS delivery_proof_url TEXT,
  ADD COLUMN IF NOT EXISTS delivery_distance_meters NUMERIC,
  ADD COLUMN IF NOT EXISTS delivery_within_tolerance BOOLEAN;

-- 2. Tabel chat baru (per pesanan)
CREATE TABLE IF NOT EXISTS public.chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID REFERENCES public.orders(id) ON DELETE CASCADE NOT NULL,
    sender_role TEXT NOT NULL, -- 'Admin' atau 'Customer'
    sender_name TEXT,
    message TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

ALTER TABLE public.chat_messages ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'chat_messages' AND policyname = 'Chat dapat diakses siapa saja'
    ) THEN
        CREATE POLICY "Chat dapat diakses siapa saja" ON public.chat_messages FOR ALL USING (true);
    END IF;
END $$;

-- Aktifkan Realtime pada tabel chat_messages agar pesan baru langsung muncul
-- tanpa perlu refresh manual (aman dijalankan berkali-kali).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime' AND tablename = 'chat_messages'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.chat_messages;
    END IF;
END $$;
