-- Migrasi AMAN (tidak menghapus data apa pun) untuk fitur:
-- Notifikasi push (Firebase Cloud Messaging)
--
-- Cara pakai: buka Supabase Dashboard -> SQL Editor -> paste seluruh isi file ini -> Run.

CREATE TABLE IF NOT EXISTS public.fcm_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id TEXT NOT NULL, -- customer: user id Supabase Auth; admin/driver: staff_accounts.id
    role TEXT NOT NULL, -- 'Customer', 'Admin', 'Driver'
    token TEXT NOT NULL UNIQUE, -- token FCM perangkat, unik per device
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_fcm_tokens_owner ON public.fcm_tokens(owner_id, role);

ALTER TABLE public.fcm_tokens ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'fcm_tokens' AND policyname = 'Token FCM dapat diakses siapa saja'
    ) THEN
        CREATE POLICY "Token FCM dapat diakses siapa saja" ON public.fcm_tokens FOR ALL USING (true);
    END IF;
END $$;
