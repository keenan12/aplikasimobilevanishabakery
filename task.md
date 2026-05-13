# Task List: Implementasi Skripsi Vanisha Bakery

## Phase 1: Persiapan & Struktur Data
- [ ] Review dan sesuaikan Entity `PenjualanEntity` untuk menampung data Kirim/Retur per jenis produk.
- [ ] Update `PenjualanDao` dengan query filter Mingguan/Bulanan.
- [ ] Pastikan sinkronisasi Firebase untuk data Penjualan berjalan lancar.

## Phase 2: UI Input Penjualan (Admin)
- [ ] Desain Layout Form Input Penjualan (Dropdown Rute -> Daftar Mitra).
- [ ] Implementasi Logika Auto-Calculate (Terjual = Kirim - Retur).
- [ ] Implementasi Bulk Insert/Update ke Local DB (Room).

## Phase 3: Visualisasi Heatmap (Google Maps)
- [ ] Integrasi data penjualan ke `MapsActivity`.
- [ ] Implementasi Logika Pewarnaan Marker (Hijau/Kuning/Merah).
- [ ] Optimasi Marker Clustering untuk 300+ titik mitra.

## Phase 4: Pelaporan Excel
- [ ] Integrasi Library Apache POI.
- [ ] Pembuatan `ExportExcelActivity` dengan filter Month/Year picker.
- [ ] Implementasi Logic Generate file Excel (.xlsx).
- [ ] Fitur Share/Open file Excel setelah berhasil dibuat.

## Phase 5: Testing & Polishing
- [ ] Simulasi input data masal (Stress Test Admin UI).
- [ ] Verifikasi akurasi data di Excel vs Database.
- [ ] Final UI Polish (Neo-Brutalism style).
