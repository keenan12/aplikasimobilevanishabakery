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
    // ── Admin: lihat SEMUA penjualan ─────────────────────────────
    fun observeAllPenjualan(): Flow<List<Penjualan>> = remote.observeAllPenjualan()

    // ── Sales: hanya lihat penjualan MILIKNYA (data isolation) ───
    fun observePenjualanByStaff(staffId: String): Flow<List<Penjualan>> =
        remote.observePenjualanByStaff(staffId)

    // ── Offline Room ─────────────────────────────────────────────
    fun getLocalPenjualan(): Flow<List<PenjualanEntity>> = dao.getAllPenjualan()

    fun getTodaySales(): Flow<Int?> {
        val (start, end) = getTodayRange()
        return dao.getTodaySales(start, end)
    }

    fun getTodaySalesByStaff(staffId: String): Flow<Int?> {
        val (start, end) = getTodayRange()
        return dao.getTodaySalesByStaff(start, end, staffId)
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

    // ── Input penjualan/retur (sales action) ────────────────────
    // jumlahSisa dihitung otomatis: jumlahDikirim - jumlahTerjual
    // Validasi: jumlahTerjual <= jumlahDikirim (dilakukan di ViewModel)
    suspend fun addPenjualan(
        pengirimanId: String,
        mitraId: String,
        mitraNama: String,
        namaProduk: String,
        jumlahDikirim: Int,
        jumlahTerjual: Int,
        hargaSatuan: Int,
        totalHarga: Int,
        staffId: String,
        staffNama: String
    ): Result<String> {
        return try {
            val jumlahSisa = jumlahDikirim - jumlahTerjual  // auto-hitung retur
            val now = Timestamp.now()

            if (network.isOnline) {
                val penjualan = Penjualan(
                    pengirimanId = pengirimanId,
                    mitraId = mitraId, mitraNama = mitraNama,
                    namaProduk = namaProduk,
                    jumlahDikirim = jumlahDikirim,
                    jumlahTerjual = jumlahTerjual,
                    jumlahSisa = jumlahSisa,
                    hargaSatuan = hargaSatuan,
                    totalHarga = totalHarga,
                    tanggal = now, staffId = staffId, staffNama = staffNama,
                    isSynced = true, createdAt = now
                )
                val docId = remote.addPenjualan(penjualan)

                remote.addLogAktivitas(
                    LogAktivitas(
                        userId = staffId, userNama = staffNama,
                        userRole = Constants.ROLE_STAFF,
                        aksi = Constants.ACTION_INPUT_PENJUALAN,
                        deskripsi = "Penjualan $jumlahTerjual $namaProduk di $mitraNama, retur $jumlahSisa",
                        referensiId = docId, tipe = Constants.LOG_TYPE_PENJUALAN,
                        createdAt = now
                    )
                )

                dao.insertPenjualan(
                    PenjualanEntity(
                        id = docId, pengirimanId = pengirimanId,
                        mitraId = mitraId, mitraNama = mitraNama,
                        namaProduk = namaProduk, jumlahDikirim = jumlahDikirim,
                        jumlahTerjual = jumlahTerjual, jumlahSisa = jumlahSisa,
                        hargaSatuan = hargaSatuan, totalHarga = totalHarga,
                        tanggal = now.toDate().time,
                        staffId = staffId, staffNama = staffNama,
                        isSynced = true
                    )
                )
                Result.Success(docId)
            } else {
                val tempId = UUID.randomUUID().toString()
                dao.insertPenjualan(
                    PenjualanEntity(
                        id = tempId, pengirimanId = pengirimanId,
                        mitraId = mitraId, mitraNama = mitraNama,
                        namaProduk = namaProduk, jumlahDikirim = jumlahDikirim,
                        jumlahTerjual = jumlahTerjual, jumlahSisa = jumlahSisa,
                        hargaSatuan = hargaSatuan, totalHarga = totalHarga,
                        tanggal = System.currentTimeMillis(),
                        staffId = staffId, staffNama = staffNama,
                        isSynced = false
                    )
                )
                Result.Success(tempId)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Gagal menyimpan penjualan", e)
        }
    }

    // ── Sync offline data ────────────────────────────────────────
    suspend fun syncPendingPenjualan() {
        val unsynced = dao.getUnsyncedPenjualan()
        unsynced.forEach { entity ->
            try {
                val penjualan = Penjualan(
                    pengirimanId = entity.pengirimanId,
                    mitraId = entity.mitraId, mitraNama = entity.mitraNama,
                    namaProduk = entity.namaProduk,
                    jumlahDikirim = entity.jumlahDikirim,
                    jumlahTerjual = entity.jumlahTerjual,
                    jumlahSisa = entity.jumlahSisa,
                    hargaSatuan = entity.hargaSatuan,
                    totalHarga = entity.totalHarga,
                    tanggal = Timestamp(java.util.Date(entity.tanggal)),
                    staffId = entity.staffId, staffNama = entity.staffNama,
                    isSynced = true, createdAt = Timestamp.now()
                )
                val docId = remote.addPenjualan(penjualan)
                dao.deletePenjualan(entity)
                dao.insertPenjualan(entity.copy(id = docId, isSynced = true))
            } catch (_: Exception) { }
        }
    }
}
