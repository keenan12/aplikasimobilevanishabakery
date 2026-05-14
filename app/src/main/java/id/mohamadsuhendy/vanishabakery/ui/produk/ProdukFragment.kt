package id.mohamadsuhendy.vanishabakery.ui.produk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import id.mohamadsuhendy.vanishabakery.data.model.Produk
import id.mohamadsuhendy.vanishabakery.databinding.FragmentProdukBinding
import id.mohamadsuhendy.vanishabakery.utils.Constants
import id.mohamadsuhendy.vanishabakery.utils.gone
import id.mohamadsuhendy.vanishabakery.utils.visible

class ProdukFragment : Fragment() {

    private var _binding: FragmentProdukBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProdukViewModel by viewModels {
        val app = requireActivity().application as VanishaBakeryApp
        ProdukViewModel.Factory(app.produkRepository, app.authRepository)
    }

    private lateinit var adapter: ProdukAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProdukBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize adapter with default state
        adapter = ProdukAdapter(
            isAdmin = false,
            onEditClick = { produk -> showAddEditDialog(produk) },
            onDeleteClick = { produk -> showDeleteConfirmation(produk) }
        )
        binding.rvProduk.adapter = adapter
        
        setupAdminUI()
        setupSearch()
        observeData()
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun setupAdminUI() {
        viewModel.currentUserRole.observe(viewLifecycleOwner) { role ->
            val isAdmin = role == Constants.ROLE_ADMIN
            binding.fabAddProduk.visibility = if (isAdmin) View.VISIBLE else View.GONE
            adapter.updateAdminStatus(isAdmin)
            
            binding.fabAddProduk.setOnClickListener { showAddEditDialog(null) }
        }
    }

    private fun observeData() {
        binding.progressBar.visible()
        viewModel.produkList.observe(viewLifecycleOwner) { products ->
            binding.progressBar.gone()
            adapter.submitList(products)
            binding.tvTotalProduk.text = products.size.toString()
            binding.tvProdukAktif.text = products.count { it.isActive }.toString()
        }

        viewModel.actionState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is id.mohamadsuhendy.vanishabakery.utils.Result.Loading -> binding.progressBar.visible()
                is id.mohamadsuhendy.vanishabakery.utils.Result.Success -> {
                    binding.progressBar.gone()
                    Toast.makeText(context, result.data, Toast.LENGTH_SHORT).show()
                }
                is id.mohamadsuhendy.vanishabakery.utils.Result.Error -> {
                    binding.progressBar.gone()
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun showAddEditDialog(produk: Produk?) {
        val bottomSheet = AddProdukBottomSheet(produk) { newProduk ->
            if (produk == null) {
                viewModel.addProduk(newProduk)
            } else {
                viewModel.updateProduk(newProduk)
            }
        }
        bottomSheet.show(childFragmentManager, AddProdukBottomSheet.TAG)
    }

    private fun showDeleteConfirmation(produk: Produk) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Produk")
            .setMessage("Apakah Anda yakin ingin menghapus ${produk.nama}?")
            .setPositiveButton("Hapus") { _, _ ->
                viewModel.deleteProduk(produk.id)
                Toast.makeText(context, "Produk dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
