package id.mohamadsuhendy.vanishabakery.data.local.dao

import androidx.room.*
import id.mohamadsuhendy.vanishabakery.data.local.entity.MitraEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MitraDao {

    // Admin: lihat semua mitra
    @Query("SELECT * FROM mitra ORDER BY createdAt DESC")
    fun getAllMitra(): Flow<List<MitraEntity>>

    // Sales: hanya lihat mitra miliknya (isolasi data)
    @Query("SELECT * FROM mitra WHERE staffId = :staffId ORDER BY createdAt DESC")
    fun getMitraByStaff(staffId: String): Flow<List<MitraEntity>>

    // Sales: filter by rute (dropdown filter di UI)
    @Query("SELECT * FROM mitra WHERE staffId = :staffId AND ruteId = :ruteId ORDER BY createdAt DESC")
    fun getMitraByStaffAndRute(staffId: String, ruteId: String): Flow<List<MitraEntity>>

    @Query("SELECT * FROM mitra WHERE status = :status ORDER BY createdAt DESC")
    fun getMitraByStatus(status: String): Flow<List<MitraEntity>>

    // Sales: filter by status (pending approval)
    @Query("SELECT * FROM mitra WHERE staffId = :staffId AND status = :status ORDER BY createdAt DESC")
    fun getMitraByStaffAndStatus(staffId: String, status: String): Flow<List<MitraEntity>>

    @Query("SELECT * FROM mitra WHERE id = :id LIMIT 1")
    suspend fun getMitraById(id: String): MitraEntity?

    @Query("SELECT * FROM mitra WHERE isSynced = 0")
    suspend fun getUnsyncedMitra(): List<MitraEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMitra(mitra: MitraEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMitra(mitraList: List<MitraEntity>)

    @Update
    suspend fun updateMitra(mitra: MitraEntity)

    @Query("UPDATE mitra SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("UPDATE mitra SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Delete
    suspend fun deleteMitra(mitra: MitraEntity)

    @Query("DELETE FROM mitra WHERE id = :id")
    suspend fun deleteMitraById(id: String)

    @Query("SELECT COUNT(*) FROM mitra")
    fun getTotalMitra(): Flow<Int>

    @Query("SELECT COUNT(*) FROM mitra WHERE staffId = :staffId")
    fun getTotalMitraByStaff(staffId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM mitra WHERE status = 'pending'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM mitra WHERE staffId = :staffId AND status = 'pending'")
    fun getPendingCountByStaff(staffId: String): Flow<Int>
}
