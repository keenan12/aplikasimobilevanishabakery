package id.mohamadsuhendy.vanishabakery.ui.mitra

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.asLiveData
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import id.mohamadsuhendy.vanishabakery.R
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import id.mohamadsuhendy.vanishabakery.data.model.Mitra
import id.mohamadsuhendy.vanishabakery.databinding.ActivityMitraDetailBinding
import id.mohamadsuhendy.vanishabakery.utils.Constants
import id.mohamadsuhendy.vanishabakery.utils.Result
import id.mohamadsuhendy.vanishabakery.utils.gone
import id.mohamadsuhendy.vanishabakery.utils.loadImage
import id.mohamadsuhendy.vanishabakery.utils.showSnackbarError
import id.mohamadsuhendy.vanishabakery.utils.showToast
import id.mohamadsuhendy.vanishabakery.utils.visible

class MitraDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMitraDetailBinding
    private val viewModel: MitraViewModel by viewModels {
        val app = application as VanishaBakeryApp
        MitraViewModel.Factory(app.mitraRepository, app.authRepository, app.ruteRepository)
    }

    private var mitra: Mitra? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMitraDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mitra = intent.getParcelableExtra(Constants.KEY_MITRA)
        val mitraId = intent.getStringExtra(Constants.KEY_MITRA_ID)

        if (mitra != null) {
            setupUI(mitra!!)
        } else if (mitraId != null) {
            viewModel.loadMitraDetail(mitraId)
        } else {
            finish()
        }

        setupToolbar()
        setupObservers()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupUI(mitra: Mitra) {
        binding.tvNamaToko.text = mitra.namaToko
        binding.tvStaff.text = "Ditambahkan oleh: ${mitra.staffNama}"
        binding.ivMitra.loadImage(mitra.fotoUrl)

        // Status
        val (statusText, bgRes, textRes) = when (mitra.status) {
            Constants.STATUS_APPROVED -> Triple("Disetujui", R.color.status_approved_bg, R.color.status_approved)
            Constants.STATUS_REJECTED -> Triple("Ditolak", R.color.status_rejected_bg, R.color.status_rejected)
            Constants.STATUS_DELETED -> Triple("Dihapus / Nonaktif", R.color.status_rejected_bg, R.color.status_rejected)
            else -> Triple("Menunggu Persetujuan", R.color.status_pending_bg, R.color.status_pending)
        }
        binding.tvStatus.text = statusText
        binding.tvStatus.backgroundTintList = ContextCompat.getColorStateList(this, bgRes)
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, textRes))

        // Tombol Lihat di Peta - Membuka MapsActivity internal agar lebih profesional
        binding.btnLihatMaps.setOnClickListener {
            val lat = mitra.latitude
            val lng = mitra.longitude
            if (lat == 0.0 && lng == 0.0) {
                showToast("Koordinat lokasi tidak tersedia untuk mitra ini")
                return@setOnClickListener
            }
            val intent = Intent(this, id.mohamadsuhendy.vanishabakery.ui.maps.MapsActivity::class.java).apply {
                putExtra(Constants.KEY_MITRA, mitra)
            }
            startActivity(intent)
        }

        // Admin actions — only show for pending mitra
        setupAdminActions(mitra)
    }

    private fun setupAdminActions(mitra: Mitra) {
        val app = application as VanishaBakeryApp
        app.authRepository.observeCurrentUser().asLiveData().observe(this) { user ->
            val isAdmin = user?.isAdmin() == true
            
            if (isAdmin) {
                // Delete button is available to admin regardless of status
                binding.btnDelete.visible()
                binding.btnDelete.setOnClickListener {
                    val options = arrayOf("Arsipkan (Tetap muncul di Peta)", "Hapus Permanen")
                    AlertDialog.Builder(this)
                        .setTitle("Hapus / Arsipkan Mitra")
                        .setItems(options) { _, which ->
                            if (which == 0) {
                                // Archive
                                AlertDialog.Builder(this)
                                    .setTitle("Arsipkan Mitra")
                                    .setMessage("Toko akan dinonaktifkan namun tetap muncul sebagai riwayat di Peta. Lanjutkan?")
                                    .setPositiveButton("Arsipkan") { _, _ -> viewModel.archiveMitra(mitra.id) }
                                    .setNegativeButton("Batal", null)
                                    .show()
                            } else {
                                // Permanent Delete
                                AlertDialog.Builder(this)
                                    .setTitle("Hapus Permanen")
                                    .setMessage("Semua data toko ${mitra.namaToko} akan dihapus selamanya. Tindakan ini tidak bisa dibatalkan!")
                                    .setPositiveButton("HAPUS") { _, _ -> viewModel.deleteMitra(mitra.id) }
                                    .setNegativeButton("Batal", null)
                                    .show()
                            }
                        }
                        .show()
                }

                // Approve/Reject only for pending
                if (mitra.isPending()) {
                    binding.layoutAdminActions.visible()
                    binding.btnApprove.setOnClickListener {
                        AlertDialog.Builder(this)
                            .setTitle("Konfirmasi")
                            .setMessage("Setujui pendaftaran mitra ${mitra.namaToko}?")
                            .setPositiveButton("Setujui") { _, _ -> viewModel.approveMitra(mitra.id) }
                            .setNegativeButton("Batal", null)
                            .show()
                    }
                    binding.btnReject.setOnClickListener {
                        AlertDialog.Builder(this)
                            .setTitle("Konfirmasi")
                            .setMessage("Tolak pendaftaran mitra ${mitra.namaToko}?")
                            .setPositiveButton("Tolak") { _, _ -> viewModel.rejectMitra(mitra.id) }
                            .setNegativeButton("Batal", null)
                            .show()
                    }
                } else {
                    binding.layoutAdminActions.gone()
                }
            } else {
                binding.layoutAdminActions.gone()
                binding.btnDelete.gone()
            }
        }
    }

    private fun setupObservers() {
        viewModel.mitraDetail.observe(this) { detail ->
            if (detail != null) {
                mitra = detail
                setupUI(detail)
            } else {
                // If we were loading by ID and it's null, it might be an error
                if (intent.getStringExtra(Constants.KEY_MITRA_ID) != null) {
                    showToast("Data mitra tidak ditemukan")
                    finish()
                }
            }
        }

        viewModel.updateState.observe(this) { result ->
            when (result) {
                is Result.Loading -> binding.progressBar.visible()
                is Result.Success -> {
                    binding.progressBar.gone()
                    showToast("Status mitra berhasil diperbarui")
                    // Small delay to ensure smooth transition back to main screen
                    binding.root.postDelayed({
                        finish()
                    }, 500)
                }
                is Result.Error -> {
                    binding.progressBar.gone()
                    binding.root.showSnackbarError(result.message)
                }
            }
        }
    }
}
