package id.mohamadsuhendy.vanishabakery.ui.mitra

import android.net.Uri
import androidx.lifecycle.*
import id.mohamadsuhendy.vanishabakery.data.model.Mitra
import id.mohamadsuhendy.vanishabakery.data.repository.AuthRepository
import id.mohamadsuhendy.vanishabakery.data.repository.MitraRepository
import id.mohamadsuhendy.vanishabakery.data.repository.RuteRepository
import id.mohamadsuhendy.vanishabakery.utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.map

class MitraViewModel(
    private val mitraRepository: MitraRepository,
    private val authRepository: AuthRepository,
    private val ruteRepository: RuteRepository
) : ViewModel() {

    private val _mitraList = MutableLiveData<List<Mitra>>()
    val mitraList: LiveData<List<Mitra>> = _mitraList

    private val _saveState = MutableLiveData<Result<String>>()
    val saveState: LiveData<Result<String>> = _saveState

    private val _updateState = MutableLiveData<Result<Unit>>()
    val updateState: LiveData<Result<Unit>> = _updateState

    private val _mitraDetail = MutableLiveData<Mitra?>()
    val mitraDetail: LiveData<Mitra?> = _mitraDetail

    private val _ruteList = MutableLiveData<List<id.mohamadsuhendy.vanishabakery.data.model.Rute>>()
    val ruteList: LiveData<List<id.mohamadsuhendy.vanishabakery.data.model.Rute>> = _ruteList

    private val _staffList = MutableLiveData<List<id.mohamadsuhendy.vanishabakery.data.model.User>>()
    val staffList: LiveData<List<id.mohamadsuhendy.vanishabakery.data.model.User>> = _staffList

    private val searchQuery = MutableStateFlow("")
    private val currentFilter = MutableStateFlow("all")
    private val selectedStaffId = MutableStateFlow("")
    private val selectedRuteId = MutableStateFlow("")

    init { 
        setupReactiveFilter() 
        observeRute()
        observeStaff()
    }

    private fun observeStaff() {
        viewModelScope.launch {
            authRepository.observeAllUsers()
                .map { users -> users.filter { it.isStaff() } }
                .onEach { _staffList.value = it }
                .launchIn(this)
        }
    }

    private fun observeRute() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUserData() ?: return@launch
            val flow = if (user.isAdmin()) {
                ruteRepository.observeAllRute()
            } else {
                ruteRepository.observeRuteByStaff(user.uid)
            }
            
            flow.onEach { _ruteList.value = it }.launchIn(this)
        }
    }

    private fun setupReactiveFilter() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUserData() ?: return@launch
            
            // 1. Base flow based on role
            val baseFlow = if (user.isAdmin()) {
                mitraRepository.observeAllMitra()
            } else {
                mitraRepository.observeMitraByStaff(user.uid)
            }

            // 2. Combine only the 4 string filters first
            val filtersFlow = combine(
                searchQuery,
                currentFilter,
                selectedStaffId,
                selectedRuteId
            ) { query, filter, staff, rute ->
                // Bundle filters into a small helper object
                FilterParams(query, filter, staff, rute)
            }

            // 3. Combine base data with the bundled filters
            combine(baseFlow, filtersFlow) { list, params ->
                list.filter { mitra ->
                    val matchFilter = when (params.filter) {
                        "pending" -> mitra.isPending()
                        "approved" -> mitra.isApproved()
                        else -> !mitra.isDeleted()
                    }
                    val matchSearch = if (params.query.isEmpty()) true 
                        else mitra.namaToko.contains(params.query, ignoreCase = true) || 
                             mitra.alamat.contains(params.query, ignoreCase = true)
                             
                    val matchStaff = if (params.staff.isEmpty()) true else mitra.staffId == params.staff
                    val matchRute = if (params.rute.isEmpty()) true else mitra.ruteId == params.rute
                    
                    matchFilter && matchSearch && matchStaff && matchRute
                }
            }.onEach { 
                _mitraList.value = it 
            }.launchIn(this)
        }
    }

    // Small helper class for filtering
    private data class FilterParams(
        val query: String,
        val filter: String,
        val staff: String,
        val rute: String
    )

    fun filterMitra(filter: String) {
        currentFilter.value = filter
    }

    fun filterByStaff(staffId: String) {
        selectedStaffId.value = staffId
    }

    fun filterByRute(ruteId: String) {
        selectedRuteId.value = ruteId
    }

    fun resetFilters() {
        currentFilter.value = "all"
        searchQuery.value = ""
        selectedStaffId.value = ""
        selectedRuteId.value = ""
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun loadMitraDetail(id: String) {
        viewModelScope.launch {
            _mitraDetail.value = mitraRepository.getMitraById(id)
        }
    }

    fun addMitra(
        namaToko: String, fotoUri: Uri,
        latitude: Double, longitude: Double, alamat: String,
        ruteId: String, ruteNama: String
    ) {
        viewModelScope.launch {
            _saveState.value = Result.Loading
            val user = authRepository.getCurrentUserData()
                ?: run { _saveState.value = Result.Error("User tidak ditemukan"); return@launch }
            _saveState.value = mitraRepository.addMitra(
                namaToko, fotoUri, latitude, longitude, alamat,
                user.uid, user.nama, user.role, ruteId, ruteNama
            )
        }
    }

    fun approveMitra(mitraId: String) {
        viewModelScope.launch {
            _updateState.value = Result.Loading
            val user = authRepository.getCurrentUserData()
                ?: run { _updateState.value = Result.Error("User tidak ditemukan"); return@launch }
            _updateState.value = mitraRepository.updateMitraStatus(
                mitraId, "approved", user.uid, user.nama
            )
        }
    }

    fun rejectMitra(mitraId: String) {
        viewModelScope.launch {
            _updateState.value = Result.Loading
            val user = authRepository.getCurrentUserData()
                ?: run { _updateState.value = Result.Error("User tidak ditemukan"); return@launch }
            _updateState.value = mitraRepository.updateMitraStatus(
                mitraId, "rejected", user.uid, user.nama
            )
        }
    }

    fun deleteMitra(mitraId: String) {
        viewModelScope.launch {
            _updateState.value = Result.Loading
            val user = authRepository.getCurrentUserData()
                ?: run { _updateState.value = Result.Error("User tidak ditemukan"); return@launch }
            _updateState.value = mitraRepository.deleteMitra(mitraId, user.uid, user.nama)
        }
    }

    fun archiveMitra(mitraId: String) {
        viewModelScope.launch {
            _updateState.value = Result.Loading
            val user = authRepository.getCurrentUserData()
                ?: run { _updateState.value = Result.Error("User tidak ditemukan"); return@launch }
            _updateState.value = mitraRepository.archiveMitra(mitraId, user.uid, user.nama)
        }
    }

    class Factory(
        private val mitraRepo: MitraRepository,
        private val authRepo: AuthRepository,
        private val ruteRepo: RuteRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MitraViewModel(mitraRepo, authRepo, ruteRepo) as T
    }
}
