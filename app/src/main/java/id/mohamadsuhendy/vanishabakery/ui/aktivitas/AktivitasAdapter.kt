package id.mohamadsuhendy.vanishabakery.ui.aktivitas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import id.mohamadsuhendy.vanishabakery.R
import id.mohamadsuhendy.vanishabakery.data.model.LogAktivitas
import id.mohamadsuhendy.vanishabakery.databinding.ItemAktivitasBinding
import id.mohamadsuhendy.vanishabakery.utils.Constants
import id.mohamadsuhendy.vanishabakery.utils.toRelativeTime

class AktivitasAdapter(
    private val onItemClick: (LogAktivitas) -> Unit
) : ListAdapter<LogAktivitas, AktivitasAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemAktivitasBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(log: LogAktivitas) {
            binding.tvAktivitas.text = log.aksi
            binding.tvDetail.text = log.deskripsi
            binding.tvUser.text = "Oleh: ${log.userNama} · ${log.userRole.capitalizeRole()}"
            binding.tvWaktu.text = log.createdAt?.toDate()?.time?.toRelativeTime() ?: "-"

            // Set icon and color based on activity type
            val (iconRes, iconColorRes, bgColorRes) = when (log.tipe) {
                Constants.LOG_TYPE_MITRA -> Triple(R.drawable.ic_store, R.color.primary, R.color.primary_container)
                Constants.LOG_TYPE_PENGIRIMAN -> Triple(R.drawable.ic_delivery, R.color.status_pending, R.color.status_pending_bg)
                Constants.LOG_TYPE_PENJUALAN -> Triple(R.drawable.ic_sales, R.color.status_approved, R.color.status_approved_bg)
                else -> Triple(R.drawable.ic_activity, R.color.text_secondary, R.color.surface_variant)
            }
            val ctx = binding.root.context
            binding.ivIcon.setImageResource(iconRes)
            binding.ivIcon.imageTintList = ContextCompat.getColorStateList(ctx, iconColorRes)
            
            // Set background circle color
            binding.ivIcon.parent?.let { parent ->
                if (parent is android.view.View) {
                    parent.backgroundTintList = ContextCompat.getColorStateList(ctx, bgColorRes)
                }
            }

            binding.root.setOnClickListener { onItemClick(log) }
        }

        private fun String.capitalizeRole() = replaceFirstChar { it.uppercase() }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAktivitasBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<LogAktivitas>() {
            override fun areItemsTheSame(oldItem: LogAktivitas, newItem: LogAktivitas) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: LogAktivitas, newItem: LogAktivitas) = oldItem == newItem
        }
    }
}
