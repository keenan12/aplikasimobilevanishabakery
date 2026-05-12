package id.mohamadsuhendy.vanishabakery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "penjualan")
data class PenjualanEntity(
    @PrimaryKey val id: String,
    val pengirimanId: String,
    val mitraId: String,
    val mitraNama: String,
    val namaProduk: String,
    val jumlahDikirim: Int,
    val jumlahTerjual: Int,
    val jumlahSisa: Int,
    val tanggal: Long,
    val staffId: String,
    val staffNama: String,
    val hargaSatuan: Int = 0,
    val totalHarga: Int = 0,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
