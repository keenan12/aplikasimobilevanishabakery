# Rencana Implementasi Skripsi: Vanisha Bakery

Rancang Bangun Aplikasi Mobile Manajemen Mitra GPS Heatmap untuk UMKM Roti Konsinyasi Berbasis Firebase.

## Latar Belakang & Tujuan
Aplikasi ini dirancang untuk mengatasi kendala operasional pada UMKM Vanisha Bakery, khususnya dalam hal pemantauan lokasi mitra (toko) dan efisiensi pelaporan penjualan mingguan. Sistem ini menggabungkan pencatatan data transaksi manual dengan visualisasi data geografis (Heatmap) untuk mendukung keputusan bisnis owner.

## User Review Required

> [!IMPORTANT]
> **Keputusan Strategis: Input Penjualan oleh Admin**
> Data penjualan tidak diinput oleh Sales di lapangan untuk menghindari "kerja dua kali" (nota fisik + aplikasi) dan kendala jaringan. Admin akan menginput data rekapitulasi nota secara mingguan di kantor.

> [!TIP]
> **Arsitektur Offline-First**
> Menggunakan Room Database sebagai penyimpanan lokal utama agar aplikasi tetap responsif saat input data masal, kemudian disinkronisasikan ke Firebase di latar belakang.

## Alur Kerja Aktor (Role-Based Workflow)

### 1. Sales (Lapangan)
- **Verifikasi Lokasi**: Memastikan setiap mitra baru memiliki titik koordinat GPS yang valid dan foto lokasi untuk mencegah mitra fiktif.
- **Monitoring Rute**: Melihat daftar mitra dan lokasi mereka di peta untuk efisiensi perjalanan.

### 2. Admin / Owner (Kantor)
- **Input Nota Mingguan**: Menginput data `Kirim` dan `Retur` dari nota fisik ke aplikasi.
- **Monitoring Performa (Heatmap)**: Melihat peta dengan penanda warna (Hijau/Kuning/Merah) untuk evaluasi cepat performa mitra.
- **Pelaporan (Excel)**: Men-download rekapitulasi penjualan mingguan atau bulanan dalam format Excel.

---

## Modul Teknis Utama

### 1. Modul Input Penjualan Cepat (Rapid Entry)
Optimasi UI agar Admin bisa menginput ratusan nota dengan cepat.
- **Fitur**: Filter Rute -> Pilih Mitra -> Input Angka Kirim/Retur -> Auto-Calculate Terjual & Omset -> Simpan.

### 2. Modul Visualisasi Heatmap (Google Maps)
Visualisasi performa mitra berbasis warna pada peta.
- **Logika Warna**:
    - 🟢 **Hijau**: Penjualan tinggi (> target rute).
    - 🟡 **Kuning**: Penjualan rata-rata (normal).
    - 🔴 **Merah**: Penjualan rendah / retur tinggi.
- **Tech**: Google Maps SDK for Android + Marker Clustering.

### 3. Modul Ekspor Laporan (Excel POI)
Mengubah data digital di Firebase/Room menjadi file Excel yang siap pakai.
- **Fitur**: Filter periode (Mingguan/Bulanan), Generate file .xlsx, Share/Simpan file.
- **Tech**: Apache POI Library.

---

## Detail Teknis & Arsitektur

### 1. Konsep Skema Database (Firebase & Room)
Untuk skripsi, Anda perlu menjelaskan hubungan data (ERD). Berikut adalah gambaran datanya:
*   **Table Mitra**: Menyimpan `id`, `nama`, `alamat`, `koordinat (lat, lng)`, `ruteId`, dan `status`.
*   **Table Produk**: Menyimpan `id`, `namaProduk`, dan `harga`.
*   **Table Penjualan**: Ini tabel utamanya.
    *   `id`, `mitraId` (Relasi ke Mitra), `tanggal`.
    *   `listProduk`: (Rincian per roti: `nama`, `kirim`, `retur`, `laku`).
    *   `totalOmset`: (Hasil hitung otomatis).
    *   `isSynced`: (Status sinkronisasi ke Firebase).

### 2. Alur UI/UX: Proses Input Admin (Weekly)
1.  **Dashboard**: Admin klik tombol "Input Penjualan Mingguan".
2.  **Filter**: Pilih **Rute** dan **Tanggal**.
3.  **Daftar Mitra**: Muncul daftar semua mitra di rute tersebut yang belum diinput.
4.  **Form Cepat**: Admin isi angka `Kirim` & `Retur`. 
5.  **Auto-Save**: Klik "Simpan", sistem kembali ke daftar untuk mitra selanjutnya.

### 3. Logika Heatmap (Warna Penanda)
Sistem membandingkan performa terhadap target atau rata-rata:
- **Tercapai (>80%)**: 🟢 **Hijau**. Toko produktif, retur sedikit.
- **Normal (50% - 80%)**: 🟡 **Kuning**. Toko stabil.
- **Rendah (<50%)**: 🔴 **Merah**. Retur banyak atau penjualan rendah.

### 4. Struktur Laporan Excel (Output)
Kolom Excel: `No | Nama Mitra | Rute | Jenis Roti A (Kirim/Retur/Laku) | Jenis Roti B (Kirim/Retur/Laku) | Total Omset`.

---

## Penutup
Rencana ini sudah mencakup seluruh aspek kebutuhan skripsi Anda: dari masalah operasional hingga hasil akhir (Excel). Anda bisa menyimpan file ini sebagai acuan utama pengerjaan kode atau laporan skripsi.
