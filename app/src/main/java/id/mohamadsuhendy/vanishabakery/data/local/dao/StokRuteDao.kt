package id.mohamadsuhendy.vanishabakery.data.local.dao

import androidx.room.*
import id.mohamadsuhendy.vanishabakery.data.local.entity.StokRuteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StokRuteDao {

    @Query("SELECT * FROM stok_rute ORDER BY tanggal DESC")
    fun getAllStokRute(): Flow<List<StokRuteEntity>>

    @Query("SELECT * FROM stok_rute WHERE ruteId = :ruteId AND tanggal >= :startDate AND tanggal < :endDate")
    fun getStokRuteByPeriode(ruteId: String, startDate: Long, endDate: Long): Flow<List<StokRuteEntity>>

    @Query("SELECT * FROM stok_rute WHERE ruteId = :ruteId AND tanggal = :tanggal LIMIT 1")
    suspend fun getStokRuteByExactDate(ruteId: String, tanggal: Long): StokRuteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStokRute(stokRute: StokRuteEntity)

    @Delete
    suspend fun deleteStokRute(stokRute: StokRuteEntity)

    @Query("SELECT * FROM stok_rute WHERE isSynced = 0")
    suspend fun getUnsyncedStokRute(): List<StokRuteEntity>

    @Query("UPDATE stok_rute SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
