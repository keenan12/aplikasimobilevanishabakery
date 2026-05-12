package id.mohamadsuhendy.vanishabakery.ui.penjualan

import androidx.lifecycle.*
import id.mohamadsuhendy.vanishabakery.data.model.Produk
import id.mohamadsuhendy.vanishabakery.data.repository.AuthRepository
import id.mohamadsuhendy.vanishabakery.data.repository.PenjualanRepository
import id.mohamadsuhendy.vanishabakery.data.repository.ProdukRepository
import id.mohamadsuhendy.vanishabakery.utils.Result
import kotlinx.coroutines.launch

class PenjualanViewModel(
    private val penjualanRepository: PenjualanRepository,
    private val authRepository: AuthRepository,
    private val produkRepository: ProdukRepository
) : ViewModel() {

    val produkList: LiveData<List<Produk>> = produkRepository.observeAllProduk().asLiveData()

    private val _saveState = MutableLiveData<Result<String>>()
    val saveState: LiveData<Result<String>> = _saveState

    // Auto-calculate sisa when terjual or dikirim changes
    val jumlahSisa = MutableLiveData<Int>()

    fun calculateSisa(dikirim: Int, terjual: Int) {
        jumlahSisa.value = maxOf(0, dikirim - terjual)
    }

    fun addPenjualan(
        pengirimanId: String, mitraId: String, mitraNama: String,
        namaProduk: String, jumlahDikirim: Int, jumlahTerjual: Int, hargaSatuan: Int
    ) {
        if (mitraId.isBlank()) { _saveState.value = Result.Error("Pilih mitra terlebih dahulu"); return }
        if (jumlahTerjual < 0) { _saveState.value = Result.Error("Jumlah terjual tidak valid"); return }
        if (jumlahTerjual > jumlahDikirim) { _saveState.value = Result.Error("Jumlah terjual melebihi jumlah dikirim"); return }
        val totalHarga = hargaSatuan * jumlahTerjual

        viewModelScope.launch {
            _saveState.value = Result.Loading
            val user = authRepository.getCurrentUserData()
                ?: run { _saveState.value = Result.Error("User tidak ditemukan"); return@launch }
            _saveState.value = penjualanRepository.addPenjualan(
                pengirimanId, mitraId, mitraNama, namaProduk,
                jumlahDikirim, jumlahTerjual, hargaSatuan, totalHarga,
                user.uid, user.nama
            )
        }
    }

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
