package id.mohamadsuhendy.vanishabakery.data.model

import com.google.firebase.Timestamp

data class StokRute(
    val id: String = "",
    val ruteId: String = "",
    val ruteNama: String = "",
    val tanggal: Timestamp? = null,
    val stokData: Map<String, Int> = emptyMap(), // Nama Produk -> Jumlah
    val inputOleh: String = "",
    val inputOlehNama: String = "",
    val createdAt: Timestamp? = null
)
