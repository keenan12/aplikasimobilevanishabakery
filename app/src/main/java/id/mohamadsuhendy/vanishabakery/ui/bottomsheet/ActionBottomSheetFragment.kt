package id.mohamadsuhendy.vanishabakery.ui.bottomsheet

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import id.mohamadsuhendy.vanishabakery.databinding.FragmentActionBottomSheetBinding
import id.mohamadsuhendy.vanishabakery.ui.mitra.AddMitraBottomSheet
import id.mohamadsuhendy.vanishabakery.ui.pengiriman.AddPengirimanBottomSheet
import id.mohamadsuhendy.vanishabakery.ui.penjualan.AddPenjualanBottomSheet
import androidx.lifecycle.lifecycleScope
import id.mohamadsuhendy.vanishabakery.utils.showToast
import id.mohamadsuhendy.vanishabakery.ui.profile.ManageRuteBottomSheet
import androidx.fragment.app.activityViewModels
import kotlinx.coroutines.launch
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import id.mohamadsuhendy.vanishabakery.ui.mitra.MitraViewModel
import id.mohamadsuhendy.vanishabakery.ui.mitra.KelolaMitraBottomSheet
import id.mohamadsuhendy.vanishabakery.ui.mitra.RuteSalesActivity

import id.mohamadsuhendy.vanishabakery.ui.mitra.ManageMitraActivity

class ActionBottomSheetFragment : BottomSheetDialogFragment() {
 
    private var _binding: FragmentActionBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentActionBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val app = requireActivity().application as VanishaBakeryApp
        
        lifecycleScope.launch {
            val user = app.authRepository.getCurrentUserData()
            if (user?.isAdmin() == true) {
                binding.btnKelolaMitra.visibility = android.view.View.VISIBLE
                binding.btnKelolaRute.visibility = android.view.View.GONE
                binding.tvKelolaMitra.text = "PENGELOLAAN MITRA"
            } else {
                // For Sales: Show DATA RUTE to access the searchable list
                binding.btnKelolaMitra.visibility = android.view.View.GONE
                binding.btnKelolaRute.visibility = android.view.View.VISIBLE
                binding.tvKelolaRute.text = "DATA RUTE"
            }
        }

        binding.btnKelolaRute.setOnClickListener {
            val context = requireContext()
            lifecycleScope.launch {
                val user = app.authRepository.getCurrentUserData()
                if (user?.isAdmin() == true) {
                    val fm = requireActivity().supportFragmentManager
                    ManageRuteBottomSheet().show(fm, "ManageRute")
                } else {
                    // Sales: Open the integrated ManageMitraActivity directly
                    val intent = Intent(context, ManageMitraActivity::class.java)
                    startActivity(intent)
                }
                dismiss()
            }
        }

        binding.btnKelolaMitra.setOnClickListener {
            val context = requireContext()
            lifecycleScope.launch {
                val user = app.authRepository.getCurrentUserData()
                if (user?.isAdmin() == true) {
                    val intent = Intent(context, ManageMitraActivity::class.java)
                    startActivity(intent)
                    dismiss()
                }
            }
        }

        binding.btnTambahMitra.setOnClickListener {
            val fm = requireActivity().supportFragmentManager
            dismiss()
            AddMitraBottomSheet().show(fm, AddMitraBottomSheet.TAG)
        }

        binding.btnInputPengiriman.setOnClickListener {
            val fm = requireActivity().supportFragmentManager
            dismiss()
            AddPengirimanBottomSheet().show(fm, AddPengirimanBottomSheet.TAG)
        }

        binding.btnInputPenjualan.setOnClickListener {
            val fm = requireActivity().supportFragmentManager
            dismiss()
            AddPenjualanBottomSheet().show(fm, AddPenjualanBottomSheet.TAG)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ActionBottomSheet"
    }
}
