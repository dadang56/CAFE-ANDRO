-- ==========================================
-- SKEMA DATABASE DAPOER LAVANA (TERBARU & LENGKAP)
-- Jalankan skrip SQL ini di SQL Editor Supabase Dashboard Anda.
-- ==========================================

-- Hapus tabel lama jika sudah ada (reset bersih)
DROP TABLE IF EXISTS public.order_items CASCADE;
DROP TABLE IF EXISTS public.orders CASCADE;
DROP TABLE IF EXISTS public.settings CASCADE;
DROP TABLE IF EXISTS public.menu_items CASCADE;
DROP TABLE IF EXISTS public.staff_accounts CASCADE;
DROP TABLE IF EXISTS public.promos CASCADE;

-- 1. Tabel Staff (Kasir, Driver, Admin)
CREATE TABLE public.staff_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    role TEXT NOT NULL, -- 'Admin', 'Kasir', 'Driver'
    name TEXT NOT NULL,
    contact_number TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Seed data staff bawaan untuk pengujian cepat
INSERT INTO public.staff_accounts (username, password, role, name, contact_number) VALUES
('admin@lavana.com', 'admin123', 'Admin', 'Administrator Lavana', '081234567890'),
('kasir@lavana.com', 'kasir123', 'Kasir', 'Kasir Siti', '081234567891'),
('driver@lavana.com', 'driver123', 'Driver', 'Budi Santoso (Driver)', '081234567892');

-- 2. Tabel Menu (Katalog Makanan & Minuman)
CREATE TABLE public.menu_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    description TEXT,
    price NUMERIC NOT NULL,
    category TEXT NOT NULL, -- 'Kopi', 'Cemilan', 'Makanan', dll.
    image_url TEXT,
    is_available BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

ALTER TABLE public.menu_items ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Menu dapat dibaca siapa saja" ON public.menu_items FOR SELECT USING (true);
CREATE POLICY "Menu dapat diakses penuh oleh siapa saja" ON public.menu_items FOR ALL USING (true);

-- 3. Tabel Banner Promosi (Horizontal Carousel)
CREATE TABLE public.banners (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    image_url TEXT NOT NULL,
    title TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

INSERT INTO public.banners (image_url, title) VALUES
('https://images.unsplash.com/photo-1541167760496-1628856ab772?w=800', 'Kopi Susu Lavana Spesial'),
('https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=800', 'Pandan Latte Menyegarkan'),
('https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=800', 'Croissant Butter Klasik');

ALTER TABLE public.banners ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Banners dapat dibaca siapa saja" ON public.banners FOR SELECT USING (true);
CREATE POLICY "Banners dapat dikelola siapa saja" ON public.banners FOR ALL USING (true);

-- 3b. View untuk Membaca Daftar Pelanggan dari auth.users
CREATE OR REPLACE VIEW public.customer_profiles AS
SELECT 
    id, 
    email, 
    created_at 
FROM auth.users;

GRANT SELECT ON public.customer_profiles TO anon, authenticated, service_role;

-- 3c. Fungsi Keamanan Tinggi untuk Menghapus User
CREATE OR REPLACE FUNCTION public.delete_user_by_id(user_uuid UUID)
RETURNS VOID AS $$
BEGIN
    DELETE FROM auth.users WHERE id = user_uuid;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.delete_user_by_id(UUID) TO anon, authenticated, service_role;


-- 4. Tabel Pengaturan (Ongkos Kirim Dinamis)
CREATE TABLE public.settings (
    id SERIAL PRIMARY KEY,
    key TEXT UNIQUE NOT NULL,
    value NUMERIC NOT NULL,
    description TEXT
);

INSERT INTO public.settings (key, value, description) VALUES
('delivery_base_fee', 5000.0, 'Tarif dasar pengiriman (jarak minimum)'),
('delivery_base_distance_km', 2.0, 'Jarak minimum dalam KM untuk tarif dasar'),
('delivery_per_km_fee', 2500.0, 'Tarif tambahan per KM setelah jarak dasar terlewati'),
('delivery_max_distance_km', 15.0, 'Batas jarak pengiriman maksimum dalam KM'),
('latest_version_code', 12, 'versionCode APK terbaru. HARUS > versionCode app terpasang agar dialog update muncul. Naikkan tiap rilis (samakan dengan versionCode di build.gradle.kts). Deskripsi baris ini dipakai sebagai Catatan Rilis di dialog update.'),
('force_update', 0, 'Set 1 untuk memaksa update (dialog tidak bisa ditutup), 0 untuk opsional (ada tombol Nanti)'),
('cafe_latitude', -6.2297, 'Latitude lokasi Dapoer Lavana Cafe'),
('cafe_longitude', 106.8296, 'Longitude lokasi Dapoer Lavana Cafe');

ALTER TABLE public.settings ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Settings dapat dibaca siapa saja" ON public.settings FOR SELECT USING (true);
CREATE POLICY "Settings dapat diupdate siapa saja" ON public.settings FOR UPDATE USING (true) WITH CHECK (true);

-- 5. Tabel Pesanan (Orders)
CREATE TABLE public.orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number SERIAL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    user_id UUID, -- NULL jika pesanan tamu / anonim
    order_type TEXT NOT NULL, -- 'Dine In', 'Take Away', 'Delivery'
    status TEXT DEFAULT 'Pending' NOT NULL, -- 'Pending', 'Diproses', 'Siap', 'Selesai'
    table_number TEXT, -- Khusus Dine In
    delivery_address TEXT, -- Khusus Delivery
    delivery_distance_km NUMERIC, -- Khusus Delivery
    delivery_fee NUMERIC DEFAULT 0 NOT NULL,
    subtotal NUMERIC NOT NULL,
    discount_amount NUMERIC DEFAULT 0 NOT NULL,
    total NUMERIC NOT NULL,
    payment_method TEXT NOT NULL, -- 'QRIS', 'DANA', 'GOPAY', 'Tunai'
    payment_status TEXT DEFAULT 'Belum Bayar' NOT NULL, -- 'Belum Bayar', 'Menunggu Verifikasi', 'Terbayar'
    payment_receipt_url TEXT, -- URL bukti transfer yang diunggah
    notes TEXT,
    customer_phone TEXT,
    coordinates TEXT, -- "latitude,longitude"
    driver_id UUID, -- ID kurir yang ditugaskan
    cashier_username TEXT -- Kasir pencatat order
);

ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Pesanan dapat diakses siapa saja" ON public.orders FOR ALL USING (true);

