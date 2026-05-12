package id.mohamadsuhendy.vanishabakery

import android.app.Application
import id.mohamadsuhendy.vanishabakery.data.local.AppDatabase
import id.mohamadsuhendy.vanishabakery.data.remote.FirebaseDataSource
import id.mohamadsuhendy.vanishabakery.data.repository.AuthRepository
import id.mohamadsuhendy.vanishabakery.data.repository.MitraRepository
import id.mohamadsuhendy.vanishabakery.data.repository.PengirimanRepository
import id.mohamadsuhendy.vanishabakery.data.repository.PenjualanRepository
import id.mohamadsuhendy.vanishabakery.data.repository.ProdukRepository
import id.mohamadsuhendy.vanishabakery.data.repository.RuteRepository
import id.mohamadsuhendy.vanishabakery.utils.NetworkUtils

class VanishaBakeryApp : Application() {

    // Lazy-initialized singletons (manual DI / Service Locator)
    val database by lazy { AppDatabase.getInstance(this) }
    val networkUtils by lazy { NetworkUtils(this) }
    val firebaseDataSource by lazy { FirebaseDataSource() }

    val authRepository by lazy { AuthRepository(firebaseDataSource) }

    val ruteRepository by lazy {
        RuteRepository(firebaseDataSource, database.ruteDao())
    }
    val mitraRepository by lazy {
        MitraRepository(firebaseDataSource, database.mitraDao(), networkUtils)
    }
    val pengirimanRepository by lazy {
        PengirimanRepository(firebaseDataSource, database.pengirimanDao(), networkUtils)
    }
    val penjualanRepository by lazy {
        PenjualanRepository(firebaseDataSource, database.penjualanDao(), networkUtils)
    }
    val produkRepository by lazy {
        ProdukRepository(firebaseDataSource)
    }

    companion object {
        lateinit var instance: VanishaBakeryApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
