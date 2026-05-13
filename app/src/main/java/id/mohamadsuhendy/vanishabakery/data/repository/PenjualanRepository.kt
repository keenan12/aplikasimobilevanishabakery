package id.mohamadsuhendy.vanishabakery.data.repository

import com.google.firebase.Timestamp
import id.mohamadsuhendy.vanishabakery.data.local.dao.PenjualanDao
import id.mohamadsuhendy.vanishabakery.data.local.entity.PenjualanEntity
import id.mohamadsuhendy.vanishabakery.data.model.LogAktivitas
import id.mohamadsuhendy.vanishabakery.data.model.Penjualan
import id.mohamadsuhendy.vanishabakery.data.remote.FirebaseDataSource
import id.mohamadsuhendy.vanishabakery.utils.Constants
import id.mohamadsuhendy.vanishabakery.utils.NetworkUtils
import id.mohamadsuhendy.vanishabakery.utils.Result
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class PenjualanRepository(
    private val remote: FirebaseDataSource,
    private val dao: PenjualanDao,
    private val network: NetworkUtils
) {
    // ── Observe semua penjualan (Admin) ──────────────────────────
    fun observeAllPenjualan(): Flow<List<Penjualan>> = remote.observeAllPenjualan()

    // ── Offline Room ─────────────────────────────────────────────
    fun getLocalPenjualan(): Flow<List<PenjualanEntity>> = dao.getAllPenjualan()

    fun getLocalPenjualanByPeriode(startDate: Long, endDate: Long): Flow<List<PenjualanEntity>> =
        dao.getPenjualanByPeriode(startDate, endDate)

    fun getLocalPenjualanByMitraPeriode(mitraId: String, startDate: Long, endDate: Long): Flow<List<PenjualanEntity>> =
        dao.getPenjualanByMitraPeriode(mitraId, startDate, endDate)

    fun getTodaySales(): Flow<Int?> {
        val (start, end) = getTodayRange()
        return dao.getTodaySales(start, end)
    }

    fun getTodaySalesByStaff(staffId: String): Flow<Int?> {
        val (start, end) = getTodayRange()
        return dao.getTodaySalesByStaff(start, end, staffId)
    }

    fun getOmsetByPeriode(startDate: Long, endDate: Long): Flow<Int?> =
        dao.getOmsetByPeriode(startDate, endDate)

    private fun getTodayRange(): Pair<Long, Long> {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val end = start + 86_400_000L
        return Pair(start, end)
    }

    // ── Rapid Entry: Simpan batch penjualan untuk 1 mitra ───────
    suspend fun saveBatchPenjualan(
        mitraId: String,
        mitraNama: String,
        ruteId: String,
        ruteNama: String,
        tanggalNota: Long,
        items: List<PenjualanItem>,
        adminId: String,
        adminNama: String
    ): Result<String> {
        return try {
            val now = Timestamp.now()
            val tanggalTs = Timestamp(java.util.Date(tanggalNota))
            var savedCount = 0

            items.forEach { item ->
                // Skip produk yang kirim=0 dan retur=0
                if (item.kirim <= 0 && item.retur <= 0) return@forEach

                val terjual = maxOf(0, item.kirim - item.retur)
                val totalHarga = terjual * item.hargaSatuan
                val docId = UUID.randomUUID().toString()

                val penjualan = Penjualan(
                    mitraId = mitraId, mitraNama = mitraNama,
                    ruteId = ruteId, ruteNama = ruteNama,
                    namaProduk = item.namaProduk,
                    jumlahKirim = item.kirim,
                    jumlahRetur = item.retur,
                    jumlahTerjual = terjual,
                    hargaSatuan = item.hargaSatuan,
                    totalHarga = totalHarga,
                    tanggalNota = tanggalTs,
                    inputOleh = adminId, inputOlehNama = adminNama,
                    isSynced = true, createdAt = now
                )

                val entity = PenjualanEntity(
                    id = docId, mitraId = mitraId, mitraNama = mitraNama,
                    ruteId = ruteId, ruteNama = ruteNama,
                    namaProduk = item.namaProduk,
                    jumlahKirim = item.kirim, jumlahRetur = item.retur,
                    jumlahTerjual = terjual,
                    hargaSatuan = item.hargaSatuan, totalHarga = totalHarga,
                    tanggalNota = tanggalNota,
                    inputOleh = adminId, inputOlehNama = adminNama,
                    isSynced = network.isOnline
                )

                if (network.isOnline) {
                    val firebaseId = remote.addPenjualan(penjualan)
                    dao.insertPenjualan(entity.copy(id = firebaseId, isSynced = true))
                } else {
                    dao.insertPenjualan(entity)
                }
                savedCount++
            }

            // Log aktivitas
            if (savedCount > 0 && network.isOnline) {
                remote.addLogAktivitas(
                    LogAktivitas(
                        userId = adminId, userNama = adminNama,
                        userRole = Constants.ROLE_ADMIN,
                        aksi = Constants.ACTION_INPUT_NOTA,
                        deskripsi = "Input nota $mitraNama: $savedCount produk",
                        referensiId = mitraId, tipe = Constants.LOG_TYPE_PENJUALAN,
                        createdAt = now
                    )
                )
            }

            Result.Success("$savedCount produk tersimpan untuk $mitraNama")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Gagal menyimpan penjualan", e)
        }
    }

    // ── Hapus penjualan (Admin) ────────────────────────────────
    suspend fun deletePenjualanById(penjualanId: String): Result<String> {
        return try {
            if (network.isOnline) {
                remote.deletePenjualan(penjualanId)
            }
            dao.deletePenjualanById(penjualanId)
            Result.Success("Data berhasil dihapus")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Gagal menghapus data", e)
        }
    }

    // ── Update penjualan (Admin) ─────────────────────────────
    suspend fun updatePenjualanFields(
        penjualanId: String, kirim: Int, retur: Int, hargaSatuan: Int
    ): Result<String> {
        return try {
            val terjual = maxOf(0, kirim - retur)
            val totalHarga = terjual * hargaSatuan
            if (network.isOnline) {
                remote.updatePenjualan(penjualanId, kirim, retur, terjual, totalHarga)
            }
            dao.updatePenjualanFields(penjualanId, kirim, retur, terjual, totalHarga)
            Result.Success("Data berhasil diperbarui")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Gagal memperbarui data", e)
        }
    }

    // ── Sync offline data ────────────────────────────────────────
    suspend fun syncPendingPenjualan() {
        val unsynced = dao.getUnsyncedPenjualan()
        unsynced.forEach { entity ->
            try {
                val penjualan = Penjualan(
                    mitraId = entity.mitraId, mitraNama = entity.mitraNama,
                    ruteId = entity.ruteId, ruteNama = entity.ruteNama,
                    namaProduk = entity.namaProduk,
                    jumlahKirim = entity.jumlahKirim,
                    jumlahRetur = entity.jumlahRetur,
                    jumlahTerjual = entity.jumlahTerjual,
                    hargaSatuan = entity.hargaSatuan,
                    totalHarga = entity.totalHarga,
                    tanggalNota = Timestamp(java.util.Date(entity.tanggalNota)),
                    inputOleh = entity.inputOleh, inputOlehNama = entity.inputOlehNama,
                    isSynced = true, createdAt = Timestamp.now()
                )
                val docId = remote.addPenjualan(penjualan)
                dao.deletePenjualan(entity)
                dao.insertPenjualan(entity.copy(id = docId, isSynced = true))
            } catch (_: Exception) { }
        }
    }
}

/** Data class untuk item input per produk di Rapid Entry */
data class PenjualanItem(
    val namaProduk: String,
    val hargaSatuan: Int,
    val kirim: Int,
    val retur: Int
)
