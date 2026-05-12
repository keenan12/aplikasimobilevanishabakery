package id.mohamadsuhendy.vanishabakery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pengiriman")
data class PengirimanEntity(
    @PrimaryKey val id: String,
    val mitraId: String,
    val mitraNama: String,
    val namaProduk: String,
    val jumlah: Int,
    val tanggal: Long,
    val staffId: String,
    val staffNama: String,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
