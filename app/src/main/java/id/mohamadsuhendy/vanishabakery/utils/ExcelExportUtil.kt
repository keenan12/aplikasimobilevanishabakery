package id.mohamadsuhendy.vanishabakery.utils

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import id.mohamadsuhendy.vanishabakery.data.model.Penjualan
import id.mohamadsuhendy.vanishabakery.data.model.StokRute
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility untuk generate file .xls dari data penjualan.
 * Menggunakan HTML-table approach yang 100% kompatibel dengan Android
 * dan dapat dibuka oleh Microsoft Excel, Google Sheets, WPS Office, dll.
 */
object ExcelExportUtil {

    data class ExportResult(
        val file: File,
        val totalMitra: Int,
        val totalKirim: Int,
        val totalTerjual: Int,
        val totalOmset: Int
    )

    /**
     * Generate laporan Excel per bulan.
     * @param context Android context
     * @param penjualanList Data penjualan yang sudah difilter per periode
     * @param periodLabel Label periode (misal: "Mei 2026")
     */
    fun generateLaporan(
        context: Context,
        penjualanList: List<Penjualan>,
        periodLabel: String,
        stokRuteList: List<StokRute> = emptyList()
    ): ExportResult {
        val grouped = penjualanList.groupBy { it.mitraId }
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        val sdfFull = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID"))
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))

        var totalKirim = 0
        var totalRetur = 0
        var totalTerjual = 0
        var totalOmset = 0

        val html = buildString {
            append("""
                <html xmlns:o="urn:schemas-microsoft-com:office:office" 
                      xmlns:x="urn:schemas-microsoft-com:office:excel">
                <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Calibri, Arial, sans-serif; }
                    table { border-collapse: collapse; width: 100%; }
                    th { 
                        background-color: #00695C; color: white; 
                        font-weight: bold; text-align: center;
                        border: 1px solid #333; padding: 8px; 
                    }
                    td { 
                        border: 1px solid #999; padding: 6px; 
                        text-align: center; 
                    }
                    .left { text-align: left; }
                    .right { text-align: right; }
                    .total-row { 
                        background-color: #FFF9C4; font-weight: bold; 
                    }
                    .title { 
                        font-size: 16pt; font-weight: bold; 
                        text-align: center; padding: 8px; 
                    }
                    .subtitle { 
                        font-size: 11pt; text-align: center; 
                        padding: 4px; 
                    }
                    .mitra-first { background-color: #F5F5F5; }
                </style>
                </head>
                <body>
            """.trimIndent())

            // Title
            append("<table>")
            append("<tr><td colspan='9' class='title'>LAPORAN PENJUALAN VANISHA BAKERY</td></tr>")
            append("<tr><td colspan='9' class='subtitle'>Periode: $periodLabel</td></tr>")
            append("<tr><td colspan='9' class='subtitle'>Waktu Unduh Laporan: ${sdfFull.format(Date())}</td></tr>")
            append("<tr><td colspan='9'>&nbsp;</td></tr>")

            // Headers
            append("<tr>")
            append("<th>No</th>")
            append("<th>Nama Mitra</th>")
            append("<th>Rute</th>")
            append("<th>Tanggal</th>")
            append("<th>Produk</th>")
            append("<th>Kirim</th>")
            append("<th>Retur</th>")
            append("<th>Terjual</th>")
            append("<th>Omset (Rp)</th>")
            append("</tr>")

            // Data rows
            var mitraNo = 0
            grouped.forEach { (_, sales) ->
                mitraNo++
                val mitraNama = sales.firstOrNull()?.mitraNama ?: ""
                val ruteNama = sales.firstOrNull()?.ruteNama ?: ""

                // Sort berdasarkan tanggal (terlama ke terbaru) kemudian harga
                val sortedSales = sales.sortedWith(compareBy({ it.tanggalNota }, { -it.hargaSatuan }))
                sortedSales.forEachIndexed { idx, p ->
                    val rowClass = if (idx == 0) "mitra-first" else ""
                    append("<tr class='$rowClass'>")

                    if (idx == 0) {
                        append("<td>${mitraNo}</td>")
                        append("<td class='left'>${mitraNama}</td>")
                        append("<td class='left'>${ruteNama}</td>")
                    } else {
                        append("<td></td><td></td><td></td>")
                    }

                    val tglStr = p.tanggalNota?.let { sdfDate.format(it.toDate()) } ?: "-"
                    append("<td>$tglStr</td>")

                    append("<td class='left'>${p.namaProduk}</td>")
                    append("<td>${p.jumlahKirim}</td>")
                    append("<td>${p.jumlahRetur}</td>")
                    append("<td>${p.jumlahTerjual}</td>")
                    append("<td class='right'>${formatter.format(p.totalHarga).replace(",00", "")}</td>")
                    append("</tr>")

                    totalKirim += p.jumlahKirim
                    totalRetur += p.jumlahRetur
                    totalTerjual += p.jumlahTerjual
                    totalOmset += p.totalHarga
                }
            }

            // Total row
            append("<tr class='total-row'>")
            append("<td colspan='5'>TOTAL</td>")
            append("<td>$totalKirim</td>")
            append("<td>$totalRetur</td>")
            append("<td>$totalTerjual</td>")
            append("<td class='right'>${formatter.format(totalOmset).replace(",00", "")}</td>")
            append("</tr>")

            append("</table>")

            // Summary section
            val rasio = if (totalKirim > 0) (totalTerjual.toFloat() / totalKirim * 100) else 0f
            append("<br><table>")
            append("<tr><td colspan='2' style='font-weight:bold; background-color:#E0F2F1; padding:8px;'>RINGKASAN</td></tr>")
            append("<tr><td style='padding:4px;'>Total Mitra</td><td style='padding:4px;'>${grouped.size}</td></tr>")
            append("<tr><td style='padding:4px;'>Total Kirim</td><td style='padding:4px;'>$totalKirim pcs</td></tr>")
            append("<tr><td style='padding:4px;'>Total Terjual</td><td style='padding:4px;'>$totalTerjual pcs</td></tr>")
            append("<tr><td style='padding:4px;'>Total Retur</td><td style='padding:4px;'>$totalRetur pcs</td></tr>")
            append("<tr><td style='padding:4px;'>Rasio Laku</td><td style='padding:4px;'>${String.format("%.1f", rasio)}%</td></tr>")
            append("<tr><td style='padding:4px; font-weight:bold;'>Total Omset</td><td style='padding:4px; font-weight:bold;'>${formatter.format(totalOmset).replace(",00", "")}</td></tr>")
            append("</table>")

            // REKONSILIASI STOK (Bawa vs Laku+Retur)
            if (stokRuteList.isNotEmpty()) {
                append("<br><br>")
                append("<table>")
                append("<tr><td colspan='6' style='font-weight:bold; background-color:#FFECB3; padding:8px;'>REKONSILIASI STOK RUTE (DISPATCH vs REALISASI)</td></tr>")
                append("<tr style='background-color:#FFF8E1;'>")
                append("<th>Rute</th>")
                append("<th>Produk</th>")
                append("<th>Total Bawa</th>")
                append("<th>Total Laku</th>")
                append("<th>Total Retur</th>")
                append("<th>Selisih (Hilang)</th>")
                append("</tr>")

                // Group Penjualan by Rute and Product
                val salesByRuteProduk = penjualanList.groupBy { it.ruteId to it.namaProduk }
                
                // Group StokBawaan by Rute and Product
                val bawaanByRuteProduk = mutableMapOf<Pair<String, String>, Int>()
                stokRuteList.forEach { stok ->
                    stok.stokData.forEach { (produk, qty) ->
                        val key = stok.ruteId to produk
                        bawaanByRuteProduk[key] = (bawaanByRuteProduk[key] ?: 0) + qty
                    }
                }

                // Iterate through all found keys
                val allKeys = (salesByRuteProduk.keys + bawaanByRuteProduk.keys).distinct().sortedBy { it.first }
                
                allKeys.forEach { key ->
                    val ruteId = key.first
                    val produkNama = key.second
                    
                    val salesItems = salesByRuteProduk[key] ?: emptyList()
                    val totalLaku = salesItems.sumOf { it.jumlahTerjual }
                    val totalRetur = salesItems.sumOf { it.jumlahRetur }
                    val totalBawa = bawaanByRuteProduk[key] ?: 0
                    
                    val selisih = totalBawa - (totalLaku + totalRetur)
                    val ruteNama = salesItems.firstOrNull()?.ruteNama ?: stokRuteList.find { it.ruteId == ruteId }?.ruteNama ?: ruteId

                    append("<tr>")
                    append("<td class='left'>$ruteNama</td>")
                    append("<td class='left'>$produkNama</td>")
                    append("<td>$totalBawa</td>")
                    append("<td>$totalLaku</td>")
                    append("<td>$totalRetur</td>")
                    
                    val style = if (selisih != 0) "style='color:red; font-weight:bold;'" else ""
                    append("<td $style>$selisih</td>")
                    append("</tr>")
                }
                append("</table>")
            }

            append("</body></html>")
        }

        // Save as .xls (Excel reads HTML tables natively)
        val timestamp = System.currentTimeMillis()
        val fileName = "Laporan_VanishaBakery_${periodLabel.replace(" ", "_")}_$timestamp.xls"
        
        val dir = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "VanishaBakery")
        } else {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "VanishaBakery")
        }
        
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)

        FileOutputStream(file).use { fos ->
            fos.write(html.toByteArray(Charsets.UTF_8))
        }

        return ExportResult(
            file = file,
            totalMitra = grouped.size,
            totalKirim = totalKirim,
            totalTerjual = totalTerjual,
            totalOmset = totalOmset
        )
    }

    /**
     * Share file Excel via intent (WhatsApp, Email, dll)
     */
    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.ms-excel"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Penjualan Vanisha Bakery")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Laporan"))
    }
}
