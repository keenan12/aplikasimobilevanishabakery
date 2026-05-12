package id.mohamadsuhendy.vanishabakery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mitra")
data class MitraEntity(
    @PrimaryKey val id: String,
    val namaToko: String,
    val fotoUrl: String,
    val latitude: Double,
    val longitude: Double,
    val alamat: String,
    val status: String,
    val staffId: String,
    val staffNama: String,
    val ruteId: String = "",
    val ruteNama: String = "",
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
