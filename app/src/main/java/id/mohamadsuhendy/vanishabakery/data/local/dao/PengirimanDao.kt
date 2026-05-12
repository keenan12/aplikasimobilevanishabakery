package id.mohamadsuhendy.vanishabakery.data.local.dao

import androidx.room.*
import id.mohamadsuhendy.vanishabakery.data.local.entity.PengirimanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PengirimanDao {

    @Query("SELECT * FROM pengiriman ORDER BY tanggal DESC")
    fun getAllPengiriman(): Flow<List<PengirimanEntity>>

    @Query("SELECT * FROM pengiriman WHERE mitraId = :mitraId ORDER BY tanggal DESC")
    fun getPengirimanByMitra(mitraId: String): Flow<List<PengirimanEntity>>

    @Query("SELECT * FROM pengiriman WHERE isSynced = 0")
    suspend fun getUnsyncedPengiriman(): List<PengirimanEntity>

    @Query("SELECT COUNT(*) FROM pengiriman WHERE tanggal >= :startOfDay AND tanggal <= :endOfDay")
    fun getTodayCount(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM pengiriman WHERE staffId = :staffId AND tanggal >= :startOfDay AND tanggal <= :endOfDay")
    fun getTodayCountByStaff(startOfDay: Long, endOfDay: Long, staffId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPengiriman(pengiriman: PengirimanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPengiriman(list: List<PengirimanEntity>)

    @Query("UPDATE pengiriman SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Delete
    suspend fun deletePengiriman(pengiriman: PengirimanEntity)
}
