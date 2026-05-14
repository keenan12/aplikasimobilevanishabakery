package id.mohamadsuhendy.vanishabakery.data.repository

import com.google.firebase.Timestamp
import id.mohamadsuhendy.vanishabakery.data.local.dao.RuteDao
import id.mohamadsuhendy.vanishabakery.data.local.entity.RuteEntity
import id.mohamadsuhendy.vanishabakery.data.model.Rute
import id.mohamadsuhendy.vanishabakery.data.remote.FirebaseDataSource
import id.mohamadsuhendy.vanishabakery.utils.Constants
import id.mohamadsuhendy.vanishabakery.utils.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach

class RuteRepository(
    private val remote: FirebaseDataSource,
    private val dao: RuteDao
) {

    // ── Admin: lihat semua rute ──────────────────────────────────
    fun observeAllRute(): Flow<List<Rute>> = remote.observeAllRute()
        .onEach { list -> cacheRute(list, "") }           // Admin context, staffId empty or handle differently

    // ── Sales: hanya rute miliknya (data isolation) ──────────────
    fun observeRuteByStaff(staffId: String): Flow<List<Rute>> =
        remote.observeRuteByStaff(staffId)
            .onEach { list -> cacheRute(list, staffId) }

    // ── Offline fallback dari Room ────────────────────────────────
    fun getLocalAllRute(): Flow<List<RuteEntity>> = dao.getAllRute()
    fun getLocalRuteByStaff(staffId: String): Flow<List<RuteEntity>> =
        dao.getRuteByStaff(staffId)

    // ── Admin: tambah rute baru ───────────────────────────────────
    suspend fun addRute(
        namaRute: String,
        kode: String,
        staffId: String,
        staffNama: String,
        adminId: String,
        adminNama: String
    ): Result<String> {
        return try {
            val rute = Rute(
                namaRute = namaRute,
                kode = kode,
                staffId = staffId,
                staffNama = staffNama,
                totalMitra = 0,
                isActive = true,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            val docId = remote.addRute(rute)
            
            // Log aktivitas
            remote.addLogAktivitas(
                id.mohamadsuhendy.vanishabakery.data.model.LogAktivitas(
                    userId = adminId, userNama = adminNama,
                    userRole = Constants.ROLE_ADMIN,
                    aksi = "TAMBAH_RUTE",
                    deskripsi = "Membuat rute baru: $namaRute ($kode) untuk $staffNama",
                    referensiId = docId, tipe = "rute",
                    createdAt = Timestamp.now()
                )
            )

            // Notifikasi ke Sales
            remote.addNotifikasi(
                id.mohamadsuhendy.vanishabakery.data.model.Notifikasi(
                    judul = "Rute Baru Ditugaskan! 🚚",
                    pesan = "Anda telah ditugaskan rute baru: $namaRute ($kode)",
                    targetUserId = staffId,
                    referensiId = docId,
                    tipe = "rute",
                    createdAt = Timestamp.now()
                )
            )

            dao.insertRute(
                RuteEntity(
                    id = docId,
                    namaRute = namaRute,
                    kode = kode,
                    staffId = staffId,
                    staffNama = staffNama
                )
            )
            Result.Success(docId)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Gagal menambah rute", e)
        }
    }

    // ── Admin: edit nama / kode rute ─────────────────────────────
    suspend fun updateRute(
        ruteId: String,
        namaRute: String,
        kode: String
    ): Result<Unit> {
        return try {
            remote.updateRute(ruteId, namaRute, kode)
            dao.updateNamaRute(ruteId, namaRute)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Gagal memperbarui rute", e)
        }
    }

    // ── Admin: hapus rute (soft delete) ─────────────────────────
    suspend fun deleteRute(ruteId: String): Result<Unit> {
        return try {
            remote.deleteRute(ruteId)
            dao.deleteRuteById(ruteId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Gagal menghapus rute", e)
        }
    }

    // ── Internal: sync cache Room ─────────────────────────────────
    private suspend fun cacheRute(list: List<Rute>, staffId: String) {
        // 1. Get current local IDs for this staff
        val localRutes = dao.getRuteByStaff(staffId).first()
        val localIds = localRutes.map { it.id }.toSet()
        val remoteIds = list.map { it.id }.toSet()

        // 2. Delete local items not in remote
        val toDelete = localIds - remoteIds
        toDelete.forEach { dao.deleteRuteById(it) }

        // 3. Update/Insert remote items
        list.forEach { rute ->
            dao.insertRute(
                RuteEntity(
                    id = rute.id,
                    namaRute = rute.namaRute,
                    kode = rute.kode,
                    staffId = rute.staffId,
                    staffNama = rute.staffNama,
                    totalMitra = rute.totalMitra,
                    isActive = rute.isActive
                )
            )
        }
    }
}
