package id.mohamadsuhendy.vanishabakery.ui.maps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.material.chip.Chip
import id.mohamadsuhendy.vanishabakery.R
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import id.mohamadsuhendy.vanishabakery.data.model.Mitra
import id.mohamadsuhendy.vanishabakery.data.model.Rute
import id.mohamadsuhendy.vanishabakery.databinding.ActivityMapsBinding
import id.mohamadsuhendy.vanishabakery.utils.Constants
import id.mohamadsuhendy.vanishabakery.utils.HeatmapHelper
import id.mohamadsuhendy.vanishabakery.utils.showToast
import id.mohamadsuhendy.vanishabakery.data.model.Penjualan
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapsBinding
    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private var allMitra = listOf<Mitra>()
    private var currentRouteId = ""
    private var currentSalesId = ""
    
    private var selectedMitraLatLng: LatLng? = null
    private var markers = mutableListOf<Marker>()
    private var polyline: Polyline? = null

    // Heatmap data
    private var mitraPerformaMap: Map<String, HeatmapHelper.MitraPerforma> = emptyMap()
    private var isAdmin = false

    private val gpsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            enableMyLocation()
        } else {
            showToast("GPS harus aktif untuk menggunakan fitur ini")
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            checkGpsAndEnableLocation()
        } else {
            showToast("Izin lokasi diperlukan")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.map.onCreate(savedInstanceState)
        binding.map.getMapAsync(this)

        setupListeners()
        loadData()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Setup Map Style & Settings
        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.isMapToolbarEnabled = false
        
        // Default center (Jakarta)
        val jakarta = LatLng(-6.2088, 106.8456)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(jakarta, 11f))

        checkGpsAndEnableLocation()
        renderMapElements()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        
        binding.fabMyLocation.setOnClickListener {
            checkGpsAndEnableLocation()
        }

        binding.fabMapType.setOnClickListener {
            showMapTypeDialog()
        }

        binding.btnNavigasi.setOnClickListener {
            selectedMitraLatLng?.let { latLng ->
                val gmmIntentUri = android.net.Uri.parse("google.navigation:q=${latLng.latitude},${latLng.longitude}&mode=d")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    val browserUri = android.net.Uri.parse("https://maps.google.com/?daddr=${latLng.latitude},${latLng.longitude}&directionsmode=driving")
                    startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                }
            } ?: showToast("Pilih lokasi mitra terlebih dahulu")
        }

        binding.fabCamera.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(packageManager) != null) {
                cameraLauncher.launch(intent)
            } else {
                showToast("Aplikasi kamera tidak ditemukan")
            }
        }
    }

    private fun checkGpsAndEnableLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(this)
        
        client.checkLocationSettings(builder.build())
            .addOnSuccessListener { enableMyLocation() }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution.intentSender).build()
                        gpsLauncher.launch(intentSenderRequest)
                    } catch (e: Exception) { }
                }
            }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap?.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15f))
                }
            }
        }
    }

    private fun loadData() {
        val app = application as VanishaBakeryApp
        lifecycleScope.launch {
            val user = app.authRepository.getCurrentUserData() ?: return@launch
            
            // Log for debugging sales access
            android.util.Log.d("MapsActivity", "Loading data for user: ${user.nama} (Role: ${user.role})")

            if (user.isAdmin()) {
                isAdmin = true
                binding.fabFilterSales.visibility = View.VISIBLE
                binding.fabFilterSales.setOnClickListener { showSalesFilterDialog() }
            } else {
                isAdmin = false
                // SALES ACCESS: Ensure filter sales is hidden
                binding.fabFilterSales.visibility = View.GONE
                currentSalesId = user.uid
            }

            val mitraFlow = if (user.isAdmin()) {
                if (currentSalesId.isEmpty()) app.mitraRepository.observeAllMitra()
                else app.mitraRepository.observeMitraByStaff(currentSalesId)
            } else {
                // Sales only see their own mitras
                app.mitraRepository.observeMitraByStaff(user.uid)
            }

            val ruteFlow = if (user.isAdmin()) {
                if (currentSalesId.isEmpty()) app.ruteRepository.observeAllRute()
                else app.ruteRepository.observeRuteByStaff(currentSalesId)
            } else {
                // Sales only see their own routes
                app.ruteRepository.observeRuteByStaff(user.uid)
            }

            launch { ruteFlow.collect { routes -> setupRouteFilters(routes) } }
            launch { mitraFlow.collect { list -> allMitra = list; renderMapElements() } }

            // Load heatmap data for admin (current month penjualan)
            if (isAdmin) {
                loadHeatmapData()
            }
        }
    }

    private fun loadHeatmapData() {
        val app = application as VanishaBakeryApp
        lifecycleScope.launch {
            try {
                val cal = Calendar.getInstance()
                val startTs = com.google.firebase.Timestamp(Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.time)
                val endTs = com.google.firebase.Timestamp(Calendar.getInstance().apply {
                    add(Calendar.MONTH, 1)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.time)

                val penjualanList = app.firebaseDataSource.observePenjualanByPeriode(startTs, endTs).first()
                mitraPerformaMap = HeatmapHelper.calculatePerforma(penjualanList)
                renderMapElements() // re-render with heatmap colors
            } catch (e: Exception) {
                android.util.Log.e("MapsActivity", "Failed to load heatmap data", e)
            }
        }
    }

    private fun setupRouteFilters(routes: List<Rute>) {
        binding.chipGroupRute.removeAllViews()
        addFilterChip("Semua", "")
        routes.forEach { addFilterChip(it.namaRute, it.id) }
    }

    private fun addFilterChip(label: String, id: String) {
        val chip = Chip(this).apply {
            text = label
            isCheckable = true
            isChecked = currentRouteId == id
            setOnClickListener {
                currentRouteId = id
                renderMapElements()
            }
        }
        binding.chipGroupRute.addView(chip)
    }

    private fun renderMapElements() {
        val map = googleMap ?: return
        map.clear()
        markers.clear()
        polyline?.remove()

        val filteredMitra = allMitra.filter { 
            if (currentRouteId.isEmpty()) true else it.ruteId == currentRouteId 
        }.sortedBy { it.namaToko }

        if (filteredMitra.isEmpty()) return

        val builder = LatLngBounds.Builder()
        val polylineOptions = PolylineOptions().color(Color.parseColor("#00F5FF")).width(8f)

        filteredMitra.forEach { mitra ->
            val position = LatLng(mitra.latitude, mitra.longitude)
            builder.include(position)
            polylineOptions.add(position)

            val icon = getMarkerIcon(mitra)
            val marker = map.addMarker(MarkerOptions()
                .position(position)
                .title(mitra.namaToko)
                .icon(icon)
                .anchor(0.5f, 0.5f))
            
            marker?.tag = mitra
            marker?.let { markers.add(it) }
        }

        if (currentRouteId.isNotEmpty() && filteredMitra.size > 1) {
            polyline = map.addPolyline(polylineOptions)
        }

        map.setOnMarkerClickListener { marker ->
            val mitra = marker.tag as? Mitra ?: return@setOnMarkerClickListener false
            updateUIForMitra(mitra)
            marker.showInfoWindow()
            map.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
            true
        }

        // Smart Zoom
        val focusedMitra = intent.getParcelableExtra<Mitra>(Constants.KEY_MITRA)
        if (focusedMitra != null) {
            val pos = LatLng(focusedMitra.latitude, focusedMitra.longitude)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f))
            updateUIForMitra(focusedMitra)
        } else if (filteredMitra.isNotEmpty()) {
            val bounds = builder.build()
            try {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
            } catch (e: Exception) {
                // Fallback: animate to center of bounds
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(bounds.center, 12f))
            }
        }
    }

    private fun getMarkerIcon(mitra: Mitra): BitmapDescriptor {
        val performa = mitraPerformaMap[mitra.id]

        val backgroundColor = when {
            mitra.isDeleted() -> Color.DKGRAY
            !mitra.isApproved() -> Color.parseColor("#FF00FF")  // Pending = Magenta
            isAdmin && performa != null -> HeatmapHelper.getColor(performa.level)  // Heatmap color
            isAdmin -> HeatmapHelper.getColor(HeatmapHelper.HeatmapLevel.GRAY)     // No data = Gray
            else -> Color.parseColor("#00F5FF")                // Sales view = Teal
        }

        val iconRes = if (mitra.isApproved()) R.drawable.ic_bakery_bag else R.drawable.ic_location
        
        // Classic Pin Shape: Circle on top with a pointed bottom
        val width = 120
        val height = 160
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val path = android.graphics.Path()

        val centerX = width / 2f
        val circleRadius = 50f
        val bottomY = height - 10f

        fun drawPinPath(p: android.graphics.Path, radius: Float, tipY: Float) {
            p.reset()
            p.moveTo(centerX, tipY) // Tip of the pin
            // Angle of the lines connecting to the circle
            val angle = 35.0
            val xOffset = (radius * Math.cos(Math.toRadians(angle))).toFloat()
            val yOffset = (radius * Math.sin(Math.toRadians(angle))).toFloat()
            
            p.lineTo(centerX - xOffset, centerX + yOffset)
            p.arcTo(centerX - radius, centerX - radius, centerX + radius, centerX + radius, 90f + angle.toFloat(), 360f - (2 * angle.toFloat()), false)
            p.close()
        }

        // 1. Shadow
        paint.color = Color.BLACK
        drawPinPath(path, circleRadius, bottomY + 4)
        canvas.drawPath(path, paint)

        // 2. White Border
        paint.color = Color.WHITE
        drawPinPath(path, circleRadius, bottomY)
        canvas.drawPath(path, paint)

        // 3. Colored Center
        paint.color = backgroundColor
        drawPinPath(path, circleRadius - 8, bottomY - 12)
        canvas.drawPath(path, paint)

        // 4. Icon
        val drawable = ContextCompat.getDrawable(this, iconRes)?.mutate()
        drawable?.setTint(if (mitra.isDeleted()) Color.WHITE else Color.BLACK)
        val iconSize = 50
        val iconLeft = (centerX - iconSize / 2).toInt()
        val iconTop = (centerX - iconSize / 2).toInt()
        drawable?.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
        drawable?.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun showMapTypeDialog() {
        val types = arrayOf("Normal", "Satelit", "Hybrid", "Terrain")
        val mapTypeValues = intArrayOf(
            GoogleMap.MAP_TYPE_NORMAL,
            GoogleMap.MAP_TYPE_SATELLITE,
            GoogleMap.MAP_TYPE_HYBRID,
            GoogleMap.MAP_TYPE_TERRAIN
        )

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Pilih Tampilan Peta")
            .setItems(types) { _, which ->
                googleMap?.mapType = mapTypeValues[which]
            }
            .show()
    }

    private fun updateUIForMitra(mitra: Mitra) {
        binding.tvMitraName.text = mitra.namaToko

        val performa = mitraPerformaMap[mitra.id]
        val status = when {
            mitra.isDeleted() -> "NONAKTIF"
            mitra.isApproved() -> "Disetujui ✓"
            else -> "Pending ⏳"
        }

        val heatmapInfo = if (isAdmin && performa != null && performa.totalKirim > 0) {
            val label = HeatmapHelper.getLabel(performa.level)
            "\nPerforma: ${String.format("%.0f", performa.rasioLaku)}% ($label)" +
            "\nKirim: ${performa.totalKirim} | Laku: ${performa.totalTerjual} | Retur: ${performa.totalRetur}"
        } else if (isAdmin) {
            "\nPerforma: Belum ada data"
        } else ""

        binding.tvMitraStatus.text = "Status: $status\nSales: ${mitra.staffNama}\nRute: ${mitra.ruteNama}\n${mitra.alamat}$heatmapInfo"
        selectedMitraLatLng = LatLng(mitra.latitude, mitra.longitude)
        
        // Update distance if location is available
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let {
                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(it.latitude, it.longitude, mitra.latitude, mitra.longitude, results)
                    binding.tvDistance.text = String.format("%.2f Km", results[0] / 1000f)
                }
            }
        }
    }

    private fun showSalesFilterDialog() {
        val app = application as VanishaBakeryApp
        lifecycleScope.launch {
            val salesList = app.authRepository.getAllUsers().filter { !it.isAdmin() }
            val salesNames = salesList.map { it.nama }.toMutableList()
            salesNames.add(0, "Semua Sales")

            androidx.appcompat.app.AlertDialog.Builder(this@MapsActivity)
                .setTitle("Filter Berdasarkan Sales")
                .setItems(salesNames.toTypedArray()) { _, which ->
                    currentSalesId = if (which == 0) "" else salesList[which - 1].uid
                    currentRouteId = "" 
                    loadData()
                }
                .show()
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) showToast("Validasi foto berhasil!")
    }

    // Lifecycle methods for MapView
    override fun onResume() { super.onResume(); binding.map.onResume() }
    override fun onPause() { super.onPause(); binding.map.onPause() }
    override fun onDestroy() { super.onDestroy(); binding.map.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); binding.map.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); binding.map.onSaveInstanceState(outState) }
}
