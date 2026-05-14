package id.mohamadsuhendy.vanishabakery.ui.penjualan

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import id.mohamadsuhendy.vanishabakery.data.model.Mitra
import id.mohamadsuhendy.vanishabakery.data.model.Produk
import id.mohamadsuhendy.vanishabakery.data.model.Rute
import id.mohamadsuhendy.vanishabakery.data.model.User
import id.mohamadsuhendy.vanishabakery.data.repository.PenjualanItem
import id.mohamadsuhendy.vanishabakery.databinding.ActivityRapidEntryBinding
import id.mohamadsuhendy.vanishabakery.utils.Result
import id.mohamadsuhendy.vanishabakery.utils.showToast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class RapidEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRapidEntryBinding
    private val app get() = application as VanishaBakeryApp

    private var allSales = listOf<User>()
    private var selectedSales: User? = null
    private var allRute = listOf<Rute>()
    private var selectedRute: Rute? = null
    private var mitraList = listOf<Mitra>()
    private var produkList = listOf<Produk>()
    private var currentMitraIndex = 0
    private var selectedDate: Calendar = Calendar.getInstance()
    private var savedMitraIndices = mutableSetOf<Int>()

    /**
     * State map: menyimpan snapshot input produk per mitra index.
     * Key = mitraIndex, Value = list snapshot ProdukEntry.
     * Ini yang memungkinkan data tidak hilang saat navigasi maju/mundur.
     */
    private val mitraEntryState = mutableMapOf<Int, List<ProdukEntryAdapter.ProdukEntry>>()

    private lateinit var produkAdapter: ProdukEntryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRapidEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupProdukAdapter()
        setupListeners()
        loadInitialData()
    }

    private fun setupProdukAdapter() {
        produkAdapter = ProdukEntryAdapter { updateSummary() }
        binding.rvProdukEntry.adapter = produkAdapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnPilihTanggal.setOnClickListener { showDatePicker() }

        // Sales dropdown
        binding.acSales.setOnClickListener { binding.acSales.showDropDown() }
        binding.acSales.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                // "Semua Sales"
                selectedSales = null
            } else {
                selectedSales = allSales[position - 1]
            }
            loadRuteForSales()
        }

        // Rute dropdown
        binding.acRute.setOnClickListener { binding.acRute.showDropDown() }
        binding.acRute.setOnItemClickListener { _, _, position, _ ->
            selectedRute = allRute[position]
            loadMitraForRute()
        }

        binding.btnSimpanLanjut.setOnClickListener { saveAndNext() }
        binding.btnPrevMitra.setOnClickListener { navigateToPrevMitra() }

        updateDateText()
    }

    private fun loadInitialData() {
        lifecycleScope.launch {
            // Load sales list
            allSales = app.authRepository.getAllUsers().filter { !it.isAdmin() }
            val salesNames = mutableListOf("Semua Sales")
            salesNames.addAll(allSales.map { it.nama })
            val salesAdapter = ArrayAdapter(
                this@RapidEntryActivity,
                android.R.layout.simple_dropdown_item_1line,
                salesNames
            )
            binding.acSales.setAdapter(salesAdapter)

            // Load all rute initially
            allRute = app.ruteRepository.observeAllRute().first()
            setupRuteDropdown()

            // Load produk
            produkList = app.produkRepository.observeAllProduk().first()
            produkAdapter.setProducts(produkList)
        }
    }

    private fun loadRuteForSales() {
        lifecycleScope.launch {
            allRute = if (selectedSales != null) {
                app.ruteRepository.observeRuteByStaff(selectedSales!!.uid).first()
            } else {
                app.ruteRepository.observeAllRute().first()
            }
            setupRuteDropdown()
            // Reset rute selection
            selectedRute = null
            binding.acRute.setText("", false)
            hideEntryUI()
        }
    }

    private fun setupRuteDropdown() {
        val ruteNames = allRute.map { it.namaRute }
        val ruteAdapter = ArrayAdapter(
            this@RapidEntryActivity,
            android.R.layout.simple_dropdown_item_1line,
            ruteNames
        )
        binding.acRute.setAdapter(ruteAdapter)
    }

    private fun loadMitraForRute() {
        val rute = selectedRute ?: return

        // Hitung start dan end untuk selectedDate
        val startCal = selectedDate.clone() as Calendar
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        val endCal = startCal.clone() as Calendar
        endCal.add(Calendar.DAY_OF_MONTH, 1)

        val startTs = com.google.firebase.Timestamp(startCal.time)
        val endTs = com.google.firebase.Timestamp(endCal.time)

        lifecycleScope.launch {
            mitraList = app.mitraRepository.observeAllMitra().first()
                .filter { it.ruteId == rute.id && it.isApproved() }
                .sortedBy { it.namaToko }

            if (mitraList.isEmpty()) {
                showToast("Tidak ada mitra aktif di rute ${rute.namaRute}")
                hideEntryUI()
                return@launch
            }

            // Load existing Penjualan data for this rute and date
            val existingSales = app.firebaseDataSource.observePenjualanByPeriode(startTs, endTs).first()
                .filter { it.ruteId == rute.id }

            currentMitraIndex = 0
            savedMitraIndices.clear()
            mitraEntryState.clear()

            // Pre-fill mitraEntryState with existingSales data
            mitraList.forEachIndexed { index, mitra ->
                val salesForMitra = existingSales.filter { it.mitraId == mitra.id }
                if (salesForMitra.isNotEmpty()) {
                    savedMitraIndices.add(index) // Tandai sudah disimpan sebelumnya
                    
                    // Buat snapshot list dari produkList
                    val snapshot = produkList.map { produk ->
                        val saleItem = salesForMitra.find { it.namaProduk == produk.nama }
                        ProdukEntryAdapter.ProdukEntry(
                            produk = produk,
                            kirim = saleItem?.jumlahKirim ?: 0,
                            retur = saleItem?.jumlahRetur ?: 0,
                            existingId = saleItem?.id
                        )
                    }
                    mitraEntryState[index] = snapshot
                }
            }

            showEntryUI()
            showCurrentMitra()
        }
    }

    /**
     * Simpan state input produk saat ini ke map sebelum pindah mitra.
     */
    private fun saveCurrentMitraState() {
        mitraEntryState[currentMitraIndex] = produkAdapter.getCurrentSnapshot()
    }

    /**
     * Navigasi ke mitra sebelumnya. Simpan state dulu, lalu restore state mitra tujuan.
     */
    private fun navigateToPrevMitra() {
        if (currentMitraIndex > 0) {
            // Simpan state mitra saat ini
            saveCurrentMitraState()
            // Pindah ke mitra sebelumnya
            currentMitraIndex--
            showCurrentMitra()
        }
    }

    private fun showCurrentMitra() {
        if (mitraList.isEmpty()) return
        val mitra = mitraList[currentMitraIndex]

        binding.tvMitraNama.text = mitra.namaToko
        binding.tvMitraAlamat.text = mitra.alamat
        binding.tvMitraCounter.text = "${currentMitraIndex + 1}/${mitraList.size}"

        // Restore state jika pernah diisi, atau reset jika mitra baru
        val savedState = mitraEntryState[currentMitraIndex]
        if (savedState != null) {
            produkAdapter.restoreEntries(savedState)
        } else {
            produkAdapter.resetAll()
        }
        updateSummary()

        binding.progressMitra.max = mitraList.size
        binding.progressMitra.progress = savedMitraIndices.size
        binding.tvProgress.text = "${savedMitraIndices.size}/${mitraList.size} mitra tersimpan"

        // Tombol Sebelum: disable jika sudah di mitra pertama
        binding.btnPrevMitra.isEnabled = currentMitraIndex > 0
        binding.btnPrevMitra.alpha = if (currentMitraIndex > 0) 1.0f else 0.5f

        // Tombol Simpan: ubah teks sesuai posisi
        binding.btnSimpanLanjut.text = if (currentMitraIndex == mitraList.size - 1) {
            "SIMPAN & SELESAI"
        } else {
            "SIMPAN & LANJUT"
        }
    }

    private fun updateSummary() {
        val totalTerjual = produkAdapter.getTotalTerjual()
        val totalOmset = produkAdapter.getTotalOmset()

        binding.tvTotalTerjual.text = "Terjual: $totalTerjual pcs"
        val formatter = java.text.NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        binding.tvTotalOmset.text = "Omset: ${formatter.format(totalOmset).replace(",00", "")}"
    }

    private fun saveAndNext() {
        if (mitraList.isEmpty()) return
        val mitra = mitraList[currentMitraIndex]
        val rute = selectedRute ?: return
        val filledEntries = produkAdapter.getFilledEntries()

        if (filledEntries.isEmpty()) {
            if (currentMitraIndex < mitraList.size - 1) {
                // Simpan state kosong juga agar saat kembali masih konsisten
                saveCurrentMitraState()
                currentMitraIndex++
                showCurrentMitra()
                showToast("Mitra dilewati (tidak ada data)")
            } else {
                showToast("Semua mitra selesai diinput!")
                finish()
            }
            return
        }

        val items = filledEntries.map { entry ->
            PenjualanItem(
                namaProduk = entry.produk.nama,
                hargaSatuan = entry.produk.harga,
                kirim = entry.kirim,
                retur = entry.retur,
                existingId = entry.existingId
            )
        }

        binding.progressSaving.visibility = View.VISIBLE
        binding.btnSimpanLanjut.isEnabled = false
        binding.btnPrevMitra.isEnabled = false

        lifecycleScope.launch {
            val user = app.authRepository.getCurrentUserData()
                ?: run {
                    showToast("User tidak ditemukan")
                    binding.progressSaving.visibility = View.GONE
                    binding.btnSimpanLanjut.isEnabled = true
                    binding.btnPrevMitra.isEnabled = currentMitraIndex > 0
                    return@launch
                }

            val result = app.penjualanRepository.saveBatchPenjualan(
                mitraId = mitra.id,
                mitraNama = mitra.namaToko,
                ruteId = rute.id,
                ruteNama = rute.namaRute,
                tanggalNota = selectedDate.timeInMillis,
                items = items,
                adminId = user.uid,
                adminNama = user.nama
            )

            binding.progressSaving.visibility = View.GONE
            binding.btnSimpanLanjut.isEnabled = true
            binding.btnPrevMitra.isEnabled = currentMitraIndex > 0

            when (result) {
                is Result.Success -> {
                    savedMitraIndices.add(currentMitraIndex)
                    // Simpan state setelah berhasil save
                    saveCurrentMitraState()
                    showToast(result.data)

                    if (currentMitraIndex < mitraList.size - 1) {
                        currentMitraIndex++
                        showCurrentMitra()
                    } else {
                        showToast("Semua mitra di rute ${rute.namaRute} selesai!")
                        finish()
                    }
                }
                is Result.Error -> showToast("Gagal: ${result.message}")
                is Result.Loading -> { /* no-op */ }
            }
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate.set(year, month, day, 0, 0, 0)
                selectedDate.set(Calendar.MILLISECOND, 0)
                updateDateText()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateText() {
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        binding.btnPilihTanggal.text = sdf.format(selectedDate.time)
    }

    private fun showEntryUI() {
        binding.cardMitraHeader.visibility = View.VISIBLE
        binding.cardProdukTable.visibility = View.VISIBLE
        binding.cardSummary.visibility = View.VISIBLE
        binding.bottomBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE
    }

    private fun hideEntryUI() {
        binding.cardMitraHeader.visibility = View.GONE
        binding.cardProdukTable.visibility = View.GONE
        binding.cardSummary.visibility = View.GONE
        binding.bottomBar.visibility = View.GONE
        binding.tvEmptyState.visibility = View.VISIBLE
    }
}
