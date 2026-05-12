package id.mohamadsuhendy.vanishabakery.data.repository

import com.google.firebase.Timestamp
import id.mohamadsuhendy.vanishabakery.data.local.dao.PengirimanDao
import id.mohamadsuhendy.vanishabakery.data.local.entity.PengirimanEntity
import id.mohamadsuhendy.vanishabakery.data.model.LogAktivitas
import id.mohamadsuhendy.vanishabakery.data.model.Pengiriman
import id.mohamadsuhendy.vanishabakery.data.remote.FirebaseDataSource
import id.mohamadsuhendy.vanishabakery.utils.Constants
import id.mohamadsuhendy.vanishabakery.utils.NetworkUtils
import id.mohamadsuhendy.vanishabakery.utils.Result
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class PengirimanRepository(
    private val remote: FirebaseDataSource,
    private val dao: PengirimanDao,
    private val network: NetworkUtils
) {
    // ── Admin: lihat SEMUA pengiriman ────────────────────────────
    fun observeAllPengiriman(): Flow<List<Pengiriman>> = remote.observeAllPengiriman()

    // ── Sales: hanya lihat pengiriman MILIKNYA (data isolation) ──
    fun observePengirimanByStaff(staffId: String): Flow<List<Pengiriman>> =
        remote.observePengirimanByStaff(staffId)

    // ── Offline Room ─────────────────────────────────────────────
    fun getLocalPengiriman(): Flow<List<PengirimanEntity>> = dao.getAllPengiriman()

    fun getTodayCount(): Flow<Int> {
        val (start, end) = getTodayRange()
        return dao.getTodayCount(start, end)
    }

    fun getTodayCountByStaff(staffId: String): Flow<Int> {
        val (start, end) = getTodayRange()
        return dao.getTodayCountByStaff(start, end, staffId)
    }

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

    // ── Input pengiriman baru (sales action) ────────────────────
    suspend fun addPengiriman(
        mitraId: String,
        mitraNama: String,
        ruteId: String,
        ruteNama: String,
        namaProduk: String,
        jumlah: Int,
        staffId: String,
        staffNama: String
    ): Result<String> {
        return try {
            val now = Timestamp.now()

            if (network.isOnline) {
                val pengiriman = Pengiriman(
                    mitraId = mitraId, mitraNama = mitraNama,
                    ruteId = ruteId, ruteNama = ruteNama,
                    namaProduk = namaProduk, jumlah = jumlah,
                    tanggal = now, staffId = staffId, staffNama = staffNama,
                    isSynced = true, createdAt = now
                )
                val docId = remote.addPengiriman(pengiriman)

                remote.addLogAktivitas(
                    LogAktivitas(
                        userId = staffId, userNama = staffNama,
                        userRole = Constants.ROLE_STAFF,
                        aksi = Constants.ACTION_INPUT_PENGIRIMAN,
                        deskripsi = "Pengiriman $jumlah $namaProduk ke $mitraNama (Rute: $ruteNama)",
                        referensiId = docId, tipe = Constants.LOG_TYPE_PENGIRIMAN,
                        createdAt = now
                    )
                )

                // Notifikasi ke Admin (Broadcast)
                remote.addNotifikasi(
                    id.mohamadsuhendy.vanishabakery.data.model.Notifikasi(
                        judul = "Laporan Pengiriman 📦",
                        pesan = "$staffNama baru saja mengirim $jumlah $namaProduk ke $mitraNama",
                        targetUserId = "", // Broadcast to admin
                        referensiId = docId,
                        tipe = Constants.LOG_TYPE_PENGIRIMAN,
                        createdAt = now
                    )
                )

                dao.insertPengiriman(
                    PengirimanEntity(
                        id = docId, mitraId = mitraId, mitraNama = mitraNama,
                        namaProduk = namaProduk, jumlah = jumlah,
                        tanggal = now.toDate().time,
                        staffId = staffId, staffNama = staffNama,
                        isSynced = true
                    )
                )
                Result.Success(docId)
            } else {
                val tempId = UUID.randomUUID().toString()
                dao.insertPengiriman(
                    PengirimanEntity(
                        id = tempId, mitraId = mitraId, mitraNama = mitraNama,
                        namaProduk = namaProduk, jumlah = jumlah,
                        tanggal = System.currentTimeMillis(),
                        staffId = staffId, staffNama = staffNama,
                        isSynced = false
                    )
                )
                Result.Success(tempId)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Gagal menyimpan pengiriman", e)
        }
    }

    // ── Sync offline data ────────────────────────────────────────
    suspend fun syncPendingPengiriman() {
        val unsynced = dao.getUnsyncedPengiriman()
        unsynced.forEach { entity ->
            try {
                val pengiriman = Pengiriman(
                    mitraId = entity.mitraId, mitraNama = entity.mitraNama,
                    namaProduk = entity.namaProduk, jumlah = entity.jumlah,
                    tanggal = Timestamp(java.util.Date(entity.tanggal)),
                    staffId = entity.staffId, staffNama = entity.staffNama,
                    isSynced = true, createdAt = Timestamp.now()
                )
                val docId = remote.addPengiriman(pengiriman)
                dao.deletePengiriman(entity)
                dao.insertPengiriman(entity.copy(id = docId, isSynced = true))
            } catch (_: Exception) { }
        }
    }
}
