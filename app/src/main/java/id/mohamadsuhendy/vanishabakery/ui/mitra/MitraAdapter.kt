package id.mohamadsuhendy.vanishabakery.ui.mitra

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import id.mohamadsuhendy.vanishabakery.R
import id.mohamadsuhendy.vanishabakery.data.model.Mitra
import id.mohamadsuhendy.vanishabakery.databinding.ItemMitraBinding
import id.mohamadsuhendy.vanishabakery.utils.Constants
import id.mohamadsuhendy.vanishabakery.utils.loadImage

class MitraAdapter(
    private val onItemClick: (Mitra) -> Unit,
    private val onMapClick: (Mitra) -> Unit
) : ListAdapter<Mitra, MitraAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemMitraBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(mitra: Mitra) {
            binding.tvNamaToko.text = mitra.namaToko
            binding.tvPemilik.text = "Sales: ${mitra.staffNama}"
            binding.tvAlamat.text = mitra.alamat
            binding.ivToko.loadImage(mitra.fotoUrl, R.drawable.ic_store)

            // Status badge
            val (statusText, bgColor, textColor) = when (mitra.status) {
                Constants.STATUS_APPROVED -> Triple(
                    "Disetujui", R.color.status_approved_bg, R.color.status_approved
                )
                Constants.STATUS_REJECTED -> Triple(
                    "Ditolak", R.color.status_rejected_bg, R.color.status_rejected
                )
                Constants.STATUS_DELETED -> Triple(
                    "Dihapus / Nonaktif", R.color.status_rejected_bg, R.color.status_rejected
                )
                else -> Triple(
                    "Menunggu", R.color.status_pending_bg, R.color.status_pending
                )
            }
            binding.tvStatus.text = statusText
            binding.tvStatus.backgroundTintList = ContextCompat.getColorStateList(binding.root.context, bgColor)
            binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, textColor))

            // Badge dot color
            binding.badgeStatus.backgroundTintList = ContextCompat.getColorStateList(
                binding.root.context, textColor
            )

            binding.root.setOnClickListener { onItemClick(mitra) }
            binding.ivMap.setOnClickListener { onMapClick(mitra) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMitraBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Mitra>() {
            override fun areItemsTheSame(oldItem: Mitra, newItem: Mitra) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Mitra, newItem: Mitra) = oldItem == newItem
        }
    }
}
