package id.mohamadsuhendy.vanishabakery.data.local.dao

import androidx.room.*
import id.mohamadsuhendy.vanishabakery.data.local.entity.PenjualanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PenjualanDao {

    @Query("SELECT * FROM penjualan ORDER BY tanggal DESC")
    fun getAllPenjualan(): Flow<List<PenjualanEntity>>

    @Query("SELECT * FROM penjualan WHERE mitraId = :mitraId ORDER BY tanggal DESC")
    fun getPenjualanByMitra(mitraId: String): Flow<List<PenjualanEntity>>

    @Query("SELECT * FROM penjualan WHERE isSynced = 0")
    suspend fun getUnsyncedPenjualan(): List<PenjualanEntity>

    @Query("SELECT SUM(jumlahTerjual) FROM penjualan WHERE tanggal >= :startOfDay AND tanggal <= :endOfDay")
    fun getTodaySales(startOfDay: Long, endOfDay: Long): Flow<Int?>

    @Query("SELECT SUM(jumlahTerjual) FROM penjualan WHERE staffId = :staffId AND tanggal >= :startOfDay AND tanggal <= :endOfDay")
    fun getTodaySalesByStaff(startOfDay: Long, endOfDay: Long, staffId: String): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPenjualan(penjualan: PenjualanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPenjualan(list: List<PenjualanEntity>)

    @Query("UPDATE penjualan SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Delete
    suspend fun deletePenjualan(penjualan: PenjualanEntity)
}
