package id.mohamadsuhendy.vanishabakery.data.model

import com.google.firebase.Timestamp

data class Pengiriman(
    val id: String = "",
    val mitraId: String = "",
    val mitraNama: String = "",
    val ruteId: String = "",         // rute mitra ini
    val ruteNama: String = "",
    val namaProduk: String = "",
    val jumlah: Int = 0,
    val tanggal: Timestamp? = null,
    val staffId: String = "",
    val staffNama: String = "",
    val isSynced: Boolean = true,
    val createdAt: Timestamp? = null
)
