# Dokumentasi Lengkap Sistem Informasi Vanisha Bakery

Dokumen ini disusun sebagai panduan teknis dan penjelasan alur sistem aplikasi Vanisha Bakery untuk keperluan dokumentasi Skripsi.

---

## 1. Ikhtisar Proyek (Project Overview)
**Vanisha Bakery App** adalah sistem informasi manajemen penjualan dan distribusi berbasis mobile (Android) yang dirancang untuk mengoptimalkan pemantauan performa mitra (outlet) dan efisiensi kerja staf sales di lapangan. Sistem ini menggunakan arsitektur **Offline-First** untuk memastikan operasional tetap berjalan meskipun koneksi internet tidak stabil.

---

## 2. Arsitektur Teknologi (Tech Stack)
Aplikasi ini dibangun menggunakan standar pengembangan Android modern:
- **Bahasa Pemrograman**: Kotlin
- **Arsitektur**: MVVM (Model-View-ViewModel) – Memisahkan logika bisnis dari UI untuk kemudahan maintenance.
- **Database Lokal**: Room (SQLite) – Menyimpan data di memori internal HP agar aplikasi bisa digunakan secara offline.
- **Database Cloud**: Firebase Firestore – Sinkronisasi data real-time antar perangkat (Admin & Sales).
- **Penyimpanan Gambar**: Firebase Storage – Untuk menyimpan foto bukti pendaftaran mitra.
- **UI System**: Neo-Brutalist Design – Gaya desain modern dengan kontras tinggi, garis tegas (border), dan shadow tebal untuk keterbacaan yang maksimal.

---

## 3. Perancangan Database (Database Design)

Sistem membedakan data menjadi dua kategori utama:

### A. Data Master (Induk)
Data yang bersifat statis atau jarang berubah.
1. **Users**: Informasi akun Admin dan Sales (Nama, Email, Role, UID).
2. **Produk**: Katalog roti (Nama Roti, Harga Jual). *Note: Stok dihilangkan untuk efisiensi produksi harian.*
3. **Rute**: Daftar jalur distribusi yang tersedia (Nama Rute, Deskripsi).
4. **Mitra**: Daftar toko/warung pelanggan (Nama Toko, Lokasi GPS, Foto, Status Persetujuan).

### B. Data Transaksi
Data yang mencatat aktivitas harian.
1. **Stok Rute**: Catatan jumlah roti yang dibawa sales per rute setiap harinya (Reconsiliation).
2. **Penjualan**: Catatan transaksi harian per mitra (Jumlah laku, jumlah retur, total nominal).
3. **Log Aktivitas**: Catatan rekam jejak sistem (Siapa melakukan apa dan kapan).
4. **Notifikasi**: Pesan sistem untuk pemberitahuan pengajuan mitra atau status approval.

---

## 4. Alur Kerja Sistem (Workflow)

### Alur Kerja Admin (Pemilik/Koordinator)
1. **Manajemen Master**: Admin mengelola akun sales, rute, dan katalog harga produk.
2. **Approval Mitra**: Menerima notifikasi pengajuan mitra baru dari sales. Admin memverifikasi lokasi (via peta) dan foto, lalu mengubah status menjadi *Approved* atau *Rejected*.
3. **Pengaturan Distribusi**: Admin mengisi jumlah roti yang dibawa setiap sales (Stok Bawaan Rute) setiap harinya.
4. **Monitoring & Reporting**: 
   - Memantau lokasi mitra di peta secara real-time.
   - Menganalisis performa rute melalui **Heatmap** (Warna Hijau = Laku Keras, Kuning = Sedang, Merah = Kurang Laku).
   - Melihat laporan penjualan harian dan mingguan.

### Alur Kerja Sales (Staf Lapangan)
1. **Pendaftaran Mitra**: Menambahkan mitra baru langsung dari lokasi toko menggunakan GPS dan kamera HP. Data akan tersimpan sebagai *Pending*.
2. **Distribusi & Penjualan**: 
   - Melihat daftar produk yang harus dibawa hari itu (berdasarkan input Admin).
   - Melakukan kunjungan ke mitra dan menginput data penjualan (jumlah terjual & retur).
3. **Pemberitahuan**: Menerima notifikasi jika pengajuan mitranya sudah disetujui oleh Admin.

---

## 5. Fitur Unggulan (Key Features)

### A. Analisis Heatmap Performa
Sistem secara otomatis menghitung rasio penjualan:
`Rasio = (Roti Laku / Total Bawaan) * 100%`
- **Hijau (>= 80%)**: Performa sangat baik.
- **Kuning (50% - 79%)**: Performa standar.
- **Merah (< 50%)**: Performa rendah (perlu evaluasi rute).

### B. Offline-to-Online Sync
Jika Sales berada di area *blank spot* (tanpa sinyal):
1. Data pendaftaran mitra tetap tersimpan di database lokal (Room).
2. Begitu HP mendapatkan sinyal internet, sistem secara otomatis mengunggah data tersebut ke Firebase di latar belakang (*background sync*).

### C. Verifikasi GPS & Foto
Untuk mencegah data fiktif, Sales wajib mengambil foto toko dan koordinat GPS secara langsung di lokasi. Admin dapat memvalidasi keaslian data ini melalui peta di dashboard.

---

## 6. Kesimpulan Logika Skripsi
Aplikasi ini tidak hanya sekadar mencatat penjualan, tetapi berfungsi sebagai **Sistem Pendukung Keputusan (Decision Support System)** bagi pemilik Vanisha Bakery. Dengan adanya data yang akurat dan visualisasi Heatmap, pemilik dapat memutuskan rute mana yang harus diperbanyak stoknya dan rute mana yang perlu dievaluasi pemasarannya.
