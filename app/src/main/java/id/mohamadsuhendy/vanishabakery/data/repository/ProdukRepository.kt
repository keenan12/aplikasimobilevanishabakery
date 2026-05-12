package id.mohamadsuhendy.vanishabakery.data.repository

import id.mohamadsuhendy.vanishabakery.data.model.Produk
import id.mohamadsuhendy.vanishabakery.data.remote.FirebaseDataSource
import kotlinx.coroutines.flow.Flow

class ProdukRepository(
    private val firebaseDataSource: FirebaseDataSource
) {
    fun observeAllProduk(): Flow<List<Produk>> = firebaseDataSource.observeAllProduk()

    suspend fun addProduk(produk: Produk) = firebaseDataSource.addProduk(produk)

    suspend fun updateProduk(produk: Produk) = firebaseDataSource.updateProduk(produk)

    suspend fun deleteProduk(produkId: String) = firebaseDataSource.deleteProduk(produkId)

    suspend fun uploadProdukPhoto(localUri: android.net.Uri, produkId: String) = 
        firebaseDataSource.uploadProdukPhoto(localUri, produkId)
}
