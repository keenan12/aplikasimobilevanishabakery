package id.mohamadsuhendy.vanishabakery.ui.aktivitas

import androidx.lifecycle.*
import id.mohamadsuhendy.vanishabakery.data.model.LogAktivitas
import id.mohamadsuhendy.vanishabakery.data.remote.FirebaseDataSource
import id.mohamadsuhendy.vanishabakery.data.repository.AuthRepository
import id.mohamadsuhendy.vanishabakery.utils.Result
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Calendar

class AktivitasViewModel(
    private val remoteDataSource: FirebaseDataSource,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _allLogs = mutableListOf<LogAktivitas>()

    private val _logList = MutableLiveData<List<LogAktivitas>>()
    val logList: LiveData<List<LogAktivitas>> = _logList

    private val _deleteState = MutableLiveData<Result<Unit>>()
    val deleteState: LiveData<Result<Unit>> = _deleteState

    // Currently selected month (0 = all, 1-12 = Jan-Dec)
    private var selectedMonth = 0
    private var selectedYear = Calendar.getInstance().get(Calendar.YEAR)
    
    // Selected Sales ID for Admin filter
    private var selectedSalesId = ""
    
    // Search query
    private var searchQuery = ""

    init { observeLogs() }

    private var logJob: kotlinx.coroutines.Job? = null

    private fun observeLogs() {
        authRepository.observeCurrentUser().onEach { user ->
            logJob?.cancel()
            if (user == null) {
                _allLogs.clear()
                _logList.postValue(emptyList())
                return@onEach
            }

            logJob = viewModelScope.launch {
                val flow = if (user.isAdmin()) {
                    remoteDataSource.observeLogAktivitas()
                } else {
                    remoteDataSource.observeLogAktivitasByUser(user.uid)
                }
                
                flow.collect { result ->
                    if (result is Result.Success) {
                        _allLogs.clear()
                        _allLogs.addAll(result.data)
                        applyFilter()
                    } else if (result is Result.Error) {
                        _allLogs.clear()
                        _logList.postValue(emptyList())
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    fun refreshLogs() {
        observeLogs()
    }

    fun filterByMonth(month: Int, year: Int = Calendar.getInstance().get(Calendar.YEAR)) {
        selectedMonth = month
        selectedYear = year
        applyFilter()
    }

    fun filterBySales(salesId: String) {
        selectedSalesId = salesId
        applyFilter()
    }

    fun setSearchQuery(query: String) {
        searchQuery = query
        applyFilter()
    }

    private fun applyFilter() {
        _logList.value = _allLogs.filter { log ->
            // Filter by Sales ID
            val matchSales = if (selectedSalesId.isEmpty()) true else log.userId == selectedSalesId
            
            // Filter by Month
            val matchMonth = if (selectedMonth == 0) true else {
                val cal = Calendar.getInstance().apply {
                    log.createdAt?.let { time = it.toDate() }
                }
                cal.get(Calendar.MONTH) + 1 == selectedMonth && cal.get(Calendar.YEAR) == selectedYear
            }

            // Filter by Search Query
            val matchSearch = if (searchQuery.isEmpty()) true else {
                log.aksi.contains(searchQuery, ignoreCase = true) ||
                log.deskripsi.contains(searchQuery, ignoreCase = true) ||
                log.tipe.contains(searchQuery, ignoreCase = true) ||
                log.userNama.contains(searchQuery, ignoreCase = true)
            }
            
            matchSales && matchMonth && matchSearch
        }.sortedByDescending { it.createdAt }
    }

    fun deleteLogsForCurrentFilter() {
        viewModelScope.launch {
            _deleteState.value = Result.Loading
            try {
                val logsToDelete = _allLogs.filter { log ->
                    val matchSales = if (selectedSalesId.isEmpty()) true else log.userId == selectedSalesId
                    val matchMonth = if (selectedMonth == 0) true else {
                        val cal = Calendar.getInstance().apply { log.createdAt?.let { time = it.toDate() } }
                        cal.get(Calendar.MONTH) + 1 == selectedMonth && cal.get(Calendar.YEAR) == selectedYear
                    }
                    matchSales && matchMonth
                }
                logsToDelete.forEach { remoteDataSource.deleteLogAktivitas(it.id) }
                _deleteState.value = Result.Success(Unit)
            } catch (e: Exception) {
                _deleteState.value = Result.Error(e.message ?: "Gagal menghapus riwayat", e)
            }
        }
    }

    class Factory(
        private val remote: FirebaseDataSource,
        private val auth: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AktivitasViewModel(remote, auth) as T
    }
}
