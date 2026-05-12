package id.mohamadsuhendy.vanishabakery.data.repository

import android.net.Uri
import com.google.firebase.Timestamp
import id.mohamadsuhendy.vanishabakery.data.local.dao.MitraDao
import id.mohamadsuhendy.vanishabakery.data.local.entity.MitraEntity
import id.mohamadsuhendy.vanishabakery.data.model.LogAktivitas
import id.mohamadsuhendy.vanishabakery.data.model.Mitra
import id.mohamadsuhendy.vanishabakery.data.model.Notifikasi
import id.mohamadsuhendy.vanishabakery.data.remote.FirebaseDataSource
import id.mohamadsuhendy.vanishabakery.utils.Constants
import id.mohamadsuhendy.vanishabakery.utils.NetworkUtils
import id.mohamadsuhendy.vanishabakery.utils.Result
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MitraRepository(
    private val remote: FirebaseDataSource,
    private val dao: MitraDao,
    private val network: NetworkUtils
) {
    suspend fun getMitraById(id: String): Mitra? = remote.getMitraById(id)

    // ── Admin: lihat SEMUA mitra ─────────────────────────────────
    fun observeAllMitra(): Flow<List<Mitra>> = remote.observeAllMitra()
    fun observeAllMitraByStatus(status: String): Flow<List<Mitra>> =
        remote.observeMitraByStatus(status)

    // ── Sales: hanya lihat mitra MILIKNYA (data isolation) ───────
    fun observeMitraByStaff(staffId: String): Flow<List<Mitra>> =
        remote.observeMitraByStaff(staffId)

    fun observeMitraByStaffAndRute(staffId: String, ruteId: String): Flow<List<Mitra>> =
        remote.observeMitraByStaffAndRute(staffId, ruteId)

    // ── Offline Room streams ─────────────────────────────────────
    fun getLocalAllMitra(): Flow<List<MitraEntity>> = dao.getAllMitra()
    fun getLocalMitraByStaff(staffId: String): Flow<List<MitraEntity>> =
        dao.getMitraByStaff(staffId)

    // Stats untuk dashboard (scoped by role)
    fun getTotalMitra(): Flow<Int> = dao.getTotalMitra()
    fun getTotalMitraByStaff(staffId: String): Flow<Int> = dao.getTotalMitraByStaff(staffId)
    fun getPendingCount(): Flow<Int> = dao.getPendingCount()
    fun getPendingCountByStaff(staffId: String): Flow<Int> = dao.getPendingCountByStaff(staffId)

    // ── Tambah mitra baru (sales action) ─────────────────────────
    // GPS & foto wajib dari kamera — validasi di UI, bukan di sini
    suspend fun addMitra(
        namaToko: String,
        fotoUri: Uri,
        latitude: Double,
        longitude: Double,
        alamat: String,
        staffId: String,
        staffNama: String,
        staffRole: String,
        ruteId: String,
        ruteNama: String
    ): Result<String> {
        return try {
            val tempId = UUID.randomUUID().toString()

            if (network.isOnline) {
                // 1. Cek apakah ini data Base64 (untuk bypass Storage yang terkunci)
                val photoUrl = if (fotoUri.scheme == "data" || fotoUri.toString().startsWith("data:image")) {
                    fotoUri.toString()
                } else {
                    try {
                        remote.uploadMitraPhoto(fotoUri, tempId)
                    } catch (e: Exception) {
                        return Result.Error("Gagal upload foto: ${e.message}. Periksa 'Storage' di Firebase Console.")
                    }
                }

                // 2. Simpan ke Firestore dengan status PENDING
                val mitra = Mitra(
                    namaToko = namaToko,
                    fotoUrl = photoUrl,
                    latitude = latitude,
                    longitude = longitude,
                    alamat = alamat,
                    status = Constants.STATUS_PENDING,  // ← selalu pending, admin yg approve
                    staffId = staffId,
                    staffNama = staffNama,
                    ruteId = ruteId,
                    ruteNama = ruteNama,
                    isSynced = true,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
                val docId = remote.addMitra(mitra)

                // 3. Log aktivitas (Ensure this is captured for Admin history)
                try {
                    remote.addLogAktivitas(
                        LogAktivitas(
                            userId = staffId, userNama = staffNama,
                            userRole = staffRole,
                            aksi = Constants.ACTION_TAMBAH_MITRA,
                            deskripsi = "Sales $staffNama mengajukan mitra baru: $namaToko",
                            referensiId = docId, tipe = Constants.LOG_TYPE_MITRA,
                            createdAt = Timestamp.now()
                        )
                    )

                    // 4. Send Notification to Admin
                    remote.addNotifikasi(
                        Notifikasi(
                            judul = "Pengajuan Mitra Baru",
                            pesan = "Sales $staffNama mengajukan mitra baru: $namaToko",
                            targetUserId = "", // Broadcast to all admins
                            referensiId = docId,
                            tipe = Constants.LOG_TYPE_MITRA,
                            isRead = false,
                            createdAt = Timestamp.now()
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.e("MitraRepository", "Gagal mencatat log: ${e.message}")
                }

                // 4. Cache ke Room
                dao.insertMitra(
                    MitraEntity(
                        id = docId, namaToko = namaToko, fotoUrl = photoUrl,
                        latitude = latitude, longitude = longitude, alamat = alamat,
                        status = Constants.STATUS_PENDING,
                        staffId = staffId, staffNama = staffNama,
                        ruteId = ruteId, ruteNama = ruteNama,
                        isSynced = true
                    )
                )
                Result.Success(docId)
            } else {
                // Offline: simpan lokal, sync saat online
                dao.insertMitra(
                    MitraEntity(
                        id = tempId, namaToko = namaToko,
                        fotoUrl = fotoUri.toString(),
                        latitude = latitude, longitude = longitude, alamat = alamat,
                        status = Constants.STATUS_PENDING,
                        staffId = staffId, staffNama = staffNama,
                        ruteId = ruteId, ruteNama = ruteNama,
                        isSynced = false
                    )
                )
                Result.Success(tempId)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Gagal menyimpan mitra", e)
        }
    }

    // ── Admin: approve / tolak mitra ─────────────────────────────
    suspend fun updateMitraStatus(
        mitraId: String,
        status: String,
        adminId: String,
        adminNama: String
    ): Result<Unit> {
        return try {
            // 1. AMBIL DATA MITRA DULU sebelum diupdate (untuk dapat staffId)
            val mitra = remote.getMitraById(mitraId)
                ?: return Result.Error("Data mitra tidak ditemukan")

            // 2. Update Remote & Local
            remote.updateMitraStatus(mitraId, status)
            dao.updateStatus(mitraId, status)

            val aksi = if (status == Constants.STATUS_APPROVED)
                Constants.ACTION_APPROVE_MITRA
            else
                Constants.ACTION_REJECT_MITRA

            val statusIndo = if (status == Constants.STATUS_APPROVED) "Disetujui" else "Ditolak"
            
            // 3. Log Aktivitas
            remote.addLogAktivitas(
                LogAktivitas(
                    userId = adminId, userNama = adminNama,
                    userRole = Constants.ROLE_ADMIN,
                    aksi = aksi,
                    deskripsi = "Pendaftaran mitra ${mitra.namaToko} telah $statusIndo oleh Admin $adminNama",
                    referensiId = mitraId, tipe = Constants.LOG_TYPE_MITRA,
                    createdAt = Timestamp.now()
                )
            )

            // 4. Kirim Notifikasi ke Sales (Pasti dapet targetUserId karena kita ambil di awal)
            try {
                remote.addNotifikasi(
                    Notifikasi(
                        judul = if (status == Constants.STATUS_APPROVED) "Mitra Disetujui! ✅" else "Mitra Ditolak ❌",
                        pesan = "Pengajuan mitra ${mitra.namaToko} telah $statusIndo oleh Admin",
                        targetUserId = mitra.staffId,
                        referensiId = mitraId,
                        tipe = Constants.LOG_TYPE_MITRA,
                        isRead = false,
                        createdAt = Timestamp.now()
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("MitraRepository", "Gagal kirim notifikasi: ${e.message}")
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.Error(e.message ?: "Gagal memperbarui status")
        }
    }

    // ── Admin: arsipkan mitra (soft delete) ─────────────────────
    suspend fun archiveMitra(
        mitraId: String,
        adminId: String,
        adminNama: String
    ): Result<Unit> {
        return try {
            remote.updateMitraStatus(mitraId, Constants.STATUS_DELETED)
            dao.updateStatus(mitraId, Constants.STATUS_DELETED)

            val mitra = remote.getMitraById(mitraId)
            val deskripsi = if (mitra != null) {
                "Mitra ${mitra.namaToko} (Sales: ${mitra.staffNama}, Rute: ${mitra.ruteNama}) diarsipkan oleh Admin"
            } else {
                "Mitra $mitraId diarsipkan (nonaktif) oleh Admin"
            }

            remote.addLogAktivitas(
                LogAktivitas(
                    userId = adminId, userNama = adminNama,
                    userRole = Constants.ROLE_ADMIN,
                    aksi = "ARSIPKAN_MITRA",
                    deskripsi = deskripsi,
                    referensiId = mitraId, tipe = Constants.LOG_TYPE_MITRA,
                    createdAt = Timestamp.now()
                )
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Gagal mengarsipkan mitra", e)
        }
    }

    // ── Admin: hapus mitra permanen ──────────────────────────────
    suspend fun deleteMitra(mitraId: String, adminId: String, adminNama: String): Result<Unit> {
        return try {
            remote.deleteMitra(mitraId)
            dao.deleteMitraById(mitraId)

            remote.addLogAktivitas(
                LogAktivitas(
                    userId = adminId, userNama = adminNama,
                    userRole = Constants.ROLE_ADMIN,
                    aksi = "HAPUS_MITRA",
                    deskripsi = "Mitra $mitraId dihapus dari sistem",
                    referensiId = mitraId, tipe = Constants.LOG_TYPE_MITRA,
                    createdAt = Timestamp.now()
                )
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Gagal menghapus mitra", e)
        }
    }

    // ── Sync offline data saat kembali online ────────────────────
    suspend fun syncPendingMitra() {
        val unsynced = dao.getUnsyncedMitra()
        unsynced.forEach { entity ->
            try {
                val photoUrl = try {
                    remote.uploadMitraPhoto(
                        android.net.Uri.parse(entity.fotoUrl), entity.id
                    )
                } catch (_: Exception) { entity.fotoUrl }

                val mitra = Mitra(
                    namaToko = entity.namaToko, fotoUrl = photoUrl,
                    latitude = entity.latitude, longitude = entity.longitude,
                    alamat = entity.alamat, status = entity.status,
                    staffId = entity.staffId, staffNama = entity.staffNama,
                    ruteId = entity.ruteId, ruteNama = entity.ruteNama,
                    isSynced = true, createdAt = Timestamp.now()
                )
                val docId = remote.addMitra(mitra)
                dao.deleteMitraById(entity.id)
                dao.insertMitra(entity.copy(id = docId, fotoUrl = photoUrl, isSynced = true))
            } catch (_: Exception) { /* retry next time */ }
        }
    }
}
