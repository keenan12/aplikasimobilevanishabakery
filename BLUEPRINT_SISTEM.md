# Blueprint Lengkap: Vanisha Bakery — Alur Sistem & Perancangan

> Dokumen ini menjawab SEMUA pertanyaan secara detail:
> 1. Bagaimana kisaran Heatmap (Hijau, Oren, Merah)
> 2. Tampilan di Google Maps seperti apa
> 3. Bagian Sales berubah jadi apa
> 4. Input Penjualan/Pengiriman yang sudah ada → dihapus atau tidak
> 5. Data tersimpan per bulan atau bagaimana (retensi data)

---

## 1. GAMBARAN BESAR SISTEM (The Big Picture)

```
┌─────────────────────────────────────────────────────────┐
│                    VANISHA BAKERY APP                     │
├──────────────────────┬──────────────────────────────────┤
│     ROLE: SALES      │        ROLE: ADMIN/OWNER         │
│  (Di Lapangan/Jalan) │        (Di Kantor/Rumah)         │
├──────────────────────┼──────────────────────────────────┤
│ ✅ Tambah Mitra Baru │ ✅ ACC/Tolak Mitra Baru          │
│ ✅ Foto Lokasi Toko  │ ✅ Input Nota Penjualan Mingguan │
│ ✅ Lihat Peta Rute   │ ✅ Lihat Heatmap Performa        │
│ ✅ Navigasi ke Mitra │ ✅ Download Laporan Excel        │
│ ❌ TIDAK input nota  │ ✅ Kelola Produk & Rute          │
│ ❌ TIDAK input data  │ ✅ Kelola User/Sales             │
└──────────────────────┴──────────────────────────────────┘
```

### Alur Mingguan (Siklus Bisnis Nyata):

| Hari | Siapa | Aktivitas |
|------|-------|-----------|
| Sen-Jum | Sales | Keliling rute → antar roti → tulis nota fisik → kumpulkan nota |
| Sabtu | Sales | Serahkan tumpukan nota fisik ke kantor |
| Sabtu-Minggu | Admin | Input data dari nota ke aplikasi (Filter Rute → Pilih Mitra → Isi Kirim/Retur → Simpan) |
| Minggu | Owner | Buka Heatmap → Lihat mitra merah → Evaluasi → Download Excel |

---

## 2. HEATMAP: Cara Kerja & Kisaran Warna

### Logika Perhitungan Warna

```
Rasio Laku (%) = (Total Terjual / Total Dikirim) × 100%
```

**Contoh Nyata:**
- Mitra "Toko Berkah" → Dikirim 100, Retur 10 → Terjual 90 → **Rasio = 90%** → 🟢 HIJAU
- Mitra "Toko Makmur" → Dikirim 100, Retur 35 → Terjual 65 → **Rasio = 65%** → 🟡 OREN
- Mitra "Toko Sepi" → Dikirim 100, Retur 60 → Terjual 40 → **Rasio = 40%** → 🔴 MERAH

### Tabel Kisaran Warna

| Warna | Kisaran Rasio | Arti | Aksi Owner |
|-------|--------------|------|------------|
| 🟢 **HIJAU** | **≥ 80%** laku | Toko produktif, retur sedikit | Pertahankan, prioritas kirim |
| 🟡 **OREN** | **50% – 79%** laku | Toko stabil tapi perlu perhatian | Monitor, evaluasi kuantitas kirim |
| 🔴 **MERAH** | **< 50%** laku | Retur banyak, penjualan rendah | Kurangi kirim / pertimbangkan putus mitra |

### Periode Heatmap
- Dihitung berdasarkan **data bulan berjalan** (default)
- Admin bisa filter per bulan untuk melihat tren historis

---

## 3. TAMPILAN DI GOOGLE MAPS

### Kondisi Saat Ini vs Sesudah Heatmap

**SEBELUM (sekarang):** Semua marker berwarna SAMA (Teal/Magenta berdasarkan status approval)

**SESUDAH (dengan Heatmap):** Marker berubah warna berdasarkan PERFORMA PENJUALAN:
- Laku ≥80%  → 🟢 PIN HIJAU
- Laku 50-79% → 🟡 PIN OREN/KUNING
- Laku <50%  → 🔴 PIN MERAH
- Belum ada data → ⚪ PIN ABU-ABU
- Pending (belum di-ACC) → 🟣 PIN MAGENTA

### Wireframe Tampilan Peta Heatmap

```
┌──────────────────────────────────────┐
│ [←]  Peta Mitra        [🗺️] [👤]   │
│                                      │
│  Chip: [Semua] [Rute1] [Rute2] ...  │
│  Filter Bulan: [◄ April 2026 ►]     │
│                                      │
│  ┌──────────────────────────────┐    │
│  │         GOOGLE MAPS          │    │
│  │                              │    │
│  │    🟢 Toko Berkah            │    │
│  │         🟡 Toko Makmur      │    │
│  │  🔴 Toko Sepi               │    │
│  │              🟢 Toko Jaya   │    │
│  │    ⚪ Toko Baru (blm data)  │    │
│  │                              │    │
│  └──────────────────────────────┘    │
│                                      │
│  ┌──────────────────────────────┐    │
│  │ Toko Berkah         🟢 90%  │    │
│  │ Sales: Andi | Rute: Cinere  │    │
│  │ Kirim: 100 | Laku: 90       │    │
│  │ [📍 Navigasi]               │    │
│  └──────────────────────────────┘    │
│                                      │
│  LEGENDA:                            │
│  🟢 ≥80%  🟡 50-79%  🔴 <50%       │
└──────────────────────────────────────┘
```

