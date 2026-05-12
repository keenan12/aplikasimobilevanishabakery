package id.mohamadsuhendy.vanishabakery.ui.profile

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import id.mohamadsuhendy.vanishabakery.R
import id.mohamadsuhendy.vanishabakery.data.model.User
import id.mohamadsuhendy.vanishabakery.databinding.ItemUserBinding
import id.mohamadsuhendy.vanishabakery.utils.loadImageCircle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserAdapter(
    private val onClick: (User) -> Unit
) : ListAdapter<User, UserAdapter.VH>(Diff()) {

    inner class VH(val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = getItem(position)
        with(holder.binding) {
            tvUserNama.text = user.nama
            tvUserEmail.text = user.email

            // Real-time Status & Last seen display
            try {
                val isOnline = user.isOnline
                val dateToUse = user.lastOnline?.toDate() ?: user.lastLogin?.toDate()
                
                if (dateToUse != null) {
                    val today = java.util.Date()
                    val diff = today.time - dateToUse.time
                    val minutes = diff / 60000
                    val hours = minutes / 60
                    val days = hours / 24

                    tvLastLogin.text = when {
                        isOnline -> "Sedang Online"
                        minutes < 60 -> "Terakhir online ${if (minutes == 0L) 1 else minutes}m lalu"
                        hours < 24 -> "Terakhir online $hours jam lalu"
                        days < 7 -> "Terakhir online $days hari lalu"
                        else -> "Terakhir online " + SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateToUse)
                    }

                    // Role-based Badge
                    tvUserStatus.text = user.role.uppercase()
                    val bgTint = if (user.isAdmin()) R.color.neo_magenta else R.color.neo_teal
                    
                    tvUserStatus.setTextColor(holder.itemView.context.getColor(R.color.black))
                    tvUserStatus.backgroundTintList = ColorStateList.valueOf(holder.itemView.context.getColor(bgTint))
                    
                } else {
                    tvLastLogin.text = "Belum pernah masuk"
                    tvUserStatus.text = user.role.uppercase()
                    val bgTint = if (user.isAdmin()) R.color.neo_magenta else R.color.neo_teal
                    
                    tvUserStatus.setTextColor(holder.itemView.context.getColor(R.color.black))
                    tvUserStatus.backgroundTintList = ColorStateList.valueOf(holder.itemView.context.getColor(bgTint))
                }
            } catch (e: Exception) {
                tvLastLogin.text = "Status tidak diketahui"
            }

            // Avatar loading with clear circle crop and fallback
            ivUserAvatar.loadImageCircle(user.fotoUrl, R.drawable.ic_person)

            root.setOnClickListener { onClick(user) }
        }
    }

    class Diff : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(a: User, b: User) = a.uid == b.uid
        override fun areContentsTheSame(a: User, b: User) = a == b
    }
}
