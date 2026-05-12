package id.mohamadsuhendy.vanishabakery.ui.aktivitas

import id.mohamadsuhendy.vanishabakery.R

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import android.content.Intent
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import id.mohamadsuhendy.vanishabakery.databinding.FragmentAktivitasBinding
import id.mohamadsuhendy.vanishabakery.ui.mitra.MitraDetailActivity
import id.mohamadsuhendy.vanishabakery.utils.Constants
import id.mohamadsuhendy.vanishabakery.utils.Result
import id.mohamadsuhendy.vanishabakery.utils.gone
import id.mohamadsuhendy.vanishabakery.utils.showToast
import id.mohamadsuhendy.vanishabakery.utils.visible
import java.util.Calendar
import androidx.lifecycle.lifecycleScope

class AktivitasFragment : Fragment() {

    private var _binding: FragmentAktivitasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AktivitasViewModel by viewModels {
        val app = requireActivity().application as VanishaBakeryApp
        AktivitasViewModel.Factory(app.firebaseDataSource, app.authRepository)
    }

    private lateinit var adapter: AktivitasAdapter
    private val monthNames = listOf(
        "Semua", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAktivitasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AktivitasAdapter { log ->
            if (log.tipe == Constants.LOG_TYPE_MITRA && log.referensiId.isNotEmpty()) {
                val intent = Intent(requireContext(), MitraDetailActivity::class.java).apply {
                    putExtra(Constants.KEY_MITRA_ID, log.referensiId)
                }
                startActivity(intent)
            }
        }
        binding.rvAktivitas.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshLogs()
        }

        setupFilters()
        setupSalesFilter()

        binding.btnDeleteAll.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Hapus Riwayat")
                .setMessage("Yakin ingin menghapus semua riwayat yang ditampilkan sekarang?")
                .setPositiveButton("Hapus") { _, _ -> viewModel.deleteLogsForCurrentFilter() }
                .setNegativeButton("Batal", null)
                .show()
        }

        viewModel.logList.observe(viewLifecycleOwner) { logs ->
            adapter.submitList(logs)
            binding.progressBar.gone()
            binding.swipeRefresh.isRefreshing = false
            binding.tvTotalKegiatan.text = "${logs.size} Kegiatan"
            if (logs.isEmpty()) binding.layoutEmpty.visible() else binding.layoutEmpty.gone()
            // Show delete button only when there are logs
            if (logs.isEmpty()) binding.btnDeleteAll.gone() else binding.btnDeleteAll.visible()
        }

        viewModel.deleteState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> requireContext().showToast("Riwayat berhasil dihapus")
                is Result.Error -> requireContext().showToast("Gagal menghapus: ${result.message}")
                else -> {}
            }
        }
    }

    private fun setupFilters() {
        binding.btnFilterPeriodeHistory.setOnClickListener { showMonthYearPicker() }
        
        // Initial text
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        binding.tvSelectedPeriodeHistory.text = "Semua $currentYear"
        
        setupSalesFilter()
    }

    private fun showMonthYearPicker() {
        val months = monthNames.toTypedArray()
        val years = arrayOf("2024", "2025", "2026")
        
        val currentText = binding.tvSelectedPeriodeHistory.text.toString()
        val currentMonthPart = currentText.substringBeforeLast(" ")
        val currentYearPart = currentText.substringAfterLast(" ")
        
        var selectedMonthIndex = months.indexOf(currentMonthPart).coerceAtLeast(0)
        var selectedYear = currentYearPart.toIntOrNull() ?: 2024

        val dialogView = layoutInflater.inflate(R.layout.dialog_month_year_picker, null)
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.Theme_VanishaBakery_Dialog_Neo)
            .setView(dialogView)
            .create()

        val npMonth = dialogView.findViewById<android.widget.NumberPicker>(R.id.npMonth).apply {
            minValue = 0
            maxValue = months.size - 1
            displayedValues = months
            value = selectedMonthIndex
        }
        val npYear = dialogView.findViewById<android.widget.NumberPicker>(R.id.npYear).apply {
            minValue = 0
            maxValue = years.size - 1
            displayedValues = years
            value = years.indexOf(selectedYear.toString()).coerceAtLeast(0)
        }

        dialogView.findViewById<android.widget.Button>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<android.widget.Button>(R.id.btnConfirm).setOnClickListener {
            selectedMonthIndex = npMonth.value
            selectedYear = years[npYear.value].toInt()
            
            val selectedText = "${months[selectedMonthIndex]} $selectedYear"
            binding.tvSelectedPeriodeHistory.text = selectedText
            
            viewModel.filterByMonth(selectedMonthIndex, selectedYear)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupSalesFilter() {
        viewLifecycleOwner.lifecycleScope.launch {
            val app = requireActivity().application as VanishaBakeryApp
            val currentUser = app.authRepository.getCurrentUserData()
            
            if (currentUser?.isAdmin() == true) {
                binding.cardSalesFilter.visibility = android.view.View.VISIBLE
                // For Admin, show the filter
                app.authRepository.observeAllUsers().onEach { users ->
                    val salesList = users.filter { it.role == Constants.ROLE_STAFF }
                    val salesNames = mutableListOf("Semua Sales")
                    salesNames.addAll(salesList.map { it.nama })
                    
                    val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, salesNames)
                    binding.spinnerSalesFilter.setAdapter(adapter)
                    
                    // Set default text so it's not empty
                    if (binding.spinnerSalesFilter.text.isNullOrEmpty()) {
                        binding.spinnerSalesFilter.setText("Semua Sales", false)
                    }
                    
                    binding.spinnerSalesFilter.setOnItemClickListener { _, _, position, _ ->
                        val salesId = if (position == 0) "" else salesList[position - 1].uid
                        viewModel.filterBySales(salesId)
                    }
                }.launchIn(viewLifecycleOwner.lifecycleScope)
            } else {
                binding.cardSalesFilter.visibility = android.view.View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
