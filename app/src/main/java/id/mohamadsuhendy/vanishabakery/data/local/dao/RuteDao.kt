package id.mohamadsuhendy.vanishabakery.data.local.dao

import androidx.room.*
import id.mohamadsuhendy.vanishabakery.data.local.entity.RuteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuteDao {

    // Ambil semua rute (untuk admin)
    @Query("SELECT * FROM rute WHERE isActive = 1 ORDER BY namaRute ASC")
    fun getAllRute(): Flow<List<RuteEntity>>

    // Ambil rute milik staff tertentu (untuk sales — isolasi data)
    @Query("SELECT * FROM rute WHERE staffId = :staffId AND isActive = 1 ORDER BY namaRute ASC")
    fun getRuteByStaff(staffId: String): Flow<List<RuteEntity>>

    @Query("SELECT * FROM rute WHERE id = :id LIMIT 1")
    suspend fun getRuteById(id: String): RuteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRute(rute: RuteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRute(ruteList: List<RuteEntity>)

    @Update
    suspend fun updateRute(rute: RuteEntity)

    @Query("UPDATE rute SET namaRute = :namaRute, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNamaRute(id: String, namaRute: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE rute SET totalMitra = :total WHERE id = :id")
    suspend fun updateTotalMitra(id: String, total: Int)

    @Query("UPDATE rute SET isActive = :isActive WHERE id = :id")
    suspend fun setRuteActive(id: String, isActive: Boolean)

    @Query("DELETE FROM rute WHERE id = :id")
    suspend fun deleteRuteById(id: String)

    @Query("SELECT COUNT(*) FROM rute WHERE staffId = :staffId AND isActive = 1")
    fun getTotalRuteByStaff(staffId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM rute WHERE isActive = 1")
    fun getTotalRute(): Flow<Int>
}
