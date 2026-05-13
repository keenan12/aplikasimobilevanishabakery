package id.mohamadsuhendy.vanishabakery.ui.admin

import androidx.lifecycle.*
import id.mohamadsuhendy.vanishabakery.data.model.Produk
import id.mohamadsuhendy.vanishabakery.data.model.Rute
import id.mohamadsuhendy.vanishabakery.data.repository.AuthRepository
import id.mohamadsuhendy.vanishabakery.data.repository.ProdukRepository
import id.mohamadsuhendy.vanishabakery.data.repository.RuteRepository
import id.mohamadsuhendy.vanishabakery.data.repository.StokRuteRepository
import id.mohamadsuhendy.vanishabakery.utils.Result
import kotlinx.coroutines.launch

class StokRuteViewModel(
    private val stokRuteRepository: StokRuteRepository,
    private val ruteRepository: RuteRepository,
    private val produkRepository: ProdukRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val ruteList: LiveData<List<Rute>> = ruteRepository.observeAllRute().asLiveData()
    val produkList: LiveData<List<Produk>> = produkRepository.observeAllProduk().asLiveData()

    private val _saveState = MutableLiveData<Result<String>>()
    val saveState: LiveData<Result<String>> = _saveState

    fun checkExistingStok(ruteId: String, tanggal: Long) = liveData {
        emit(stokRuteRepository.getStokRuteByExactDate(ruteId, tanggal))
    }

    fun saveStokRute(
        ruteId: String,
        ruteNama: String,
        tanggal: Long,
        stokData: Map<String, Int>
    ) {
        viewModelScope.launch {
            _saveState.value = Result.Loading
            val user = authRepository.getCurrentUserData()
            if (user != null) {
                _saveState.value = stokRuteRepository.saveStokRute(
                    ruteId, ruteNama, tanggal, stokData, user.uid, user.nama
                )
            } else {
                _saveState.value = Result.Error("User tidak ditemukan")
            }
        }
    }

    class Factory(
        private val stokRuteRepo: StokRuteRepository,
        private val ruteRepo: RuteRepository,
        private val produkRepo: ProdukRepository,
        private val authRepo: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StokRuteViewModel(stokRuteRepo, ruteRepo, produkRepo, authRepo) as T
    }
}
