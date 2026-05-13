package id.mohamadsuhendy.vanishabakery.data.model

import com.google.firebase.Timestamp

data class Penjualan(
    val id: String = "",
    val mitraId: String = "",
    val mitraNama: String = "",
    val ruteId: String = "",
    val ruteNama: String = "",
    val namaProduk: String = "",
    val jumlahKirim: Int = 0,
    val jumlahRetur: Int = 0,
    val jumlahTerjual: Int = 0,       // auto: kirim - retur
    val hargaSatuan: Int = 0,
    val totalHarga: Int = 0,          // auto: terjual * hargaSatuan
    val tanggalNota: Timestamp? = null, // tanggal nota fisik (dari Sales)
    val inputOleh: String = "",       // UID admin yang menginput
    val inputOlehNama: String = "",   // Nama admin yang menginput
    val isSynced: Boolean = true,
    val createdAt: Timestamp? = null   // timestamp saat data diinput ke sistem
)
