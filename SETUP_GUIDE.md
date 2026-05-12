# Setup Guide — Vanisha Bakery Android App

Ikuti langkah-langkah berikut sebelum menjalankan aplikasi di Android Studio.

---

## 1. 🔥 Setup Firebase

### a. Buat Firebase Project
1. Buka [Firebase Console](https://console.firebase.google.com/)
2. Klik **Add Project** → nama: `Vanisha Bakery`
3. Enable **Google Analytics** (opsional)

### b. Daftarkan Android App
1. Klik ikon Android di Firebase Console
2. **Package name**: `id.mohamadsuhendy.vanishabakery`
3. Download file **`google-services.json`**
4. Letakkan di: `app/google-services.json`

### c. Aktifkan Layanan Firebase
Di Firebase Console, aktifkan:
- **Authentication** → Email/Password provider → Enable
- **Firestore Database** → Create in test mode
- **Storage** → Start in test mode

### d. Buat User Pertama (Admin)
Di Firebase Console → Authentication → Add User:
- Email: `muhammadhendi070@gmail.com`
- Password: (terserah)

Lalu di Firestore → Collection `users` → Add Document:
```
Document ID: [UID dari user yang baru dibuat]
Fields:
  uid: [UID]
  nama: "Administrator"
  email: "muhammadhendi070@gmail.com"
  role: "admin"
  isActive: true
  createdAt: [Timestamp sekarang]
```

---

## 2. 🗺️ Setup Google Maps API

1. Buka [Google Cloud Console](https://console.cloud.google.com/)
2. Aktifkan **Maps SDK for Android** dan **Geocoding API**
3. Buat API Key → copy key-nya
4. Buka `AndroidManifest.xml` → cari:
   ```xml
   android:value="AIzaSyC2AneE5VE20hGVB94Xr08vf098KurE5Wk"
   ```
   Ganti dengan API Key Anda.

---

## 3. 📋 Struktur Firestore Collections

Buat collections berikut di Firestore (akan auto-terbuat saat data pertama disimpan):

| Collection | Deskripsi |
|---|---|
| `users` | Data user (admin & staff) |
| `mitra` | Data toko/mitra |
| `pengiriman` | Catatan pengiriman roti |
| `penjualan` | Catatan penjualan |
| `log_aktivitas` | Log semua aktivitas |
| `rute` | Rute distribusi (future) |

### Firestore Security Rules (Development)
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

---

## 4. 📱 Menjalankan Aplikasi

```bash
# Sync Gradle
File → Sync Project with Gradle Files

# Build & Run
Run → Run 'app'  (Shift+F10)
```

**Minimum SDK**: API 24 (Android 7.0)  
**Target SDK**: API 35 (Android 15)

---

## 5. 🏗️ Struktur Project

```
app/src/main/java/id/mohamadsuhendy/vanishabakery/
├── VanishaBakeryApp.kt          ← Application class (Service Locator)
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt       ← Room Database
│   │   ├── dao/                 ← MitraDao, PengirimanDao, PenjualanDao
│   │   └── entity/              ← Room Entities (offline cache)
│   ├── model/                   ← Data models (Firestore models)
│   ├── remote/
│   │   └── FirebaseDataSource.kt ← All Firebase operations
│   └── repository/              ← AuthRepository, MitraRepository, dll
├── ui/
│   ├── auth/                    ← LoginActivity + LoginViewModel
│   ├── main/                    ← MainActivity (Bottom Nav + FAB)
│   ├── home/                    ← HomeFragment + HomeViewModel
│   ├── mitra/                   ← MitraFragment, Detail, AddBottomSheet
│   ├── pengiriman/              ← AddPengirimanBottomSheet + ViewModel
│   ├── penjualan/               ← AddPenjualanBottomSheet + ViewModel
│   ├── aktivitas/               ← AktivitasFragment + Adapter + ViewModel
│   ├── profile/                 ← ProfileFragment + ProfileViewModel
│   └── bottomsheet/             ← ActionBottomSheetFragment (FAB menu)
└── utils/
    ├── Constants.kt             ← App-wide constants
    ├── Extensions.kt            ← Kotlin extension functions
    ├── NetworkUtils.kt          ← Online/offline detection (Flow)
    └── Result.kt                ← Sealed class untuk state management
```

---

## 6. 🔑 Role & Akses

| Fitur | Admin | Staff |
|---|---|---|
| Login | ✅ | ✅ |
| Tambah Mitra | ✅ | ✅ |
| Approve/Tolak Mitra | ✅ | ❌ |
| Input Pengiriman | ✅ | ✅ |
| Input Penjualan | ✅ | ✅ |
| Kelola User | ✅ | ❌ |
| Lihat Semua Data | ✅ | ✅ |

---

## 7. 📶 Fitur Offline

Ketika tidak ada koneksi internet:
- Data mitra/pengiriman/penjualan tersimpan ke **Room Database** lokal
- Field `is_synced = false` menandai data belum sinkron
- Saat koneksi kembali online → **auto-sync** ke Firestore berjalan otomatis

---

## 8. ⚠️ Catatan Penting

- Pastikan `google-services.json` sudah ada di folder `app/` sebelum build
- Pastikan API Key Maps sudah benar di `AndroidManifest.xml`
- CircleImageView, Glide, dan dependency lain akan didownload saat Gradle sync
- Untuk production, update Firestore Security Rules sesuai kebutuhan