---

## 4. BAGIAN SALES — APA YANG BERUBAH?

### Rencana Perubahan

| Fitur | Sekarang | Rencana | Alasan |
|-------|----------|---------|--------|
| Input Pengiriman | ✅ Ada | ❌ **HAPUS dari Sales** | Admin yang input dari nota |
| Input Penjualan | ✅ Ada | ❌ **HAPUS dari Sales** | Admin yang input dari nota |
| Kelola Mitra | ✅ Ada | ✅ **TETAP** | Sales yang daftar mitra baru |
| Lihat Peta | ✅ Ada | ✅ **TETAP** | Sales lihat rute, navigasi |
| Lihat Dashboard | ✅ Ada | ✅ **TETAP (simplified)** | Ringkasan saja |

### Bottom Navigation Sales (Sesudah)

```
SEBELUM: [🏠 Home] [👥 Mitra] [➕] [📦 Pengiriman] [💰 Penjualan]
SESUDAH: [🏠 Home] [👥 Mitra] [🗺️ Peta]
```

---

## 5. INPUT PENJUALAN/PENGIRIMAN — BAGAIMANA?

**Kode TIDAK dihapus**, tapi **dipindahkan** agar hanya diakses oleh **Admin/Owner**.

Admin punya menu baru "Input Nota Mingguan" dengan UI Rapid Entry:

```
┌──────────────────────────────────────┐
│  INPUT NOTA MINGGUAN                 │
│  Rute: [▼ Pilih Rute          ]      │
│  Tanggal: [📅 11 Mei 2026     ]      │
│  ══════════════════════════════════   │
│  Mitra: Toko Berkah (1/30)     [→]   │
│  ──────────────────────────────────   │
│  │ Roti Tawar    Kirim:[__] Retur:[__]│
│  │ Roti Coklat   Kirim:[__] Retur:[__]│
│  ──────────────────────────────────   │
│  Auto: Terjual = Kirim - Retur       │
│  Auto: Omset = Rp 450.000           │
│  [💾 Simpan & Lanjut ke Mitra →]     │
│  Progress: ████░░░░░░ 10/30 mitra    │
└──────────────────────────────────────┘
```

---

## 6. RETENSI DATA — DATA TIDAK PERNAH HILANG

**Data TIDAK pernah dihapus otomatis.**

```
Firebase Firestore (Cloud) — PERMANEN
├── /users           → Data user
├── /mitra           → Data semua toko mitra
├── /produk          → Katalog produk roti
├── /rute            → Data rute pengiriman
├── /penjualan       → SEMUA data transaksi (terus bertambah)
├── /log_aktivitas   → Log aktivitas user
└── /notifikasi      → Notifikasi
```

| Kebutuhan | Cara Akses |
|-----------|-----------|
| Heatmap bulan ini | Filter tanggal = bulan berjalan |
| Heatmap bulan lalu | Filter tanggal = bulan sebelumnya |
| Download Excel bulanan | Filter tanggal = bulan dipilih |
| Lihat tren 6 bulan | Query 6 bulan terakhir → grafik |

Data **hanya dihapus manual** oleh Admin jika salah input.

---

## 7. DIAGRAM ALUR SISTEM LENGKAP

```
SALES (Lapangan)                    ADMIN (Kantor)
     │                                    │
     ▼                                    │
[Daftar Mitra Baru]                       │
  ├── Foto toko                           │
  ├── Titik GPS                           │
  └── Kirim ke Firebase ──────► [Notif: Mitra Pending]
                                          │
                                          ▼
                                   [ACC / Tolak Mitra]
                                          │
                                          ▼
                                   [Input Nota Mingguan]
                                     ├── Pilih Rute
                                     ├── Pilih Tanggal
                                     ├── Pilih Mitra
                                     ├── Input Kirim & Retur
                                     └── Simpan ke Firebase
                                          │
                              ┌───────────┼───────────┐
                              ▼           ▼           ▼
                         [HEATMAP]   [DASHBOARD]  [EXCEL]
                          Pin Warna   Chart+Omset  .xlsx
```

---

## OPEN QUESTIONS (Perlu Jawaban Anda)

1. **Threshold Heatmap**: Batas 80% (hijau) dan 50% (merah) sudah cocok? Atau mau angka lain?
2. **Menu Sales**: Setuju Sales hanya punya 3 menu (Home, Mitra, Peta)?
3. **Excel**: Mau download per minggu DAN per bulan? Atau cukup per bulan?
4. **Tabel**: Di sistem sekarang ada 2 tabel terpisah (pengiriman & penjualan). Untuk baru, sebaiknya digabung jadi 1 karena Admin input Kirim & Retur sekaligus. Setuju?
