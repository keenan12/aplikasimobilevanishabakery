package id.mohamadsuhendy.vanishabakery.ui.penjualan

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import id.mohamadsuhendy.vanishabakery.data.model.Mitra
import id.mohamadsuhendy.vanishabakery.databinding.BottomSheetAddPenjualanBinding
import id.mohamadsuhendy.vanishabakery.utils.Result
import id.mohamadsuhendy.vanishabakery.utils.gone
import id.mohamadsuhendy.vanishabakery.utils.showToast
import id.mohamadsuhendy.vanishabakery.utils.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import id.mohamadsuhendy.vanishabakery.utils.Constants
import android.widget.TextView
import id.mohamadsuhendy.vanishabakery.R
import com.google.android.material.textfield.TextInputEditText

class AddPenjualanBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddPenjualanBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PenjualanViewModel by viewModels {
        val app = requireActivity().application as VanishaBakeryApp
        PenjualanViewModel.Factory(app.penjualanRepository, app.authRepository, app.produkRepository)
    }

    private var mitraList: List<Mitra> = emptyList()
    private var selectedMitra: Mitra? = null
    private val addedProducts = mutableListOf<AddedProduct>()
    private lateinit var addedProductAdapter: AddedProductAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAddPenjualanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadMitraList()
        setupProductList()
        setupAddedProductsList()

        binding.btnAddToList.setOnClickListener { addProductToList() }
        binding.btnSimpan.setOnClickListener { savePenjualan() }
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
            val names = mitraList.map { it.namaToko }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
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
            
            binding.acNamaProduk.setOnItemClickListener { _, _, _, _ ->
                val selectedName = binding.acNamaProduk.text.toString()
                val selectedProduct = products.find { it.nama == selectedName }
                selectedProduct?.let {
                    binding.etHargaSatuan.setText(it.harga.toString())
                }
            }
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
        val hargaStr = binding.etHargaSatuan.text.toString()
        val dikirimStr = binding.etJumlahDikirim.text.toString()
        val terjualStr = binding.etJumlahTerjual.text.toString()

        if (nama.isBlank()) { binding.tilNamaProduk.error = "Pilih produk"; return }
        val harga = hargaStr.toIntOrNull() ?: 0
        val dikirim = dikirimStr.toIntOrNull() ?: 0
        val terjual = terjualStr.toIntOrNull() ?: 0

        if (harga <= 0) { binding.tilHargaSatuan.error = "Harga tidak valid"; return }
        if (dikirim <= 0) { binding.tilJumlahDikirim.error = "Isi jumlah"; return }
        if (terjual > dikirim) { binding.tilJumlahTerjual.error = "Melebihi dikirim"; return }

        // Check if already added
        if (addedProducts.any { it.namaProduk == nama }) {
            requireContext().showToast("$nama sudah ada di daftar")
            return
        }

        addedProducts.add(AddedProduct(nama, harga, dikirim, terjual))
        addedProductAdapter.submitList(addedProducts.toList())

        // Clear inputs
        binding.acNamaProduk.setText("")
        binding.etHargaSatuan.setText("")
        binding.etJumlahDikirim.setText("")
        binding.etJumlahTerjual.setText("")
        binding.tilNamaProduk.error = null
        binding.tilHargaSatuan.error = null
        binding.tilJumlahDikirim.error = null
        binding.tilJumlahTerjual.error = null
    }

    private fun savePenjualan() {
        // This bottom sheet is no longer used — input is done via RapidEntryActivity
        requireContext().showToast("Gunakan menu Input Nota Mingguan untuk input penjualan")
        dismiss()
    }

    private fun observeViewModel() {
        viewModel.saveState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> { binding.progressBar.visible(); binding.btnSimpan.isEnabled = false }
                is Result.Success -> {
                    binding.progressBar.gone()
                    requireContext().showToast("Penjualan berhasil dicatat!")
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

    companion object { const val TAG = "AddPenjualanBottomSheet" }
}
