package id.mohamadsuhendy.vanishabakery.ui.produk

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import id.mohamadsuhendy.vanishabakery.data.model.Produk
import id.mohamadsuhendy.vanishabakery.databinding.ItemProdukBinding
import id.mohamadsuhendy.vanishabakery.utils.visible
import id.mohamadsuhendy.vanishabakery.utils.gone

class ProdukAdapter(
    private var isAdmin: Boolean,
    private val onEditClick: (Produk) -> Unit,
    private val onDeleteClick: (Produk) -> Unit
) : ListAdapter<Produk, ProdukAdapter.ViewHolder>(DiffCallback()) {

    fun updateAdminStatus(status: Boolean) {
        isAdmin = status
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProdukBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemProdukBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(produk: Produk) {
            binding.tvNamaProduk.text = produk.nama
            binding.tvHargaProduk.text = "Rp ${String.format("%,d", produk.harga).replace(',', '.')}"
            binding.tvStokProduk.text = "Stok: ${produk.stok} pcs"
            
            if (isAdmin) {
                binding.layoutAdminActions.visible()
                binding.btnEditProduk.setOnClickListener { onEditClick(produk) }
                binding.btnDeleteProduk.setOnClickListener { onDeleteClick(produk) }
            } else {
                binding.layoutAdminActions.gone()
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Produk>() {
        override fun areItemsTheSame(oldItem: Produk, newItem: Produk) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Produk, newItem: Produk) = oldItem == newItem
    }
}
