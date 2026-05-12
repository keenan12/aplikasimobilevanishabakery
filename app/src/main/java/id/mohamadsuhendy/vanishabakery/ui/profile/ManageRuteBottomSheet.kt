package id.mohamadsuhendy.vanishabakery.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import id.mohamadsuhendy.vanishabakery.R
import id.mohamadsuhendy.vanishabakery.databinding.BottomSheetManageRuteBinding
import id.mohamadsuhendy.vanishabakery.utils.Result
import id.mohamadsuhendy.vanishabakery.utils.showToast
import id.mohamadsuhendy.vanishabakery.utils.Constants

class ManageRuteBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetManageRuteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        val app = requireActivity().application as VanishaBakeryApp
        ProfileViewModel.Factory(app.authRepository, app.firebaseDataSource, app.ruteRepository)
    }

    private lateinit var adapter: RuteAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetManageRuteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RuteAdapter(
            onEditClick = { rute ->
                showEditRuteDialog(rute)
            },
            onDeleteClick = { rute ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Hapus Rute")
                    .setMessage("Yakin ingin menghapus rute ${rute.namaRute}?")
                    .setPositiveButton("Hapus") { _, _ ->
                        viewModel.deleteRute(rute.id)
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        )
        binding.rvRute.adapter = adapter

        binding.fabAddRute.setOnClickListener {
            showAddRuteDialog()
        }

        observeViewModel()
        if (viewModel.ruteList.value == null) {
            viewModel.observeRute()
        }
        // Ensure users are loaded for staff selection
        viewModel.observeAllUsers()
    }

    private fun observeViewModel() {
        viewModel.ruteList.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }
        
        viewModel.addRuteState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> { } // Maybe show progress
                is Result.Success -> {
                    requireContext().showToast("Rute berhasil ditambahkan!")
                    // We don't reset here because we just show a toast
                }
                is Result.Error -> {
                    requireContext().showToast(result.message)
                }
            }
        }

        viewModel.deleteRuteState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    requireContext().showToast("Rute berhasil dihapus")
                    viewModel.resetDeleteRuteState()
                }
                is Result.Error -> {
                    requireContext().showToast(result.message)
                    viewModel.resetDeleteRuteState()
                }
                else -> {}
            }
        }

        viewModel.updateRuteState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    requireContext().showToast("Rute berhasil diperbarui")
                    viewModel.resetUpdateRuteState()
                }
                is Result.Error -> {
                    requireContext().showToast(result.message)
                    viewModel.resetUpdateRuteState()
                }
                else -> {}
            }
        }
    }

    private fun showAddRuteDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_rute, null)
        val etNamaRute = dialogView.findViewById<EditText>(R.id.etNamaRute)
        val etKodeRute = dialogView.findViewById<EditText>(R.id.etKodeRute)
        val spinnerStaff = dialogView.findViewById<Spinner>(R.id.spinnerStaff)

        // Function to setup spinner
        fun setupSpinner(users: List<id.mohamadsuhendy.vanishabakery.data.model.User>) {
            val staffList = users.filter { it.role == Constants.ROLE_STAFF }
            val staffNames = staffList.map { it.nama }.toMutableList()
            staffNames.add(0, "Belum Ditugaskan")
            
            val adapterStaff = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, staffNames)
            adapterStaff.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerStaff.adapter = adapterStaff
        }

        // Initial setup
        setupSpinner(viewModel.allUsers.value ?: emptyList())

        // Observe for changes while dialog is potentially loading
        viewModel.allUsers.observe(viewLifecycleOwner) { users ->
            setupSpinner(users)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Rute Baru")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val nama = etNamaRute.text.toString().trim()
                val kode = etKodeRute.text.toString().trim()
                val selectedPos = spinnerStaff.selectedItemPosition
                
                val staffList = (viewModel.allUsers.value ?: emptyList()).filter { it.role == Constants.ROLE_STAFF }
                val staffId = if (selectedPos == 0) "" else staffList[selectedPos - 1].uid
                val staffNama = if (selectedPos == 0) "Belum Ditugaskan" else staffList[selectedPos - 1].nama
                
                if (nama.isNotEmpty() && kode.isNotEmpty()) {
                    viewModel.addRute(nama, kode, staffId, staffNama)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showEditRuteDialog(rute: id.mohamadsuhendy.vanishabakery.data.model.Rute) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_rute, null)
        val etNamaRute = dialogView.findViewById<EditText>(R.id.etNamaRute)
        val etKodeRute = dialogView.findViewById<EditText>(R.id.etKodeRute)
        val spinnerStaff = dialogView.findViewById<Spinner>(R.id.spinnerStaff)

        etNamaRute.setText(rute.namaRute)
        etKodeRute.setText(rute.kode)
        
        // Hide spinner because staff update is not supported here easily, or we can just hide it
        spinnerStaff.visibility = View.GONE

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Rute")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val nama = etNamaRute.text.toString().trim()
                val kode = etKodeRute.text.toString().trim()
                
                if (nama.isNotEmpty() && kode.isNotEmpty()) {
                    viewModel.updateRute(rute.id, nama, kode)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ManageRuteBottomSheet"
    }
}
