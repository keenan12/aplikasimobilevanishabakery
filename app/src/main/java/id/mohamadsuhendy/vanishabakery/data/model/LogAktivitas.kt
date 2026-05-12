package id.mohamadsuhendy.vanishabakery.data.model

import com.google.firebase.Timestamp

data class LogAktivitas(
    val id: String = "",
    val userId: String = "",
    val userNama: String = "",
    val userRole: String = "",
    val aksi: String = "",       // e.g. "Tambah Mitra", "Input Pengiriman"
    val deskripsi: String = "",  // e.g. "Menambah mitra Toko Mawar"
    val referensiId: String = "",// ID of related document
    val tipe: String = "",       // "mitra", "pengiriman", "penjualan"
    val createdAt: Timestamp? = null
)
