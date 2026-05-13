package id.mohamadsuhendy.vanishabakery.ui.penjualan

import androidx.lifecycle.*
import id.mohamadsuhendy.vanishabakery.data.model.Produk
import id.mohamadsuhendy.vanishabakery.data.repository.AuthRepository
import id.mohamadsuhendy.vanishabakery.data.repository.PenjualanRepository
import id.mohamadsuhendy.vanishabakery.data.repository.ProdukRepository
import id.mohamadsuhendy.vanishabakery.utils.Result
import kotlinx.coroutines.launch

/**
 * ViewModel for penjualan — now simplified since input is done via RapidEntryActivity.
 * This ViewModel is kept for listing/viewing penjualan data on the admin side.
 */
class PenjualanViewModel(
    private val penjualanRepository: PenjualanRepository,
    private val authRepository: AuthRepository,
    private val produkRepository: ProdukRepository
) : ViewModel() {

    val produkList: LiveData<List<Produk>> = produkRepository.observeAllProduk().asLiveData()

    private val _saveState = MutableLiveData<Result<String>>()
    val saveState: LiveData<Result<String>> = _saveState

    class Factory(
        private val penjualanRepo: PenjualanRepository,
        private val authRepo: AuthRepository,
        private val produkRepo: ProdukRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PenjualanViewModel(penjualanRepo, authRepo, produkRepo) as T
    }
}
