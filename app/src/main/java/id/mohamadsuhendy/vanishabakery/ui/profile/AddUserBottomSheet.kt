package id.mohamadsuhendy.vanishabakery.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import id.mohamadsuhendy.vanishabakery.databinding.BottomSheetAddUserBinding
import id.mohamadsuhendy.vanishabakery.utils.Constants
import id.mohamadsuhendy.vanishabakery.utils.Result
import id.mohamadsuhendy.vanishabakery.utils.gone
import id.mohamadsuhendy.vanishabakery.utils.showToast
import id.mohamadsuhendy.vanishabakery.utils.visible

class AddUserBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddUserBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels({ requireParentFragment() }) {
        val app = requireActivity().application as VanishaBakeryApp
        ProfileViewModel.Factory(app.authRepository, app.firebaseDataSource, app.ruteRepository)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAddUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSimpan.setOnClickListener {
            val nama = binding.etNama.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()
            val role = if (binding.rbAdmin.isChecked) Constants.ROLE_ADMIN else Constants.ROLE_STAFF

            if (nama.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                requireContext().showToast("Semua field harus diisi!")
                return@setOnClickListener
            }
            if (pass.length < 6) {
                requireContext().showToast("Password minimal 6 karakter")
                return@setOnClickListener
            }

            viewModel.registerUser(email, pass, nama, role)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.registerState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visible()
                    binding.btnSimpan.text = ""
                    binding.btnSimpan.isEnabled = false
                }
                is Result.Success -> {
                    requireContext().showToast("Akun berhasil dibuat!")
                    viewModel.loadAllUsers() // Refresh list
                    dismiss()
                }
                is Result.Error -> {
                    binding.progressBar.gone()
                    binding.btnSimpan.text = "Daftarkan Akun"
                    binding.btnSimpan.isEnabled = true
                    requireContext().showToast(result.message)
                }
                else -> {} // Idle
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset state
        viewModel.resetRegisterState()
        _binding = null
    }

    companion object {
        const val TAG = "AddUserBottomSheet"
    }
}
