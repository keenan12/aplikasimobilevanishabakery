package id.mohamadsuhendy.vanishabakery.ui.pengiriman

import androidx.lifecycle.*
import id.mohamadsuhendy.vanishabakery.data.model.Produk
import id.mohamadsuhendy.vanishabakery.data.repository.AuthRepository
import id.mohamadsuhendy.vanishabakery.data.repository.PengirimanRepository
import id.mohamadsuhendy.vanishabakery.data.repository.ProdukRepository
import id.mohamadsuhendy.vanishabakery.utils.Result
import kotlinx.coroutines.launch

class PengirimanViewModel(
    private val pengirimanRepository: PengirimanRepository,
    private val authRepository: AuthRepository,
    private val produkRepository: ProdukRepository
) : ViewModel() {

    val produkList: LiveData<List<Produk>> = produkRepository.observeAllProduk().asLiveData()

    private val _saveState = MutableLiveData<Result<String>>()
    val saveState: LiveData<Result<String>> = _saveState

    fun addPengiriman(
        mitraId: String, mitraNama: String, ruteId: String, ruteNama: String,
        namaProduk: String, jumlah: Int
    ) {
        viewModelScope.launch {
            _saveState.value = Result.Loading
            val user = authRepository.getCurrentUserData()
                ?: run { _saveState.value = Result.Error("User tidak ditemukan"); return@launch }
            _saveState.value = pengirimanRepository.addPengiriman(
                mitraId, mitraNama, ruteId, ruteNama, namaProduk, jumlah, user.uid, user.nama
            )
        }
    }

    class Factory(
        private val pengirimanRepo: PengirimanRepository,
        private val authRepo: AuthRepository,
        private val produkRepo: ProdukRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PengirimanViewModel(pengirimanRepo, authRepo, produkRepo) as T
    }
}
