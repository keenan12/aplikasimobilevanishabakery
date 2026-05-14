package id.mohamadsuhendy.vanishabakery.ui.produk

import android.net.Uri
import androidx.lifecycle.*
import id.mohamadsuhendy.vanishabakery.data.model.LogAktivitas
import id.mohamadsuhendy.vanishabakery.data.model.Produk
import id.mohamadsuhendy.vanishabakery.data.repository.AuthRepository
import id.mohamadsuhendy.vanishabakery.data.repository.ProdukRepository
import id.mohamadsuhendy.vanishabakery.utils.Constants
import id.mohamadsuhendy.vanishabakery.utils.Result
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.launch
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class ProdukViewModel(
    private val repository: ProdukRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    
    val produkList: LiveData<List<Produk>> = combine(
        repository.observeAllProduk(),
        _searchQuery
    ) { list, query ->
        if (query.isEmpty()) list
        else list.filter { 
            it.nama.contains(query, ignoreCase = true) || 
            it.deskripsi.contains(query, ignoreCase = true)
        }
    }.asLiveData()
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val currentUserRole: LiveData<String?> = authRepository.observeCurrentUser()
        .map { it?.role }
        .asLiveData()

    private val _actionState = MutableLiveData<id.mohamadsuhendy.vanishabakery.utils.Result<String>>()
    val actionState: LiveData<id.mohamadsuhendy.vanishabakery.utils.Result<String>> = _actionState

    fun addProduk(produk: Produk) {
        viewModelScope.launch {
            _actionState.value = Result.Loading
            try {
                val finalProduk = produk.copy(createdAt = Timestamp.now())
                repository.addProduk(finalProduk)
                
                authRepository.getCurrentUserData()?.let { user ->
                    authRepository.addLog(LogAktivitas(
                        userId = user.uid, userNama = user.nama, userRole = user.role,
                        tipe = "produk", aksi = "Tambah Produk",
                        deskripsi = "Menambahkan produk: ${produk.nama}",
                        createdAt = Timestamp.now()
                    ))
                }
                
                _actionState.value = Result.Success("Produk ${produk.nama} berhasil ditambahkan")
            } catch (e: Exception) {
                _actionState.value = Result.Error(e.message ?: "Gagal menambah produk")
            }
        }
    }

    fun updateProduk(produk: Produk) {
        viewModelScope.launch {
            _actionState.value = Result.Loading
            try {
                val finalProduk = produk.copy(updatedAt = Timestamp.now())
                repository.updateProduk(finalProduk)
                
                authRepository.getCurrentUserData()?.let { user ->
                    authRepository.addLog(LogAktivitas(
                        userId = user.uid, userNama = user.nama, userRole = user.role,
                        tipe = "produk", aksi = "Update Produk",
                        deskripsi = "Memperbarui produk: ${produk.nama}",
                        createdAt = Timestamp.now()
                    ))
                }
                
                _actionState.value = Result.Success("Produk ${produk.nama} berhasil diperbarui")
            } catch (e: Exception) {
                _actionState.value = Result.Error(e.message ?: "Gagal memperbarui produk")
            }
        }
    }

    fun deleteProduk(produkId: String) {
        viewModelScope.launch {
            _actionState.value = id.mohamadsuhendy.vanishabakery.utils.Result.Loading
            try {
                val user = authRepository.getCurrentUserData()
                repository.deleteProduk(produkId)
                
                // Logging - only if user data is available
                user?.let {
                    authRepository.addLog(id.mohamadsuhendy.vanishabakery.data.model.LogAktivitas(
                        userId = it.uid,
                        userNama = it.nama,
                        userRole = it.role,
                        tipe = "produk",
                        aksi = "Hapus Produk",
                        deskripsi = "Menghapus produk dari sistem",
                        createdAt = com.google.firebase.Timestamp.now()
                    ))
                }
                
                _actionState.value = id.mohamadsuhendy.vanishabakery.utils.Result.Success("Produk berhasil dihapus")
            } catch (e: Exception) {
                _actionState.value = id.mohamadsuhendy.vanishabakery.utils.Result.Error(e.message ?: "Gagal menghapus produk")
            }
        }
    }

    class Factory(
        private val repository: ProdukRepository,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProdukViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProdukViewModel(repository, authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
