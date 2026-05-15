package id.mohamadsuhendy.vanishabakery.ui.profile

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import id.mohamadsuhendy.vanishabakery.ui.main.MainActivity
import id.mohamadsuhendy.vanishabakery.BuildConfig
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import id.mohamadsuhendy.vanishabakery.databinding.FragmentProfileBinding
import id.mohamadsuhendy.vanishabakery.utils.Result
import id.mohamadsuhendy.vanishabakery.utils.gone
import id.mohamadsuhendy.vanishabakery.utils.loadImage
import id.mohamadsuhendy.vanishabakery.utils.loadImageCircle
import id.mohamadsuhendy.vanishabakery.utils.showToast
import id.mohamadsuhendy.vanishabakery.utils.showSnackbar
import id.mohamadsuhendy.vanishabakery.utils.visible
import java.io.File

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        val app = requireActivity().application as VanishaBakeryApp
        ProfileViewModel.Factory(app.authRepository, app.firebaseDataSource, app.ruteRepository)
    }

    private var userAdapter: UserAdapter? = null
    private var cameraImageUri: Uri? = null

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraImageUri?.let { readAndUpload(it) }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { readAndUpload(it) }
    }

    private fun readAndUpload(uri: Uri) {
        try {
            val contentResolver = requireContext().contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: throw Exception("Gagal membuka file")
            
            val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (originalBitmap == null) throw Exception("Format gambar tidak valid")

            // Auto-resize for efficiency
            val maxDim = 1024f
            val scale = kotlin.math.min(maxDim / originalBitmap.width, maxDim / originalBitmap.height)
            val finalBitmap = if (scale < 1f) {
                android.graphics.Bitmap.createScaledBitmap(
                    originalBitmap, 
                    (originalBitmap.width * scale).toInt(), 
                    (originalBitmap.height * scale).toInt(), 
                    true
                )
            } else originalBitmap

            val baos = java.io.ByteArrayOutputStream()
            finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos)
            val bytes = baos.toByteArray()

            viewModel.uploadProfilePhoto(bytes)
            
        } catch (e: Exception) {
            requireContext().showToast("Kesalahan: ${e.localizedMessage ?: "Gagal memproses foto"}")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.tvAppVersion.text = "Vanisha Bakery v${BuildConfig.VERSION_NAME}"
        
        binding.btnEditAvatar.setOnClickListener { showPhotoOptions() }
        

        
        binding.cardLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Logout")
                .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
                .setPositiveButton("Logout") { _, _ ->
                    viewModel.logout()
                    (requireActivity() as MainActivity).logout()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        observeViewModel()
    }



    private fun setupUserList() {
        if (userAdapter == null) {
            userAdapter = UserAdapter { user ->
                AlertDialog.Builder(requireContext())
                    .setTitle(user.nama)
                    .setMessage("Role: ${user.role.uppercase()}\nStatus: ${if (user.isActive) "Aktif" else "Nonaktif"}")
                    .setPositiveButton(if (user.isActive) "Nonaktifkan" else "Aktifkan") { _, _ ->
                        viewModel.toggleUserActive(user.uid, !user.isActive)
                    }
                    .setNegativeButton("Tutup", null)
                    .show()
            }
        }
        binding.rvUserList.adapter = userAdapter
    }

    private fun showPhotoOptions() {
        AlertDialog.Builder(requireContext())
            .setTitle("Ganti Foto Profil")
            .setItems(arrayOf("Ambil Foto Kamera", "Pilih dari Galeri")) { _, which ->
                if (which == 0) launchCamera() else galleryLauncher.launch("image/*")
            }.show()
    }

    private fun launchCamera() {
        try {
            val photoFile = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            cameraImageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", photoFile)
            cameraLauncher.launch(cameraImageUri)
        } catch (e: Exception) {
            requireContext().showToast("Akses kamera gagal")
        }
    }

    private fun observeViewModel() {
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.tvNama.text = it.nama
                binding.tvEmail.text = it.email
                binding.tvRole.text = if (it.isAdmin()) "ADMINISTRATOR" else "STAFF SALES"
                binding.ivAvatar.loadImageCircle(it.fotoUrl)

                binding.tvStatusText.text = "Status: Online"
                binding.tvStatusText.setTextColor(android.graphics.Color.parseColor("#34C759"))
                binding.tvLastLogin.text = "Login Real-time Aktif"

                if (it.isAdmin()) {
                    binding.layoutAdminSection.visible()
                    setupUserList()
                    viewModel.observeAllUsers()
                    
                    binding.btnKelolaUser.setOnClickListener { 
                        viewModel.observeAllUsers() 
                        requireView().showSnackbar("Daftar pengguna diperbarui")
                    }
                    binding.btnTambahUser.setOnClickListener { AddUserBottomSheet().show(childFragmentManager, AddUserBottomSheet.TAG) }
                    binding.btnKelolaRute.setOnClickListener { ManageRuteBottomSheet().show(childFragmentManager, ManageRuteBottomSheet.TAG) }
                } else {
                    binding.layoutAdminSection.gone()
                }
            }
        }

        viewModel.photoUploadState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> binding.progressBar.visible()
                is Result.Success -> { binding.progressBar.gone(); requireContext().showToast("Berhasil memperbarui foto profil") }
                is Result.Error -> { binding.progressBar.gone(); requireContext().showToast(result.message) }
            }
        }

        viewModel.sampulUploadState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> binding.progressBar.visible()
                is Result.Success -> { binding.progressBar.gone(); requireContext().showToast("Berhasil memperbarui foto sampul") }
                is Result.Error -> { binding.progressBar.gone(); requireContext().showToast(result.message) }
            }
        }

        viewModel.allUsers.observe(viewLifecycleOwner) { users ->
            binding.btnKelolaUser.text = "REFRESH (${users.size})"
            userAdapter?.submitList(users)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        userAdapter = null
    }
}
