package id.mohamadsuhendy.vanishabakery.ui.bottomsheet

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import id.mohamadsuhendy.vanishabakery.databinding.FragmentActionBottomSheetBinding
import id.mohamadsuhendy.vanishabakery.ui.mitra.AddMitraBottomSheet
import androidx.lifecycle.lifecycleScope
import id.mohamadsuhendy.vanishabakery.ui.profile.ManageRuteBottomSheet
import kotlinx.coroutines.launch
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import id.mohamadsuhendy.vanishabakery.ui.mitra.ManageMitraActivity
import id.mohamadsuhendy.vanishabakery.ui.penjualan.RapidEntryActivity
import id.mohamadsuhendy.vanishabakery.ui.laporan.LaporanActivity

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
                // Admin: Show Input Nota + Laporan Excel + Kelola Mitra
                binding.btnInputPengiriman.visibility = View.VISIBLE  // "Input Nota Mingguan"
                binding.btnLaporanExcel.visibility = View.VISIBLE
                binding.btnInputPenjualan.visibility = View.GONE
                binding.btnKelolaMitra.visibility = View.VISIBLE
                binding.btnKelolaRute.visibility = View.GONE
            } else {
                // Sales: Only show Tambah Mitra + Data Rute
                binding.btnInputPengiriman.visibility = View.GONE
                binding.btnLaporanExcel.visibility = View.GONE
                binding.btnInputPenjualan.visibility = View.GONE
                binding.btnKelolaMitra.visibility = View.GONE
                binding.btnKelolaRute.visibility = View.VISIBLE
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

        // "Input Nota Mingguan" → RapidEntryActivity (Admin only)
        binding.btnInputPengiriman.setOnClickListener {
            val context = requireContext()
            dismiss()
            startActivity(Intent(context, RapidEntryActivity::class.java))
        }

        // "Laporan Excel" → LaporanActivity (Admin only)
        binding.btnLaporanExcel.setOnClickListener {
            val context = requireContext()
            dismiss()
            startActivity(Intent(context, LaporanActivity::class.java))
        }

        // Old penjualan button (hidden)
        binding.btnInputPenjualan.setOnClickListener { }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ActionBottomSheet"
    }
}
