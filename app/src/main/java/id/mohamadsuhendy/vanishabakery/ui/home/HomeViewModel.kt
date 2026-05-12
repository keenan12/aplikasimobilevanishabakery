package id.mohamadsuhendy.vanishabakery.ui.home

import androidx.lifecycle.*
import id.mohamadsuhendy.vanishabakery.data.model.LogAktivitas
import id.mohamadsuhendy.vanishabakery.data.model.User
import id.mohamadsuhendy.vanishabakery.data.remote.FirebaseDataSource
import id.mohamadsuhendy.vanishabakery.data.repository.AuthRepository
import id.mohamadsuhendy.vanishabakery.data.repository.MitraRepository
import id.mohamadsuhendy.vanishabakery.data.repository.PengirimanRepository
import id.mohamadsuhendy.vanishabakery.data.repository.PenjualanRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val mitraRepository: MitraRepository,
    private val pengirimanRepository: PengirimanRepository,
    private val penjualanRepository: PenjualanRepository,
    private val remoteDataSource: FirebaseDataSource
) : ViewModel() {

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _totalMitra = MutableLiveData<Int>()
    val totalMitra: LiveData<Int> = _totalMitra

    private val _pengirimanHariIni = MutableLiveData<Int>()
    val pengirimanHariIni: LiveData<Int> = _pengirimanHariIni

    private val _penjualanHariIni = MutableLiveData<Int>()
    val penjualanHariIni: LiveData<Int> = _penjualanHariIni

    private val _pendingMitra = MutableLiveData<Int>()
    val pendingMitra: LiveData<Int> = _pendingMitra

    private val _recentLog = MutableLiveData<List<LogAktivitas>>()
    val recentLog: LiveData<List<LogAktivitas>> = _recentLog

    private val _visitedMitraCount = MutableLiveData<Int>()
    val visitedMitraCount: LiveData<Int> = _visitedMitraCount

    private val _unvisitedMitraCount = MutableLiveData<Int>()
    val unvisitedMitraCount: LiveData<Int> = _unvisitedMitraCount

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        authRepository.observeCurrentUser()
            .onEach { user ->
                _currentUser.value = user
                user?.let {
                    observeStats(it)
                    observeRecentLog(it)
                    observeNotifikasi(it)
                }
            }
            .launchIn(viewModelScope)
    }

    private val _unreadNotifCount = MutableLiveData<Int>()
    val unreadNotifCount: LiveData<Int> = _unreadNotifCount

    private fun observeNotifikasi(user: User) {
        remoteDataSource.observeNotifikasiByUser(user.uid, user.isAdmin()).onEach { result ->
            if (result is id.mohamadsuhendy.vanishabakery.utils.Result.Success) {
                _unreadNotifCount.value = result.data.count { !it.isRead }
            }
        }.launchIn(viewModelScope)
    }

    private val _omsetBulanIni = MutableLiveData<Int>()
    val omsetBulanIni: LiveData<Int> = _omsetBulanIni

    private val _topProduk = MutableLiveData<List<Pair<String, Int>>>()
    val topProduk: LiveData<List<Pair<String, Int>>> = _topProduk

    private val _chartData = MutableLiveData<List<Pair<String, Float>>>()
    val chartData: LiveData<List<Pair<String, Float>>> = _chartData

    private var _lastPenjualanList: List<id.mohamadsuhendy.vanishabakery.data.model.Penjualan>? = null

    private fun observeStats(user: User) {
        val mitraFlow = if (user.isAdmin()) mitraRepository.observeAllMitra() else mitraRepository.observeMitraByStaff(user.uid)
        val pengirimanFlow = if (user.isAdmin()) pengirimanRepository.observeAllPengiriman() else pengirimanRepository.observePengirimanByStaff(user.uid)
        val penjualanFlow = if (user.isAdmin()) penjualanRepository.observeAllPenjualan() else penjualanRepository.observePenjualanByStaff(user.uid)

        // Real-time Counts
        mitraFlow.onEach { mitras ->
            val approvedCount = mitras.filter { it.isApproved() }.size
            _totalMitra.value = approvedCount
            
            // Re-trigger visited calculation when mitra count changes
            updateVisitedStats(approvedCount)
        }.launchIn(viewModelScope)

        mitraRepository.getPendingCount().onEach { _pendingMitra.value = it }.launchIn(viewModelScope)
        
        // Today's Stats
        pengirimanFlow.onEach { list ->
            val today = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
            }.time
            
            val visitedIds = list
                .filter { it.tanggal?.toDate()?.after(today) == true }
                .map { it.mitraId }
                .distinct()
            
            _visitedMitraCount.value = visitedIds.size
            _unvisitedMitraCount.value = ((_totalMitra.value ?: 0) - visitedIds.size).coerceAtLeast(0)
            _pengirimanHariIni.value = list.filter { it.tanggal?.toDate()?.after(today) == true }.size
        }.launchIn(viewModelScope)

        penjualanFlow.onEach { list ->
            _lastPenjualanList = list
            calculateAdvancedStats(list)
            val today = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
            }.time
            _penjualanHariIni.value = list.filter { it.tanggal?.toDate()?.after(today) == true }.sumOf { it.totalHarga }
        }.launchIn(viewModelScope)
    }

    private fun updateVisitedStats(totalApproved: Int) {
        val visited = _visitedMitraCount.value ?: 0
        _unvisitedMitraCount.value = (totalApproved - visited).coerceAtLeast(0)
    }

    private var selectedMonthForStats = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
    private var selectedYearForStats = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

    fun setFilterMonth(monthIndex: Int) {
        selectedMonthForStats = monthIndex
        _lastPenjualanList?.let { calculateAdvancedStats(it) }
    }

    fun setFilterYear(year: Int) {
        selectedYearForStats = year
        _lastPenjualanList?.let { calculateAdvancedStats(it) }
    }

    private fun calculateAdvancedStats(list: List<id.mohamadsuhendy.vanishabakery.data.model.Penjualan>) {
        val cal = java.util.Calendar.getInstance()
        val currentMonth = selectedMonthForStats
        val currentYear = selectedYearForStats

        var totalOmset = 0
        val productCounts = mutableMapOf<String, Int>()

        // For traffic chart (Entire selected month)
        val sdf = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault())
        val monthTrafficMap = mutableMapOf<String, Float>()
        
        // Initialize map with all days of the selected month
        val tempCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, currentYear)
            set(java.util.Calendar.MONTH, currentMonth)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        val maxDay = tempCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        for (i in 1..maxDay) {
            tempCal.set(java.util.Calendar.DAY_OF_MONTH, i)
            monthTrafficMap[sdf.format(tempCal.time)] = 0f
        }

        for (p in list) {
            // Count for top products & Omset
            val date = p.tanggal?.toDate() ?: p.createdAt?.toDate() ?: continue
            cal.time = date
            
            if (cal.get(java.util.Calendar.MONTH) == currentMonth &&
                cal.get(java.util.Calendar.YEAR) == currentYear) {
                totalOmset += p.totalHarga
                productCounts[p.namaProduk] = (productCounts[p.namaProduk] ?: 0) + p.jumlahTerjual
                
                // Add to traffic map
                val dateStr = sdf.format(date)
                if (monthTrafficMap.containsKey(dateStr)) {
                    monthTrafficMap[dateStr] = monthTrafficMap[dateStr]!! + p.totalHarga.toFloat()
                }
            }
        }

        _omsetBulanIni.value = totalOmset
        
        val top = productCounts.toList()
            .sortedByDescending { it.second }
            .take(5) // Show top 5
        _topProduk.value = top

        // Sort chart data by day
        _chartData.value = monthTrafficMap.toList().sortedBy { it.first }
    }

    private val _photoUploadState = MutableLiveData<id.mohamadsuhendy.vanishabakery.utils.Result<String>>()
    val photoUploadState: LiveData<id.mohamadsuhendy.vanishabakery.utils.Result<String>> = _photoUploadState

    fun uploadProfilePhoto(bytes: ByteArray) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _photoUploadState.value = id.mohamadsuhendy.vanishabakery.utils.Result.Loading
            try {
                val url = remoteDataSource.uploadProfilePhotoBytes(bytes, uid)
                authRepository.updateProfilePhoto(uid, url)
                _photoUploadState.value = id.mohamadsuhendy.vanishabakery.utils.Result.Success(url)
            } catch (e: Exception) {
                _photoUploadState.value = id.mohamadsuhendy.vanishabakery.utils.Result.Error(e.message ?: "Gagal upload foto", e)
            }
        }
    }

    private fun observeRecentLog(user: User) {
        val flow = if (user.isAdmin()) {
            remoteDataSource.observeLogAktivitas()
        } else {
            remoteDataSource.observeLogAktivitasByUser(user.uid)
        }
        flow.onEach { result ->
            if (result is id.mohamadsuhendy.vanishabakery.utils.Result.Success) {
                _recentLog.value = result.data.take(5)
            } else if (result is id.mohamadsuhendy.vanishabakery.utils.Result.Error) {
                _recentLog.value = emptyList()
            }
        }.launchIn(viewModelScope)
    }

    class Factory(
        private val auth: AuthRepository,
        private val mitra: MitraRepository,
        private val pengiriman: PengirimanRepository,
        private val penjualan: PenjualanRepository,
        private val remote: FirebaseDataSource
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(auth, mitra, pengiriman, penjualan, remote) as T
    }
}
