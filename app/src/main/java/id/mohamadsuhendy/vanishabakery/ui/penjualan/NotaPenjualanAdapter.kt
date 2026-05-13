package id.mohamadsuhendy.vanishabakery.ui.penjualan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import id.mohamadsuhendy.vanishabakery.data.model.Penjualan
import id.mohamadsuhendy.vanishabakery.databinding.ItemNotaPenjualanBinding
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter untuk daftar nota penjualan di LaporanActivity.
 * Menampilkan setiap record penjualan dengan tombol edit dan hapus.
 */
class NotaPenjualanAdapter(
    private val onEdit: (Penjualan) -> Unit,
    private val onDelete: (Penjualan) -> Unit
) : ListAdapter<Penjualan, NotaPenjualanAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Penjualan>() {
            override fun areItemsTheSame(a: Penjualan, b: Penjualan) = a.id == b.id
            override fun areContentsTheSame(a: Penjualan, b: Penjualan) = a == b
        }
    }

    private val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotaPenjualanBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemNotaPenjualanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Penjualan) {
            binding.tvProdukNama.text = item.namaProduk
            binding.tvMitraNama.text = "📍 ${item.mitraNama}"
            binding.tvKirim.text = "Kirim: ${item.jumlahKirim}"
            binding.tvRetur.text = "Retur: ${item.jumlahRetur}"
            binding.tvTerjual.text = "Terjual: ${item.jumlahTerjual}"
            binding.tvOmset.text = formatter.format(item.totalHarga).replace(",00", "")

            binding.btnEdit.setOnClickListener { onEdit(item) }
            binding.btnDelete.setOnClickListener { onDelete(item) }
        }
    }
}
