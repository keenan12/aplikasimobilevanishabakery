package id.mohamadsuhendy.vanishabakery.ui.produk

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import id.mohamadsuhendy.vanishabakery.data.model.Produk
import id.mohamadsuhendy.vanishabakery.databinding.BottomSheetAddProdukBinding
import id.mohamadsuhendy.vanishabakery.utils.loadImage
import id.mohamadsuhendy.vanishabakery.utils.showToast

class AddProdukBottomSheet(
    private val produk: Produk? = null,
    private val onSave: (Produk) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddProdukBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAddProdukBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (produk != null) {
            binding.tvTitle.text = "Edit Produk"
            binding.etNamaProduk.setText(produk.nama)
            binding.etHargaProduk.setText(produk.harga.toString())
            binding.etStokProduk.setText(produk.stok.toString())
        }

        binding.btnBatal.setOnClickListener { dismiss() }

        binding.btnSimpan.setOnClickListener {
            val nama = binding.etNamaProduk.text.toString().trim()
            val hargaStr = binding.etHargaProduk.text.toString().trim()
            val stokStr = binding.etStokProduk.text.toString().trim()

            if (nama.isEmpty() || hargaStr.isEmpty() || stokStr.isEmpty()) {
                requireContext().showToast("Harap isi semua data")
                return@setOnClickListener
            }

            try {
                val cleanHarga = hargaStr.replace(Regex("[^0-9]"), "")
                val cleanStok = stokStr.replace(Regex("[^0-9]"), "")
                
                if (cleanHarga.isEmpty() || cleanStok.isEmpty()) {
                    requireContext().showToast("Harga dan stok harus berupa angka valid")
                    return@setOnClickListener
                }

                val newProduk = Produk(
                    id = produk?.id ?: "",
                    nama = nama,
                    harga = cleanHarga.toInt(),
                    stok = cleanStok.toInt(),
                    fotoUrl = produk?.fotoUrl ?: ""
                )
                onSave(newProduk)
                dismiss()
            } catch (e: Exception) {
                requireContext().showToast("Harga dan stok harus berupa angka")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddProdukBottomSheet"
    }
}
