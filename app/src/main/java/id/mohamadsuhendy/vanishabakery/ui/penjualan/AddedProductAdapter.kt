package id.mohamadsuhendy.vanishabakery.ui.penjualan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import id.mohamadsuhendy.vanishabakery.databinding.ItemAddedProductBinding

data class AddedProduct(
    val namaProduk: String,
    val harga: Int,
    val dikirim: Int,
    val terjual: Int = 0,
    val isPenjualan: Boolean = true
) {
    val sisa: Int get() = if (isPenjualan) kotlin.math.max(0, dikirim - terjual) else 0
}

class AddedProductAdapter(
    private val onRemove: (AddedProduct) -> Unit
) : ListAdapter<AddedProduct, AddedProductAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAddedProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAddedProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AddedProduct) {
            binding.tvNamaProduk.text = item.namaProduk
            if (item.isPenjualan) {
                binding.tvDetail.text = "Harga: Rp${item.harga} | Dikirim: ${item.dikirim} | Terjual: ${item.terjual} | Sisa: ${item.sisa}"
            } else {
                binding.tvDetail.text = "Jumlah Dikirim: ${item.dikirim}"
            }
            binding.btnRemove.setOnClickListener { onRemove(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AddedProduct>() {
        override fun areItemsTheSame(oldItem: AddedProduct, newItem: AddedProduct) = oldItem.namaProduk == newItem.namaProduk
        override fun areContentsTheSame(oldItem: AddedProduct, newItem: AddedProduct) = oldItem == newItem
    }
}
