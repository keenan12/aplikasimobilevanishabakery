package id.mohamadsuhendy.vanishabakery.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import id.mohamadsuhendy.vanishabakery.R
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import id.mohamadsuhendy.vanishabakery.databinding.FragmentHomeBinding
import id.mohamadsuhendy.vanishabakery.utils.*
import id.mohamadsuhendy.vanishabakery.ui.notifikasi.NotifikasiBottomSheet
import id.mohamadsuhendy.vanishabakery.ui.admin.ManageStokRuteActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng

class HomeFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        val app = requireActivity().application as VanishaBakeryApp
        HomeViewModel.Factory(
            app.authRepository, app.mitraRepository,
            app.pengirimanRepository, app.penjualanRepository,
            app.firebaseDataSource
        )
    }

    private var savedMapState: Bundle? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.mapPreview.onCreate(savedMapState)
        binding.mapPreview.getMapAsync(this)
        setupChart()
        setupPieChart()
        setupClickListeners()
        observeViewModel()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val jakarta = LatLng(-6.2088, 106.8456)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(jakarta, 11f))
        googleMap.uiSettings.setAllGesturesEnabled(false)
        googleMap.uiSettings.isMapToolbarEnabled = false
    }

    private fun setupChart() {
        val colorOnSurface = com.google.android.material.color.MaterialColors
            .getColor(binding.lineChartSales, com.google.android.material.R.attr.colorOnSurface)

        binding.lineChartSales.apply {
            description.isEnabled = false
            legend.textColor = colorOnSurface
            xAxis.apply {
                textColor = colorOnSurface
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
            }
            axisLeft.apply {
                textColor = colorOnSurface
                setDrawGridLines(true)
                gridColor = com.google.android.material.color.MaterialColors
                    .getColor(binding.lineChartSales, com.google.android.material.R.attr.colorOutline)
            }
            axisRight.isEnabled = false
            setNoDataTextColor(colorOnSurface)
            setNoDataText("Belum ada data penjualan di periode ini")
        }

        binding.pieChartTopProduk.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(11f)
            setCenterTextSize(14f)
            setCenterTextColor(colorOnSurface)
            setHoleColor(Color.TRANSPARENT)
            setNoDataText("Belum ada data produk di periode ini")
            setNoDataTextColor(Color.GRAY)
            legend.apply {
                isEnabled = true
                textColor = colorOnSurface
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }
            
            // Re-animate on click
            setOnClickListener {
                animateXY(1000, 1000)
            }
        }
        
        binding.pieChartTopProduk.setOnClickListener {
            binding.pieChartTopProduk.animateY(1200, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
        }

        binding.btnFilterPeriode.setOnClickListener { showMonthYearPicker() }
        
        // Initial text
        val currentMonthIndex = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val months = arrayOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
        binding.tvSelectedPeriode.text = "${months[currentMonthIndex]} $currentYear"
        
        // Update hidden spinners for compatibility
        binding.spinnerBulan.setText(months[currentMonthIndex], false)
        binding.spinnerTahun.setText(currentYear.toString(), false)
    }

    private fun showMonthYearPicker() {
        val months = arrayOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
        val years = arrayOf("2024", "2025", "2026")
        
        val currentMonth = binding.spinnerBulan.text.toString()
        val currentYear = binding.spinnerTahun.text.toString()
        
        var selectedMonthIndex = months.indexOf(currentMonth).coerceAtLeast(0)
        var selectedYear = currentYear.toIntOrNull() ?: 2024

        val dialogView = layoutInflater.inflate(R.layout.dialog_month_year_picker, null)
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.Theme_VanishaBakery_Dialog_Neo)
            .setView(dialogView)
            .create()

        val npMonth = dialogView.findViewById<android.widget.NumberPicker>(R.id.npMonth).apply {
            minValue = 0
            maxValue = 11
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
            binding.tvSelectedPeriode.text = selectedText
            
            // Keep hidden spinners in sync
            binding.spinnerBulan.setText(months[selectedMonthIndex], false)
            binding.spinnerTahun.setText(selectedYear.toString(), false)
            
            viewModel.setFilterMonth(selectedMonthIndex)
            viewModel.setFilterYear(selectedYear)
            
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupPieChart() {
        // Implementation logic handled in setupChart and updatePieChart
    }

    private val cameraLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraImageUri?.let { readAndUpload(it) }
    }

    private val galleryLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { 
            try {
                requireContext().contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { /* ignore if not support */ }
            readAndUpload(it) 
        }
    }

    private var cameraImageUri: android.net.Uri? = null

    private fun readAndUpload(uri: android.net.Uri) {
        try {
            binding.pbProfilePhoto.visibility = android.view.View.VISIBLE
            binding.ivProfilePhoto.alpha = 0.5f
            
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: throw Exception("Gagal membuka file")
            val bytes = inputStream.readBytes()
            inputStream.close()
            
            val originalBitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (originalBitmap == null) throw Exception("Format gambar tidak valid")
            
            val maxDim = 400f
            val scale = kotlin.math.min(maxDim / originalBitmap.width, maxDim / originalBitmap.height)
            val finalBitmap = if (scale < 1f) {
                android.graphics.Bitmap.createScaledBitmap(originalBitmap, (originalBitmap.width * scale).toInt(), (originalBitmap.height * scale).toInt(), true)
            } else originalBitmap

            val baos = java.io.ByteArrayOutputStream()
            finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, baos)
            viewModel.uploadProfilePhoto(baos.toByteArray())
        } catch (e: Exception) {
            binding.pbProfilePhoto.visibility = android.view.View.GONE
            binding.ivProfilePhoto.alpha = 1.0f
            requireContext().showToast("Kesalahan: ${e.localizedMessage}")
        }
    }

    private fun setupClickListeners() {
        binding.cardProfileHeader.setOnClickListener {
            val navController = androidx.navigation.Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
            navController.navigate(R.id.profileFragment)
        }

        binding.ivEditProfile.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Ganti Foto Profil")
                .setItems(arrayOf("Ambil Foto Kamera", "Pilih dari Galeri")) { _, which ->
                    if (which == 0) launchCamera() else galleryLauncher.launch(arrayOf("image/*"))
                }.show()
        }

        binding.btnOpenMaps.setOnClickListener {
            val navController = androidx.navigation.Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
            navController.navigate(R.id.mapsActivity)
        }

        binding.btnNotifikasi.setOnClickListener {
            NotifikasiBottomSheet().show(childFragmentManager, NotifikasiBottomSheet.TAG)
        }

        binding.btnManageStokRute.setOnClickListener {
            val intent = android.content.Intent(requireContext(), ManageStokRuteActivity::class.java)
            startActivity(intent)
        }
    }

    private fun launchCamera() {
        try {
            val photoFile = java.io.File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            cameraImageUri = androidx.core.content.FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", photoFile)
            cameraLauncher.launch(cameraImageUri)
        } catch (e: Exception) {
            requireContext().showToast("Akses kamera gagal")
        }
    }

    private fun observeViewModel() {
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            binding.tvGreeting.text = "Halo, ${user?.nama ?: "Admin"}!"
            user?.fotoUrl?.let { binding.ivProfilePhoto.loadImageCircle(it) }
            
            if (user?.isAdmin() == true) {
                binding.layoutAdminActions.visibility = View.VISIBLE
            } else {
                binding.layoutAdminActions.visibility = View.GONE
            }
        }

        viewModel.visitedMitraCount.observe(viewLifecycleOwner) { count ->
            binding.tvTotalCheckin.text = count.toString()
        }

        viewModel.unvisitedMitraCount.observe(viewLifecycleOwner) { count ->
            binding.tvTotalUnvisited.text = count.toString()
        }

        viewModel.unreadNotifCount.observe(viewLifecycleOwner) { count ->
            if (count > 0) {
                binding.tvNotifBadge.visible()
                binding.tvNotifBadge.text = if (count > 9) "9+" else count.toString()
            } else {
                binding.tvNotifBadge.gone()
            }
        }

        viewModel.chartData.observe(viewLifecycleOwner) { dataList ->
            if (dataList.isNotEmpty()) {
                val entries = dataList.mapIndexed { index, pair ->
                    Entry(index.toFloat(), pair.second)
                }
                updateLineChart(entries)
                
                // Feedback for user
                val hasData = dataList.any { it.second > 0 }
                if (!hasData) {
                    requireContext().showToast("Data ditemukan, tapi semua bernilai Rp 0")
                }
            }
        }

        viewModel.topProduk.observe(viewLifecycleOwner) { products ->
            if (products.isNotEmpty()) {
                updatePieChart(products)
            } else {
                binding.pieChartTopProduk.clear()
            }
        }

        viewModel.omsetBulanIni.observe(viewLifecycleOwner) { omset ->
            val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
            binding.tvTotalOmset.text = formatter.format(omset).replace(",00", "")
        }

        viewModel.photoUploadState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is id.mohamadsuhendy.vanishabakery.utils.Result.Loading -> {
                    binding.pbProfilePhoto.visibility = android.view.View.VISIBLE
                    binding.ivProfilePhoto.alpha = 0.5f
                }
                is id.mohamadsuhendy.vanishabakery.utils.Result.Success -> {
                    binding.pbProfilePhoto.visibility = android.view.View.GONE
                    binding.ivProfilePhoto.alpha = 1.0f
                    requireContext().showToast("Berhasil memperbarui foto profil")
                }
                is id.mohamadsuhendy.vanishabakery.utils.Result.Error -> {
                    binding.pbProfilePhoto.visibility = android.view.View.GONE
                    binding.ivProfilePhoto.alpha = 1.0f
                    requireContext().showToast(result.message)
                }
            }
        }

        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user ?: return@observe
            val app = requireActivity().application as VanishaBakeryApp
            viewLifecycleOwner.lifecycleScope.launch {
                if (user.isAdmin()) {
                    // Admin: badge = jumlah mitra pending
                    app.mitraRepository.observeAllMitra().collect { list ->
                        val pendingCount = list.count { it.status == "pending" }
                        if (pendingCount > 0) {
                            binding.tvNotifBadge.text = if (pendingCount > 9) "9+" else pendingCount.toString()
                            binding.tvNotifBadge.visible()
                        } else {
                            binding.tvNotifBadge.gone()
                        }
                    }
                } else {
                    // Staff: badge = jumlah notifikasi yang belum dibaca (dari notifikasi collection)
                    app.firebaseDataSource.observeNotifikasiByUser(user.uid, false).collect { result ->
                        if (result is id.mohamadsuhendy.vanishabakery.utils.Result.Success) {
                            val unreadCount = result.data.count { !it.isRead }
                            if (unreadCount > 0) {
                                binding.tvNotifBadge.text = if (unreadCount > 9) "9+" else unreadCount.toString()
                                binding.tvNotifBadge.visible()
                            } else {
                                binding.tvNotifBadge.gone()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateLineChart(entries: List<Entry>) {
        val colorOnSurface = com.google.android.material.color.MaterialColors
            .getColor(binding.lineChartSales, com.google.android.material.R.attr.colorOnSurface)
            
        val dataSet = LineDataSet(entries, "Penjualan (IDR)").apply {
            color = Color.BLACK
            setCircleColor(Color.BLACK)
            lineWidth = 3f
            circleRadius = 4f
            setDrawCircleHole(true)
            circleHoleColor = Color.WHITE
            valueTextColor = colorOnSurface
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.BLACK
            fillAlpha = 10
        }

        binding.lineChartSales.apply {
            data = LineData(dataSet)
            xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(
                viewModel.chartData.value?.map { it.first } ?: emptyList()
            )
            animateXY(1000, 1000)
            invalidate()
        }
    }

    private fun updatePieChart(products: List<Pair<String, Int>>) {
        val entries = products.map { PieEntry(it.second.toFloat(), it.first) }
        
        val colors = listOf(
            Color.parseColor("#00F5FF"), // Neo Teal
            Color.parseColor("#FFFF00"), // Neo Yellow
            Color.parseColor("#FF00FF"), // Neo Magenta
            Color.parseColor("#00FF00"), // Neo Green
            Color.parseColor("#FF5722")  // Neo Orange
        )

        val dataSet = PieDataSet(entries, "").apply {
            setColors(colors)
            valueTextColor = Color.BLACK
            valueTextSize = 14f
            valueFormatter = PercentFormatter(binding.pieChartTopProduk)
            sliceSpace = 4f
            selectionShift = 8f
        }

        binding.pieChartTopProduk.apply {
            data = PieData(dataSet)
            setUsePercentValues(true)
            description.isEnabled = false
            legend.isEnabled = true
            legend.textColor = Color.BLACK
            
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(10f)
            
            centerText = "Produk\nTerlaris"
            setCenterTextTypeface(android.graphics.Typeface.DEFAULT_BOLD)
            setCenterTextColor(Color.BLACK)
            
            animateY(1200)
            invalidate()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapPreview.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapPreview.onPause()
    }

    override fun onDestroyView() {
        savedMapState = Bundle()
        binding.mapPreview.onSaveInstanceState(savedMapState!!)
        binding.mapPreview.onDestroy()
        super.onDestroyView()
        _binding = null
    }
}
