package id.mohamadsuhendy.vanishabakery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "penjualan")
data class PenjualanEntity(
    @PrimaryKey val id: String,
    val mitraId: String,
    val mitraNama: String,
    val ruteId: String = "",
    val ruteNama: String = "",
    val namaProduk: String,
    val jumlahKirim: Int,
    val jumlahRetur: Int,
    val jumlahTerjual: Int,
    val hargaSatuan: Int = 0,
    val totalHarga: Int = 0,
    val tanggalNota: Long,           // epoch millis of the physical note date
    val inputOleh: String = "",
    val inputOlehNama: String = "",
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
