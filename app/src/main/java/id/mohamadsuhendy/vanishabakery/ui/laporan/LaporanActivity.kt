package id.mohamadsuhendy.vanishabakery.ui.laporan

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Timestamp
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import id.mohamadsuhendy.vanishabakery.data.model.Penjualan
import id.mohamadsuhendy.vanishabakery.data.model.StokRute
import id.mohamadsuhendy.vanishabakery.databinding.ActivityLaporanBinding
import id.mohamadsuhendy.vanishabakery.ui.penjualan.NotaPenjualanAdapter
import id.mohamadsuhendy.vanishabakery.utils.ExcelExportUtil
import id.mohamadsuhendy.vanishabakery.utils.Result
import id.mohamadsuhendy.vanishabakery.utils.showToast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class LaporanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLaporanBinding
    private val app get() = application as VanishaBakeryApp

    private var selectedMonth = Calendar.getInstance().get(Calendar.MONTH)
    private var selectedYear = Calendar.getInstance().get(Calendar.YEAR)
    private var currentPenjualanList: List<Penjualan> = emptyList()
    private var filteredPenjualanList: List<Penjualan> = emptyList()
    private var currentStokRuteList: List<StokRute> = emptyList()
    private var lastExportedFile: File? = null

    private lateinit var notaAdapter: NotaPenjualanAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLaporanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNotaAdapter()
        setupListeners()
        updateMonthLabel()
        loadData()
    }

    private fun setupNotaAdapter() {
        notaAdapter = NotaPenjualanAdapter(
            onEdit = { penjualan -> showEditDialog(penjualan) },
            onDelete = { penjualan -> showDeleteConfirmation(penjualan) }
        )
        binding.rvNotaPenjualan.adapter = notaAdapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnPilihBulan.setOnClickListener { showMonthPicker() }

        binding.btnDownloadExcel.setOnClickListener { exportToExcel() }

        binding.btnShareExcel.setOnClickListener {
            lastExportedFile?.let { file ->
                if (file.exists()) {
                    ExcelExportUtil.shareFile(this, file)
                } else {
                    exportToExcel(shareAfter = true)
                }
            } ?: exportToExcel(shareAfter = true)
        }

        binding.etSearchLaporan.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applySearch(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun applySearch(query: String) {
        filteredPenjualanList = if (query.isEmpty()) {
            currentPenjualanList
        } else {
            currentPenjualanList.filter {
                it.mitraNama.contains(query, ignoreCase = true) ||
                it.namaProduk.contains(query, ignoreCase = true)
            }
        }
        notaAdapter.submitList(filteredPenjualanList.sortedWith(
            compareBy<Penjualan> { it.mitraNama }.thenBy { it.namaProduk }
        ))
    }

    private fun showMonthPicker() {
        // Use DatePickerDialog showing only month/year
        val dialog = DatePickerDialog(this, { _, year, month, _ ->
            selectedMonth = month
            selectedYear = year
            updateMonthLabel()
            loadData()
        }, selectedYear, selectedMonth, 1)

        // Hide day picker (only show month + year)
        dialog.datePicker.findViewById<View>(
            resources.getIdentifier("day", "id", "android")
        )?.visibility = View.GONE

        dialog.show()
    }

    private fun updateMonthLabel() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.YEAR, selectedYear)
        }
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        binding.btnPilihBulan.text = sdf.format(cal.time)
    }

    private fun getPeriodLabel(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.YEAR, selectedYear)
        }
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        return sdf.format(cal.time)
    }

    private fun loadData() {
        binding.progressBar.visibility = View.VISIBLE
        binding.cardRingkasan.visibility = View.GONE
        binding.bottomBar.visibility = View.GONE
        binding.tvEmptyState.visibility = View.GONE
        binding.tvDetailHeader.visibility = View.GONE
        binding.rvNotaPenjualan.visibility = View.GONE
        binding.cardSearchLaporan.visibility = View.GONE
        binding.etSearchLaporan.text.clear()

        lifecycleScope.launch {
            try {
                val startCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.MONTH, 1)
                }

                val startTs = Timestamp(startCal.time)
                val endTs = Timestamp(endCal.time)

                currentPenjualanList = app.firebaseDataSource
                    .observePenjualanByPeriode(startTs, endTs)
                    .first()

                currentStokRuteList = app.firebaseDataSource
                    .observeAllStokRuteByPeriode(startTs, endTs)
                    .first()

                binding.progressBar.visibility = View.GONE

                if (currentPenjualanList.isEmpty()) {
                    binding.tvEmptyState.text = "Belum ada data penjualan untuk ${getPeriodLabel()}"
                    binding.tvEmptyState.visibility = View.VISIBLE
                    return@launch
                }

                showSummary()
                showNotaList()
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.tvEmptyState.text = "Gagal memuat data: ${e.message}"
                binding.tvEmptyState.visibility = View.VISIBLE
            }
        }
    }

    private fun showSummary() {
        val grouped = currentPenjualanList.groupBy { it.mitraId }
        val totalKirim = currentPenjualanList.sumOf { it.jumlahKirim }
        val totalRetur = currentPenjualanList.sumOf { it.jumlahRetur }
        val totalTerjual = currentPenjualanList.sumOf { it.jumlahTerjual }
        val totalOmset = currentPenjualanList.sumOf { it.totalHarga }

        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

        binding.tvTotalMitra.text = "Mitra: ${grouped.size}"
        binding.tvTotalKirim.text = "Kirim: $totalKirim pcs"
        binding.tvTotalTerjual.text = "Terjual: $totalTerjual pcs"
        binding.tvTotalRetur.text = "Retur: $totalRetur pcs"
        binding.tvTotalOmset.text = "Total Omset: ${formatter.format(totalOmset).replace(",00", "")}"

        binding.cardRingkasan.visibility = View.VISIBLE
        binding.bottomBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE
    }

    private fun showNotaList() {
        // Sort: by mitra name, then product name
        val sorted = currentPenjualanList.sortedWith(
            compareBy<Penjualan> { it.mitraNama }.thenBy { it.namaProduk }
        )
        binding.tvDetailHeader.visibility = View.VISIBLE
        binding.rvNotaPenjualan.visibility = View.VISIBLE
        binding.cardSearchLaporan.visibility = View.VISIBLE
        applySearch("") // Load all initially
    }

    // ── Edit Dialog ──────────────────────────────────────────────

    private fun showEditDialog(penjualan: Penjualan) {
        val context = this
        val padding = (16 * resources.displayMetrics.density).toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        // Info label
        val tvInfo = TextView(context).apply {
            text = "📦 ${penjualan.namaProduk}\n📍 ${penjualan.mitraNama}\n💰 Harga: Rp${penjualan.hargaSatuan}/pcs"
            textSize = 14f
            setTextColor(resources.getColor(android.R.color.black, null))
        }
        container.addView(tvInfo)

        // Spacer
        container.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (12 * resources.displayMetrics.density).toInt()
            )
        })

        // Kirim input
        val tvKirimLabel = TextView(context).apply {
            text = "Jumlah Kirim"
            textSize = 12f
            setTextColor(resources.getColor(android.R.color.black, null))
        }
        container.addView(tvKirimLabel)

        val etKirim = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(penjualan.jumlahKirim.toString())
            textSize = 16f
            setBackgroundResource(android.R.drawable.edit_text)
        }
        container.addView(etKirim)

        // Spacer
        container.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (8 * resources.displayMetrics.density).toInt()
            )
        })

        // Retur input
        val tvReturLabel = TextView(context).apply {
            text = "Jumlah Retur"
            textSize = 12f
            setTextColor(resources.getColor(android.R.color.black, null))
        }
        container.addView(tvReturLabel)

        val etRetur = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(penjualan.jumlahRetur.toString())
            textSize = 16f
            setBackgroundResource(android.R.drawable.edit_text)
        }
        container.addView(etRetur)

        AlertDialog.Builder(context)
            .setTitle("✏️ Edit Nota")
            .setView(container)
            .setPositiveButton("Simpan") { _, _ ->
                val newKirim = etKirim.text.toString().toIntOrNull() ?: 0
                val newRetur = etRetur.text.toString().toIntOrNull() ?: 0

                if (newKirim < 0 || newRetur < 0) {
                    showToast("Jumlah tidak boleh negatif")
                    return@setPositiveButton
                }
                if (newRetur > newKirim) {
                    showToast("Retur tidak boleh melebihi kirim")
                    return@setPositiveButton
                }

                performUpdate(penjualan.id, newKirim, newRetur, penjualan.hargaSatuan)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun performUpdate(id: String, kirim: Int, retur: Int, hargaSatuan: Int) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = app.penjualanRepository.updatePenjualanFields(id, kirim, retur, hargaSatuan)

            when (result) {
                is Result.Success -> {
                    showToast("✅ ${result.data}")
                    loadData() // Refresh
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    showToast("❌ ${result.message}")
                }
                is Result.Loading -> { /* no-op */ }
            }
        }
    }

    // ── Delete Confirmation ──────────────────────────────────────

    private fun showDeleteConfirmation(penjualan: Penjualan) {
        AlertDialog.Builder(this)
            .setTitle("🗑️ Hapus Nota")
            .setMessage("Yakin ingin menghapus data penjualan?\n\n" +
                "📦 ${penjualan.namaProduk}\n" +
                "📍 ${penjualan.mitraNama}\n" +
                "Kirim: ${penjualan.jumlahKirim} | Retur: ${penjualan.jumlahRetur}")
            .setPositiveButton("Hapus") { _, _ -> performDelete(penjualan.id) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun performDelete(id: String) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = app.penjualanRepository.deletePenjualanById(id)

            when (result) {
                is Result.Success -> {
                    showToast("✅ ${result.data}")
                    loadData() // Refresh
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    showToast("❌ ${result.message}")
                }
                is Result.Loading -> { /* no-op */ }
            }
        }
    }

    // ── Export ────────────────────────────────────────────────────

    private fun exportToExcel(shareAfter: Boolean = false) {
        if (currentPenjualanList.isEmpty()) {
            showToast("Tidak ada data untuk di-export")
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = ExcelExportUtil.generateLaporan(
                    context = this@LaporanActivity,
                    penjualanList = currentPenjualanList,
                    periodLabel = getPeriodLabel(),
                    stokRuteList = currentStokRuteList
                )

                lastExportedFile = result.file
                binding.progressBar.visibility = View.GONE

                if (shareAfter) {
                    ExcelExportUtil.shareFile(this@LaporanActivity, result.file)
                } else {
                    androidx.appcompat.app.AlertDialog.Builder(this@LaporanActivity)
                        .setTitle("✅ Laporan Berhasil Diunduh")
                        .setMessage("File Excel telah tersimpan di folder:\nDownload / VanishaBakery\n\nCatatan Penting:\nJika file tidak bisa dibuka menggunakan aplikasi Microsoft Excel di HP, hal ini wajar karena keamanan aplikasi Excel versi HP. Silakan gunakan WPS Office, Google Sheets, atau klik 'Bagikan' untuk mengirimnya ke WhatsApp/Laptop Anda.")
                        .setPositiveButton("Tutup", null)
                        .setNeutralButton("Bagikan") { _, _ ->
                            ExcelExportUtil.shareFile(this@LaporanActivity, result.file)
                        }
                        .show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                showToast("Gagal export: ${e.message}")
            }
        }
    }
}
