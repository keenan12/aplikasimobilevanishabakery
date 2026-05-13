package id.mohamadsuhendy.vanishabakery.data.local.dao

import androidx.room.*
import id.mohamadsuhendy.vanishabakery.data.local.entity.PenjualanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PenjualanDao {

    @Query("SELECT * FROM penjualan ORDER BY tanggalNota DESC")
    fun getAllPenjualan(): Flow<List<PenjualanEntity>>

    @Query("SELECT * FROM penjualan WHERE mitraId = :mitraId ORDER BY tanggalNota DESC")
    fun getPenjualanByMitra(mitraId: String): Flow<List<PenjualanEntity>>

    @Query("SELECT * FROM penjualan WHERE mitraId = :mitraId AND tanggalNota >= :startDate AND tanggalNota < :endDate")
    fun getPenjualanByMitraPeriode(mitraId: String, startDate: Long, endDate: Long): Flow<List<PenjualanEntity>>

    @Query("SELECT * FROM penjualan WHERE tanggalNota >= :startDate AND tanggalNota < :endDate ORDER BY tanggalNota DESC")
    fun getPenjualanByPeriode(startDate: Long, endDate: Long): Flow<List<PenjualanEntity>>

    @Query("SELECT * FROM penjualan WHERE ruteId = :ruteId AND tanggalNota >= :startDate AND tanggalNota < :endDate ORDER BY tanggalNota DESC")
    fun getPenjualanByRutePeriode(ruteId: String, startDate: Long, endDate: Long): Flow<List<PenjualanEntity>>

    @Query("SELECT * FROM penjualan WHERE isSynced = 0")
    suspend fun getUnsyncedPenjualan(): List<PenjualanEntity>

    @Query("SELECT SUM(totalHarga) FROM penjualan WHERE tanggalNota >= :startDate AND tanggalNota < :endDate")
    fun getOmsetByPeriode(startDate: Long, endDate: Long): Flow<Int?>

    @Query("SELECT SUM(jumlahTerjual) FROM penjualan WHERE tanggalNota >= :startOfDay AND tanggalNota <= :endOfDay")
    fun getTodaySales(startOfDay: Long, endOfDay: Long): Flow<Int?>

    @Query("SELECT SUM(jumlahTerjual) FROM penjualan WHERE inputOleh = :staffId AND tanggalNota >= :startOfDay AND tanggalNota <= :endOfDay")
    fun getTodaySalesByStaff(startOfDay: Long, endOfDay: Long, staffId: String): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPenjualan(penjualan: PenjualanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPenjualan(list: List<PenjualanEntity>)

    @Query("UPDATE penjualan SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Delete
    suspend fun deletePenjualan(penjualan: PenjualanEntity)

    @Query("DELETE FROM penjualan WHERE id = :id")
    suspend fun deletePenjualanById(id: String)

    @Query("UPDATE penjualan SET jumlahKirim = :kirim, jumlahRetur = :retur, jumlahTerjual = :terjual, totalHarga = :totalHarga WHERE id = :id")
    suspend fun updatePenjualanFields(id: String, kirim: Int, retur: Int, terjual: Int, totalHarga: Int)
}
