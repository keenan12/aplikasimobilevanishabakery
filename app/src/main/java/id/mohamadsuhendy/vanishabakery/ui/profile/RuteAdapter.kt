package id.mohamadsuhendy.vanishabakery.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import id.mohamadsuhendy.vanishabakery.data.model.Rute
import id.mohamadsuhendy.vanishabakery.databinding.ItemRuteBinding

class RuteAdapter(
    private val onEditClick: (Rute) -> Unit,
    private val onDeleteClick: (Rute) -> Unit
) : ListAdapter<Rute, RuteAdapter.RuteViewHolder>(RuteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuteViewHolder {
        val binding = ItemRuteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RuteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RuteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RuteViewHolder(private val binding: ItemRuteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(rute: Rute) {
            binding.tvNamaRute.text = rute.namaRute
            binding.tvStaff.text = "Sales: ${rute.staffNama}"
            binding.tvKodeRute.text = "CODE: ${rute.kode}"
            binding.btnEdit.setOnClickListener { onEditClick(rute) }
            binding.btnDelete.setOnClickListener { onDeleteClick(rute) }
        }
    }

    class RuteDiffCallback : DiffUtil.ItemCallback<Rute>() {
        override fun areItemsTheSame(oldItem: Rute, newItem: Rute) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Rute, newItem: Rute) = oldItem == newItem
    }
}
