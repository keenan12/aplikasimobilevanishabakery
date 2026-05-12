package id.mohamadsuhendy.vanishabakery.ui.profile

import android.net.Uri
import androidx.lifecycle.*
import id.mohamadsuhendy.vanishabakery.data.model.User
import id.mohamadsuhendy.vanishabakery.data.remote.FirebaseDataSource
import id.mohamadsuhendy.vanishabakery.data.repository.AuthRepository
import id.mohamadsuhendy.vanishabakery.utils.Result
import id.mohamadsuhendy.vanishabakery.data.repository.RuteRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val remoteDataSource: FirebaseDataSource,
    private val ruteRepository: RuteRepository
) : ViewModel() {

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _allUsers = MutableLiveData<List<User>>()
    val allUsers: LiveData<List<User>> = _allUsers

    private val _photoUploadState = MutableLiveData<Result<String>>()
    val photoUploadState: LiveData<Result<String>> = _photoUploadState

    private val _sampulUploadState = MutableLiveData<Result<String>>()
    val sampulUploadState: LiveData<Result<String>> = _sampulUploadState

    private val _registerState = MutableLiveData<Result<String>>()
    val registerState: LiveData<Result<String>> = _registerState

    init { observeCurrentUser() }

    private fun observeCurrentUser() {
        authRepository.observeCurrentUser()
            .onEach { _currentUser.value = it }
            .launchIn(viewModelScope)
    }

    fun observeAllUsers() {
        authRepository.observeAllUsers()
            .onEach { _allUsers.value = it }
            .launchIn(viewModelScope)
    }

    @Deprecated("Use observeAllUsers for real-time updates", ReplaceWith("observeAllUsers()"))
    fun loadAllUsers() {
        observeAllUsers()
    }

    /** Upload photo using raw bytes */
    fun uploadProfilePhoto(bytes: ByteArray) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _photoUploadState.value = Result.Loading
            try {
                val url = remoteDataSource.uploadProfilePhotoBytes(bytes, uid)
                authRepository.updateProfilePhoto(uid, url)
                _photoUploadState.value = Result.Success(url)
            } catch (e: Exception) {
                _photoUploadState.value = Result.Error(e.message ?: "Gagal upload foto", e)
            }
        }
    }

    /** Upload cover photo using raw bytes */
    fun uploadSampulPhoto(bytes: ByteArray) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _sampulUploadState.value = Result.Loading
            try {
                val url = remoteDataSource.uploadProfilePhotoBytes(bytes, uid + "_sampul")
                authRepository.updateSampulPhoto(uid, url)
                _sampulUploadState.value = Result.Success(url)
            } catch (e: Exception) {
                _sampulUploadState.value = Result.Error(e.message ?: "Gagal upload sampul", e)
            }
        }
    }

    fun toggleUserActive(uid: String, isActive: Boolean) {
        viewModelScope.launch {
            authRepository.setUserActive(uid, isActive)
        }
    }

    fun registerUser(email: String, pass: String, nama: String, role: String) {
        viewModelScope.launch {
            _registerState.value = Result.Loading
            _registerState.value = authRepository.createUser(email, pass, nama, role)
        }
    }

    fun resetRegisterState() {
        _registerState.value = Result.Loading
    }

    // --- Rute Management ---
    private val _ruteList = MutableLiveData<List<id.mohamadsuhendy.vanishabakery.data.model.Rute>>()
    val ruteList: LiveData<List<id.mohamadsuhendy.vanishabakery.data.model.Rute>> = _ruteList

    fun observeRute() {
        ruteRepository.observeAllRute()
            .onEach { _ruteList.value = it }
            .launchIn(viewModelScope)
    }

    private val _addRuteState = MutableLiveData<Result<String>>()
    val addRuteState: LiveData<Result<String>> = _addRuteState

    fun addRute(namaRute: String, kode: String, staffId: String, staffNama: String) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            _addRuteState.value = Result.Loading
            _addRuteState.value = ruteRepository.addRute(
                namaRute, kode, staffId, staffNama, 
                admin.uid, admin.nama
            )
        }
    }

    private val _deleteRuteState = MutableLiveData<Result<Unit>>()
    val deleteRuteState: LiveData<Result<Unit>> = _deleteRuteState

    fun deleteRute(ruteId: String) {
        viewModelScope.launch {
            _deleteRuteState.value = Result.Loading
            _deleteRuteState.value = ruteRepository.deleteRute(ruteId)
        }
    }

    fun resetDeleteRuteState() {
        _deleteRuteState.value = Result.Loading
    }

    private val _updateRuteState = MutableLiveData<Result<Unit>>()
    val updateRuteState: LiveData<Result<Unit>> = _updateRuteState

    fun updateRute(ruteId: String, namaRute: String, kode: String) {
        viewModelScope.launch {
            _updateRuteState.value = Result.Loading
            _updateRuteState.value = ruteRepository.updateRute(ruteId, namaRute, kode)
        }
    }

    fun resetUpdateRuteState() {
        _updateRuteState.value = Result.Loading
    }

    fun logout() = authRepository.logout()

    class Factory(
        private val authRepo: AuthRepository,
        private val remote: FirebaseDataSource,
        private val ruteRepo: RuteRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProfileViewModel(authRepo, remote, ruteRepo) as T
    }
}
