package id.mohamadsuhendy.vanishabakery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stok_rute")
data class StokRuteEntity(
    @PrimaryKey val id: String,
    val ruteId: String,
    val ruteNama: String,
    val tanggal: Long,           // Epoch millis of the dispatch date
    val stokDataJson: String,    // JSON string: Map<String, Int> (ProdukNama -> Jumlah)
    val inputOleh: String = "",
    val inputOlehNama: String = "",
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
