package id.mohamadsuhendy.vanishabakery.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import id.mohamadsuhendy.vanishabakery.data.local.dao.MitraDao
import id.mohamadsuhendy.vanishabakery.data.local.dao.PengirimanDao
import id.mohamadsuhendy.vanishabakery.data.local.dao.PenjualanDao
import id.mohamadsuhendy.vanishabakery.data.local.dao.RuteDao
import id.mohamadsuhendy.vanishabakery.data.local.dao.StokRuteDao
import id.mohamadsuhendy.vanishabakery.data.local.entity.MitraEntity
import id.mohamadsuhendy.vanishabakery.data.local.entity.PengirimanEntity
import id.mohamadsuhendy.vanishabakery.data.local.entity.PenjualanEntity
import id.mohamadsuhendy.vanishabakery.data.local.entity.RuteEntity
import id.mohamadsuhendy.vanishabakery.data.local.entity.StokRuteEntity

@Database(
    entities = [
        MitraEntity::class,
        PengirimanEntity::class,
        PenjualanEntity::class,
        RuteEntity::class,
        StokRuteEntity::class
    ],
    version = 5,                     // bumped: added stok_rute for reconciliation
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mitraDao(): MitraDao
    abstract fun pengirimanDao(): PengirimanDao
    abstract fun penjualanDao(): PenjualanDao
    abstract fun ruteDao(): RuteDao
    abstract fun stokRuteDao(): StokRuteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vanisha_bakery_db"
                )
                    .fallbackToDestructiveMigration()   // ok for dev; use proper Migration in prod
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
