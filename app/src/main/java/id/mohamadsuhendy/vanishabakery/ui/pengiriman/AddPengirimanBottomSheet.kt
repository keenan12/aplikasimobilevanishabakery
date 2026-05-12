package id.mohamadsuhendy.vanishabakery.ui.pengiriman

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import id.mohamadsuhendy.vanishabakery.data.model.Mitra
import id.mohamadsuhendy.vanishabakery.databinding.BottomSheetAddPengirimanBinding
import id.mohamadsuhendy.vanishabakery.utils.Result
import id.mohamadsuhendy.vanishabakery.utils.gone
import id.mohamadsuhendy.vanishabakery.utils.showToast
import id.mohamadsuhendy.vanishabakery.utils.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import id.mohamadsuhendy.vanishabakery.utils.Constants
import android.widget.TextView
import id.mohamadsuhendy.vanishabakery.R
import com.google.android.material.textfield.TextInputEditText
import id.mohamadsuhendy.vanishabakery.ui.penjualan.AddedProduct
import id.mohamadsuhendy.vanishabakery.ui.penjualan.AddedProductAdapter

class AddPengirimanBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddPengirimanBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PengirimanViewModel by viewModels {
        val app = requireActivity().application as VanishaBakeryApp
        PengirimanViewModel.Factory(app.pengirimanRepository, app.authRepository, app.produkRepository)
    }

    private var mitraList: List<Mitra> = emptyList()
    private var selectedMitra: Mitra? = null
    private var selectedDate = Calendar.getInstance()
    private val addedProducts = mutableListOf<AddedProduct>()
    private lateinit var addedProductAdapter: AddedProductAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAddPengirimanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadMitraList()
        setupProductList()
        setupAddedProductsList()
        setupDatePicker()
        updateDateDisplay()

        binding.btnAddToList.setOnClickListener { addProductToList() }
        binding.btnSimpan.setOnClickListener { savePengiriman() }
        binding.btnBatal.setOnClickListener { dismiss() }

        // Trigger dropdown on click for non-editable fields
        binding.acMitra.setOnClickListener { binding.acMitra.showDropDown() }
        binding.acNamaProduk.setOnClickListener { binding.acNamaProduk.showDropDown() }

        observeViewModel()
    }

    private fun loadMitraList() {
        val app = requireActivity().application as VanishaBakeryApp
        val uid = app.authRepository.currentUser?.uid ?: ""
        
        CoroutineScope(Dispatchers.Main).launch {
            val user = app.authRepository.getCurrentUserData()
            val flow = if (user?.isAdmin() == true) {
                app.mitraRepository.observeAllMitra()
            } else {
                app.mitraRepository.observeMitraByStaff(uid)
            }
            
            mitraList = flow.first().filter { it.isApproved() }
            val mitraNames = mitraList.map { it.namaToko }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, mitraNames)
            binding.acMitra.setAdapter(adapter)
            binding.acMitra.setOnItemClickListener { _, _, position, _ ->
                selectedMitra = mitraList[position]
            }
        }
    }

    private fun setupProductList() {
        viewModel.produkList.observe(viewLifecycleOwner) { products ->
            if (products.isEmpty()) {
                binding.acNamaProduk.setHint("Tambahkan produk di menu Produk dulu")
            } else {
                binding.acNamaProduk.setHint("Pilih Produk")
            }
            
            val productNames = products.map { it.nama }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, productNames)
            binding.acNamaProduk.setAdapter(adapter)
        }
    }

    private fun setupAddedProductsList() {
        addedProductAdapter = AddedProductAdapter { item ->
            addedProducts.remove(item)
            addedProductAdapter.submitList(addedProducts.toList())
        }
        binding.rvAddedProducts.adapter = addedProductAdapter
    }

    private fun addProductToList() {
        val nama = binding.acNamaProduk.text.toString()
        val jumlahStr = binding.etJumlah.text.toString()

        if (nama.isBlank()) { binding.tilNamaProduk.error = "Pilih produk"; return }
        val jumlah = jumlahStr.toIntOrNull() ?: 0

        if (jumlah <= 0) { binding.tilJumlah.error = "Jumlah tidak valid"; return }

        // Check if already added
        if (addedProducts.any { it.namaProduk == nama }) {
            requireContext().showToast("$nama sudah ada di daftar")
            return
        }

        addedProducts.add(AddedProduct(nama, 0, jumlah, isPenjualan = false))
        addedProductAdapter.submitList(addedProducts.toList())

        // Clear inputs
        binding.acNamaProduk.setText("")
        binding.etJumlah.setText("")
        binding.tilNamaProduk.error = null
        binding.tilJumlah.error = null
    }

    private fun setupDatePicker() {
        binding.etTanggal.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedDate.set(year, month, day)
                    updateDateDisplay()
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateDateDisplay() {
        binding.etTanggal.setText(
            SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(selectedDate.time)
        )
    }

    private fun savePengiriman() {
        val mitra = selectedMitra ?: run { requireContext().showToast("Pilih mitra terlebih dahulu"); return }
        
        if (addedProducts.isEmpty()) {
            requireContext().showToast("Daftar produk masih kosong")
            return
        }
        
        addedProducts.forEach { item ->
            viewModel.addPengiriman(mitra.id, mitra.namaToko, mitra.ruteId, mitra.ruteNama, item.namaProduk, item.dikirim)
        }
    }

    private fun observeViewModel() {
        viewModel.saveState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> { binding.progressBar.visible(); binding.btnSimpan.isEnabled = false }
                is Result.Success -> {
                    binding.progressBar.gone()
                    requireContext().showToast("Pengiriman berhasil dicatat!")
                    dismiss()
                }
                is Result.Error -> {
                    binding.progressBar.gone()
                    binding.btnSimpan.isEnabled = true
                    requireContext().showToast(result.message)
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object { const val TAG = "AddPengirimanBottomSheet" }
}
