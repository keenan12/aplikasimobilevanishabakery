package id.mohamadsuhendy.vanishabakery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rute")
data class RuteEntity(
    @PrimaryKey val id: String,
    val namaRute: String,
    val kode: String,
    val staffId: String,
    val staffNama: String,
    val totalMitra: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