-- Enable Realtime pada tabel orders agar status terupdate otomatis di aplikasi Android
ALTER PUBLICATION supabase_realtime ADD TABLE public.orders;

-- 6. Tabel Detail Item Pesanan (Order Items)
CREATE TABLE public.order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID REFERENCES public.orders(id) ON DELETE CASCADE NOT NULL,
    menu_item_id UUID REFERENCES public.menu_items(id) NOT NULL,
    quantity INTEGER NOT NULL,
    price_at_order NUMERIC NOT NULL, -- Untuk mengunci harga saat dipesan
    notes TEXT
);

ALTER TABLE public.order_items ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Item pesanan dapat diakses siapa saja" ON public.order_items FOR ALL USING (true);

-- ==========================================
-- DATA CONTOH SEEDING (MENU AWAL KAFE)
-- ==========================================
INSERT INTO public.menu_items (id, name, description, price, category, image_url) VALUES
('00000000-0000-0000-0000-000000000001', 'Kopi Susu Lavana', 'Kopi espresso susu gula aren segar gurih.', 28000.0, 'Minuman Dingin', 'https://images.unsplash.com/photo-1541167760496-1628856ab772?w=500'),
('00000000-0000-0000-0000-000000000002', 'Pandan Latte', 'Espresso premium dipadu susu segar & daun pandan.', 30000.0, 'Minuman Dingin', 'https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=500'),
('00000000-0000-0000-0000-000000000003', 'Iced Americano', 'Espresso dingin double-shot segar berenergi.', 25000.0, 'Minuman Dingin', 'https://images.unsplash.com/photo-1517701604599-bb29b565090c?w=500'),
('00000000-0000-0000-0000-000000000004', 'Espresso Romano', 'Espresso segar panas dengan irisan jeruk lemon.', 20000.0, 'Minuman Panas', 'https://images.unsplash.com/photo-1510707577719-ea7c182ac44d?w=500'),
('00000000-0000-0000-0000-000000000005', 'Kopi Tubruk', 'Kopi hitam tubruk robusta khas nusantara.', 18000.0, 'Minuman Panas', 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=500'),
('00000000-0000-0000-0000-000000000006', 'Rice Bowl Chicken Teriyaki', 'Nasi ayam saus teriyaki gurih & telur mata sapi.', 35000.0, 'Makanan', 'https://images.unsplash.com/photo-1512058564366-18510be2db19?w=500'),
('00000000-0000-0000-0000-000000000007', 'Nasi Goreng Retro', 'Nasi goreng spesial kecap manis dengan bumbu rahasia.', 30000.0, 'Makanan', 'https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=500'),
('00000000-0000-0000-0000-000000000008', 'Croissant Classic', 'Mentega premium, renyah di luar lembut di dalam.', 25000.0, 'Snack', 'https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=500'),
('00000000-0000-0000-0000-000000000009', 'Kentang Goreng', 'Kentang goreng renyah bumbu bawang garam laut.', 20000.0, 'Snack', 'https://images.unsplash.com/photo-1576107232684-1279f390859f?w=500');
