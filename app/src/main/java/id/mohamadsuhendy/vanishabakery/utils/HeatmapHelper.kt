package id.mohamadsuhendy.vanishabakery.utils

import android.graphics.Color
import id.mohamadsuhendy.vanishabakery.data.model.Penjualan

/**
 * Helper untuk menghitung warna heatmap berdasarkan performa penjualan mitra.
 *
 * Logika:
 *   Rasio Laku = Total Terjual / Total Kirim × 100%
 *   🟢 Hijau  : rasio >= 80%
 *   🟡 Oren   : rasio 50% - 79%
 *   🔴 Merah  : rasio < 50%
 *   ⚪ Abu-abu : belum ada data
 */
object HeatmapHelper {

    enum class HeatmapLevel {
        GREEN,   // >= 80% laku
        ORANGE,  // 50-79% laku
        RED,     // < 50% laku
        GRAY     // belum ada data
    }

    data class MitraPerforma(
        val mitraId: String,
        val mitraNama: String,
        val totalKirim: Int,
        val totalRetur: Int,
        val totalTerjual: Int,
        val totalOmset: Int,
        val rasioLaku: Float,  // 0.0 - 100.0
        val level: HeatmapLevel
    )

    /**
     * Hitung performa per mitra dari data penjualan dalam periode tertentu.
     * Mengembalikan Map<mitraId, MitraPerforma>
     */
    fun calculatePerforma(penjualanList: List<Penjualan>): Map<String, MitraPerforma> {
        // Group by mitraId
        val grouped = penjualanList.groupBy { it.mitraId }

        return grouped.mapValues { (mitraId, sales) ->
            val totalKirim = sales.sumOf { it.jumlahKirim }
            val totalRetur = sales.sumOf { it.jumlahRetur }
            val totalTerjual = sales.sumOf { it.jumlahTerjual }
            val totalOmset = sales.sumOf { it.totalHarga }

            val rasio = if (totalKirim > 0) {
                (totalTerjual.toFloat() / totalKirim.toFloat()) * 100f
            } else 0f

            val level = when {
                totalKirim == 0 -> HeatmapLevel.GRAY
                rasio >= Constants.HEATMAP_GREEN_THRESHOLD -> HeatmapLevel.GREEN
                rasio >= Constants.HEATMAP_RED_THRESHOLD -> HeatmapLevel.ORANGE
                else -> HeatmapLevel.RED
            }

            MitraPerforma(
                mitraId = mitraId,
                mitraNama = sales.firstOrNull()?.mitraNama ?: "",
                totalKirim = totalKirim,
                totalRetur = totalRetur,
                totalTerjual = totalTerjual,
                totalOmset = totalOmset,
                rasioLaku = rasio,
                level = level
            )
        }
    }

    /** Get color int for heatmap level */
    fun getColor(level: HeatmapLevel): Int = when (level) {
        HeatmapLevel.GREEN -> Color.parseColor("#4CAF50")   // Material Green
        HeatmapLevel.ORANGE -> Color.parseColor("#FF9800")  // Material Orange
        HeatmapLevel.RED -> Color.parseColor("#F44336")     // Material Red
        HeatmapLevel.GRAY -> Color.parseColor("#9E9E9E")    // Material Gray
    }

    /** Get hex color string for heatmap level */
    fun getColorHex(level: HeatmapLevel): String = when (level) {
        HeatmapLevel.GREEN -> "#4CAF50"
        HeatmapLevel.ORANGE -> "#FF9800"
        HeatmapLevel.RED -> "#F44336"
        HeatmapLevel.GRAY -> "#9E9E9E"
    }

    /** Get label text for heatmap level */
    fun getLabel(level: HeatmapLevel): String = when (level) {
        HeatmapLevel.GREEN -> "Produktif"
        HeatmapLevel.ORANGE -> "Normal"
        HeatmapLevel.RED -> "Rendah"
        HeatmapLevel.GRAY -> "Belum Ada Data"
    }
}
