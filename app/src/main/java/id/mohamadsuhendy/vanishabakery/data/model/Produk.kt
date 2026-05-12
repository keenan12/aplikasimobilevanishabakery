package id.mohamadsuhendy.vanishabakery.data.model

import com.google.firebase.Timestamp

data class Produk(
    val id: String = "",
    val nama: String = "",
    val harga: Int = 0,
    val stok: Int = 0,
    val deskripsi: String = "",
    val fotoUrl: String = "",
    val isActive: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
