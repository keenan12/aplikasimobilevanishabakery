package id.mohamadsuhendy.vanishabakery.utils

object Constants {
    // Firestore Collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_MITRA = "mitra"
    const val COLLECTION_PENGIRIMAN = "pengiriman"
    const val COLLECTION_PENJUALAN = "penjualan"
    const val COLLECTION_LOG_AKTIVITAS = "log_aktivitas"
    const val COLLECTION_RUTE = "rute"
    const val COLLECTION_PRODUK = "produk"
    const val COLLECTION_NOTIFIKASI = "notifikasi"

    // Mitra Status
    const val STATUS_PENDING = "pending"
    const val STATUS_APPROVED = "approved"
    const val STATUS_REJECTED = "rejected"
    const val STATUS_DELETED = "deleted"

    // User Roles
    const val ROLE_ADMIN = "admin"
    const val ROLE_STAFF = "staff"

    // Storage Paths
    const val STORAGE_MITRA_PHOTOS = "mitra_photos"
    const val STORAGE_PROFILE_PHOTOS = "profile_photos"

    // Intent Keys
    const val KEY_MITRA = "key_mitra"
    const val KEY_MITRA_ID = "key_mitra_id"
    const val KEY_RUTE_ID = "key_rute_id"
    const val KEY_USER_ID = "key_user_id"
    const val KEY_ROLE = "key_role"

    // SharedPreferences
    const val PREF_NAME = "vanisha_prefs"
    const val PREF_USER_ROLE = "pref_user_role"
    const val PREF_USER_ID = "pref_user_id"
    const val PREF_USER_NAME = "pref_user_name"

    // Log Activity Types
    const val LOG_TYPE_MITRA = "mitra"
    const val LOG_TYPE_PENGIRIMAN = "pengiriman"
    const val LOG_TYPE_PENJUALAN = "penjualan"
    const val LOG_TYPE_RUTE = "rute"
    const val LOG_TYPE_USER = "user"

    // Action Types
    const val ACTION_TAMBAH_MITRA = "Pengajuan Mitra Baru"
    const val ACTION_APPROVE_MITRA = "Setujui Mitra"
    const val ACTION_REJECT_MITRA = "Tolak Mitra"
    const val ACTION_INPUT_PENGIRIMAN = "Input Pengiriman"
    const val ACTION_INPUT_PENJUALAN = "Input Penjualan"
    const val ACTION_TAMBAH_RUTE = "Tambah Rute"
    const val ACTION_EDIT_RUTE = "Edit Rute"
    const val ACTION_HAPUS_RUTE = "Hapus Rute"
    const val ACTION_TAMBAH_USER = "Tambah User"
    const val ACTION_NONAKTIF_USER = "Nonaktifkan User"

    // Default sample routes (bisa diubah/ditambah oleh admin)
    val DEFAULT_RUTE_SALES_A = listOf("Cilandak", "Lebak Bulus", "Fatmawati", "TB Simatupang", "Pesanggrahan", "Kebayoran Lama")
    val DEFAULT_RUTE_SALES_B = listOf("Depok", "Citayam", "Bojong Gede", "Sawangan")

}
