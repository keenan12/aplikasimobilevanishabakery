package id.mohamadsuhendy.vanishabakery.ui.mitra

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.common.api.ResolvableApiException
import androidx.activity.result.IntentSenderRequest
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import id.mohamadsuhendy.vanishabakery.data.model.Rute
import id.mohamadsuhendy.vanishabakery.databinding.BottomSheetAddMitraBinding
import id.mohamadsuhendy.vanishabakery.utils.Result
import id.mohamadsuhendy.vanishabakery.utils.gone
import id.mohamadsuhendy.vanishabakery.utils.loadImage
import id.mohamadsuhendy.vanishabakery.utils.showToast
import id.mohamadsuhendy.vanishabakery.utils.visible
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddMitraBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddMitraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MitraViewModel by viewModels {
        val app = requireActivity().application as VanishaBakeryApp
        MitraViewModel.Factory(app.mitraRepository, app.authRepository, app.ruteRepository)
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var cancellationSource = CancellationTokenSource()
    private var photoUri: Uri? = null
    private var photoFile: File? = null
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private var currentAlamat: String = ""
    private var isLocationCaptured = false
    private var isPhotoCaptured = false
    private var ruteOptions: List<Rute> = emptyList()

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            photoUri?.let {
                isPhotoCaptured = true
                binding.ivFotoPreview.visible()
                binding.ivFotoPreview.loadImage(it.toString())
                binding.tvAmbilFoto.text = "Foto berhasil diambil ✓"
            }
        }
    }

    // Permission launchers
    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else requireContext().showToast("Izin kamera diperlukan")
    }
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) checkLocationSettingsAndGetLocation()
        else requireContext().showToast("Izin lokasi diperlukan")
    }

    private val resolutionForResult = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            getCurrentLocation() // GPS diaktifkan
        } else {
            binding.tvLokasiStatus.text = "GPS tidak diaktifkan. Gagal mengambil lokasi."
            binding.btnAmbilLokasi.isEnabled = true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAddMitraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        binding.btnAmbilFoto.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) launchCamera()
            else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.btnAmbilLokasi.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) checkLocationSettingsAndGetLocation()
            else locationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }

        binding.btnSimpan.setOnClickListener { saveMitra() }
        binding.btnBatal.setOnClickListener { dismiss() }

        // Trigger dropdown on click for non-editable fields
        binding.acRute.setOnClickListener { binding.acRute.showDropDown() }

        observeViewModel()
    }

    private fun launchCamera() {
        photoFile = createImageFile()
        photoUri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.provider", photoFile!!
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }
        cameraLauncher.launch(intent)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("MITRA_${timeStamp}_", ".jpg", storageDir)
    }

    private fun checkLocationSettingsAndGetLocation() {
        binding.tvLokasiStatus.text = "Memeriksa status GPS..."
        binding.btnAmbilLokasi.isEnabled = false

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(requireActivity())

        client.checkLocationSettings(builder.build())
            .addOnSuccessListener {
                getCurrentLocation()
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                        resolutionForResult.launch(intentSenderRequest)
                    } catch (sendEx: Exception) {
                        binding.tvLokasiStatus.text = "Gagal memunculkan prompt GPS."
                        binding.btnAmbilLokasi.isEnabled = true
                    }
                } else {
                    binding.tvLokasiStatus.text = "GPS tidak tersedia."
                    binding.btnAmbilLokasi.isEnabled = true
                }
            }
    }

    private fun getCurrentLocation() {
        binding.tvLokasiStatus.text = "📍 Mengambil lokasi GPS..."
        binding.btnAmbilLokasi.isEnabled = false

        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            binding.btnAmbilLokasi.isEnabled = true
            return
        }

        // Cancel any pending request
        cancellationSource.cancel()
        cancellationSource = CancellationTokenSource()

        // Use getCurrentLocation() — actively requests a fresh GPS fix
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationSource.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                currentLatitude = location.latitude
                currentLongitude = location.longitude
                currentAlamat = "${String.format("%.6f", currentLatitude)}, ${String.format("%.6f", currentLongitude)}"
                isLocationCaptured = true
                binding.tvLokasiStatus.text = "✓ Lokasi berhasil: $currentAlamat"
                binding.btnAmbilLokasi.isEnabled = true
            } else {
                binding.tvLokasiStatus.text = "Lokasi tidak ditemukan. Aktifkan GPS lalu coba lagi."
                binding.btnAmbilLokasi.isEnabled = true
            }
        }.addOnFailureListener { e ->
            binding.tvLokasiStatus.text = "Gagal: ${e.message ?: "Error tidak diketahui"}"
            binding.btnAmbilLokasi.isEnabled = true
        }
    }

    private var selectedRute: id.mohamadsuhendy.vanishabakery.data.model.Rute? = null

    private fun saveMitra() {
        val namaToko = binding.etNamaToko.text.toString().trim()
        val rute = selectedRute
        
        if (namaToko.isBlank()) { 
            binding.tilNamaToko.error = "Nama toko tidak boleh kosong"
            return 
        }
        if (rute == null) { 
            binding.tilRute.error = "Pilih rute pengiriman"
            return 
        }
        if (!isPhotoCaptured || photoFile == null) { 
            requireContext().showToast("Silakan ambil foto toko terlebih dahulu")
            return 
        }
        if (!isLocationCaptured) { 
            requireContext().showToast("Silakan ambil lokasi GPS terlebih dahulu")
            return 
        }

        binding.tilNamaToko.error = null
        
        // Karena Storage terkunci, kita konversi foto ke Base64 (Simpan langsung di Firestore)
        val uri = photoUri ?: run {
            requireContext().showToast("Silakan ambil foto toko terlebih dahulu")
            return
        }
        val base64Photo = uriToBase64(uri)
        if (base64Photo == null) {
            requireContext().showToast("Gagal memproses foto")
            return
        }
        
        // Kirim dengan prefix khusus agar Repository tahu ini tidak perlu diupload ke Storage
        val photoData = "data:image/jpeg;base64,$base64Photo"
        
        viewModel.addMitra(
            namaToko, Uri.parse(photoData), currentLatitude, currentLongitude, currentAlamat,
            rute.id, rute.namaRute
        )
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            // Resize ke ukuran kecil (lebar 400px) agar muat di limit Firestore 1MB
            val width = 400
            val height = (bitmap.height * (width.toDouble() / bitmap.width)).toInt()
            val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true)
            
            val outputStream = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, outputStream)
            val bytes = outputStream.toByteArray()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun observeViewModel() {
        viewModel.ruteList.observe(viewLifecycleOwner) { list ->
            ruteOptions = list
            if (list.isEmpty()) {
                binding.acRute.setText("Belum ada rute ditugaskan", false)
                binding.tilRute.helperText = "Minta Admin untuk menambahkan rute Anda"
                return@observe
            }
            
            binding.tilRute.helperText = null
            val adapter = android.widget.ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                list.map { it.namaRute }
            )
            binding.acRute.setAdapter(adapter)
            binding.acRute.setOnItemClickListener { _, _, position, _ ->
                selectedRute = list[position]
                binding.tilRute.error = null
            }
        }

        viewModel.saveState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visible()
                    binding.btnSimpan.isEnabled = false
                }
                is Result.Success -> {
                    binding.progressBar.gone()
                    requireContext().showToast("Mitra berhasil disimpan! Menunggu persetujuan admin.")
                    dismiss()
                }
                is Result.Error -> {
                    binding.progressBar.gone()
                    binding.btnSimpan.isEnabled = true
                    requireContext().showToast("Gagal menyimpan: ${result.message}")
                }
                else -> {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddMitraBottomSheet"
    }
}
