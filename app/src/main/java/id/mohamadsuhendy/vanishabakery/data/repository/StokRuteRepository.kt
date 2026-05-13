package id.mohamadsuhendy.vanishabakery.data.repository

import com.google.firebase.Timestamp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.mohamadsuhendy.vanishabakery.data.local.dao.StokRuteDao
import id.mohamadsuhendy.vanishabakery.data.local.entity.StokRuteEntity
import id.mohamadsuhendy.vanishabakery.data.model.LogAktivitas
import id.mohamadsuhendy.vanishabakery.data.model.StokRute
import id.mohamadsuhendy.vanishabakery.data.remote.FirebaseDataSource
import id.mohamadsuhendy.vanishabakery.utils.Constants
import id.mohamadsuhendy.vanishabakery.utils.NetworkUtils
import id.mohamadsuhendy.vanishabakery.utils.Result
import kotlinx.coroutines.flow.Flow
import java.util.*

class StokRuteRepository(
    private val remote: FirebaseDataSource,
    private val dao: StokRuteDao,
    private val network: NetworkUtils
) {
    private val gson = Gson()

    fun getLocalStokRute(): Flow<List<StokRuteEntity>> = dao.getAllStokRute()

    fun getLocalStokRuteByPeriode(ruteId: String, startDate: Long, endDate: Long): Flow<List<StokRuteEntity>> =
        dao.getStokRuteByPeriode(ruteId, startDate, endDate)

    suspend fun getStokRuteByExactDate(ruteId: String, tanggal: Long): StokRuteEntity? {
        return dao.getStokRuteByExactDate(ruteId, tanggal)
    }

    suspend fun saveStokRute(
        ruteId: String,
        ruteNama: String,
        tanggal: Long,
        stokData: Map<String, Int>,
        adminId: String,
        adminNama: String
    ): Result<String> {
        return try {
            val existing = dao.getStokRuteByExactDate(ruteId, tanggal)
            val id = existing?.id ?: UUID.randomUUID().toString()
            val stokDataJson = gson.toJson(stokData)
            
            val entity = StokRuteEntity(
                id = id,
                ruteId = ruteId,
                ruteNama = ruteNama,
                tanggal = tanggal,
                stokDataJson = stokDataJson,
                inputOleh = adminId,
                inputOlehNama = adminNama,
                isSynced = network.isOnline
            )

            if (network.isOnline) {
                val model = StokRute(
                    ruteId = ruteId,
                    ruteNama = ruteNama,
                    tanggal = Timestamp(Date(tanggal)),
                    stokData = stokData,
                    inputOleh = adminId,
                    inputOlehNama = adminNama,
                    createdAt = Timestamp.now()
                )
                remote.setStokRute(id, model)
                dao.insertStokRute(entity.copy(isSynced = true))
                
                // Log Aktivitas
                val aksi = if (existing == null) Constants.ACTION_INPUT_STOK_RUTE else "Update Stok Bawaan Rute"
                remote.addLogAktivitas(LogAktivitas(
                    userId = adminId, userNama = adminNama,
                    userRole = Constants.ROLE_ADMIN,
                    aksi = aksi,
                    deskripsi = "$aksi $ruteNama",
                    referensiId = ruteId, tipe = Constants.LOG_TYPE_STOK_RUTE,
                    createdAt = Timestamp.now()
                ))
            } else {
                dao.insertStokRute(entity)
            }

            Result.Success("Stok bawaan rute $ruteNama berhasil disimpan")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Gagal menyimpan stok rute", e)
        }
    }

    suspend fun syncPendingStokRute() {
        val unsynced = dao.getUnsyncedStokRute()
        unsynced.forEach { entity ->
            try {
                val type = object : TypeToken<Map<String, Int>>() {}.type
                val stokData: Map<String, Int> = gson.fromJson(entity.stokDataJson, type)
                
                val model = StokRute(
                    ruteId = entity.ruteId,
                    ruteNama = entity.ruteNama,
                    tanggal = Timestamp(Date(entity.tanggal)),
                    stokData = stokData,
                    inputOleh = entity.inputOleh,
                    inputOlehNama = entity.inputOlehNama,
                    createdAt = Timestamp.now()
                )
                remote.setStokRute(entity.id, model)
                dao.insertStokRute(entity.copy(isSynced = true))
            } catch (_: Exception) { }
        }
    }
}
