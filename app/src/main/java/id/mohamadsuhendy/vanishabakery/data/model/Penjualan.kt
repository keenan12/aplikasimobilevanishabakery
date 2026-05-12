package id.mohamadsuhendy.vanishabakery.data.model

import com.google.firebase.Timestamp

data class Penjualan(
    val id: String = "",
    val pengirimanId: String = "",
    val mitraId: String = "",
    val mitraNama: String = "",
    val namaProduk: String = "",
    val jumlahDikirim: Int = 0,
    val jumlahTerjual: Int = 0,
    val jumlahSisa: Int = 0,
    val tanggal: Timestamp? = null,
    val staffId: String = "",
    val staffNama: String = "",
    val hargaSatuan: Int = 0,
    val totalHarga: Int = 0,
    val isSynced: Boolean = true,
    val createdAt: Timestamp? = null
)
