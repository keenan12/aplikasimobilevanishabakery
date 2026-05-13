package id.mohamadsuhendy.vanishabakery.ui.notifikasi

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import id.mohamadsuhendy.vanishabakery.VanishaBakeryApp
import id.mohamadsuhendy.vanishabakery.databinding.BottomSheetNotifikasiBinding
import id.mohamadsuhendy.vanishabakery.databinding.ItemNotifikasiBinding
import id.mohamadsuhendy.vanishabakery.ui.mitra.MitraDetailActivity
import id.mohamadsuhendy.vanishabakery.utils.Constants
import id.mohamadsuhendy.vanishabakery.utils.gone
import id.mohamadsuhendy.vanishabakery.utils.showToast
import id.mohamadsuhendy.vanishabakery.utils.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NotifItem(
    val notifId: String = "",
    val judul: String,
    val pesan: String,
    val waktu: Date?,
    val isUnread: Boolean = true,
    val referensiId: String = ""
)

class NotifikasiBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetNotifikasiBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "NotifikasiBottomSheet"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetNotifikasiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as VanishaBakeryApp
        val adapter = NotifAdapter { item ->
            if (item.notifId.isNotEmpty()) {
                app.firebaseDataSource.markNotifAsRead(item.notifId)
            }
            if (item.referensiId.isNotEmpty()) {
                val intent = Intent(requireContext(), MitraDetailActivity::class.java).apply {
                    putExtra(Constants.KEY_MITRA_ID, item.referensiId)
                }
                startActivity(intent)
                dismiss()
            }
        }
        binding.rvNotifikasi.adapter = adapter
        binding.rvNotifikasi.layoutManager = LinearLayoutManager(requireContext())

        binding.btnTandaiSemua.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val user = app.authRepository.getCurrentUserData() ?: return@launch
                app.firebaseDataSource.markAllNotifAsRead(user.uid, user.isAdmin())
                dismiss()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val user = app.authRepository.getCurrentUserData() ?: return@launch

            if (user.isAdmin()) {
                // For Admin: Combine pending mitras + regular notifications for a complete picture
                kotlinx.coroutines.flow.combine(
                    app.firebaseDataSource.observePendingMitra(),
                    app.firebaseDataSource.observeNotifikasiByUser(user.uid, true)
                ) { pendingMitras, notifResult ->

                    // Convert pending mitras to notification items
                    val mitraNotifs = pendingMitras.map { mitra ->
                        NotifItem(
                            notifId = "", // pending mitras are not actual notif documents
                            judul = "📋 Pengajuan Mitra Baru",
                            pesan = "${mitra.staffNama} mengajukan: ${mitra.namaToko} (${mitra.ruteNama})",
                            waktu = mitra.createdAt?.toDate(),
                            isUnread = true,
                            referensiId = mitra.id
                        )
                    }

                    // Get regular notifications
                    val regularNotifs = if (notifResult is id.mohamadsuhendy.vanishabakery.utils.Result.Success) {
                        notifResult.data.map { notif ->
                            NotifItem(
                                notifId = notif.id,
                                judul = notif.judul,
                                pesan = notif.pesan,
                                waktu = notif.createdAt?.toDate(),
                                isUnread = !notif.isRead,
                                referensiId = notif.referensiId
                            )
                        }
                    } else emptyList()

                    // Merge: pending mitras first, then other notifs (deduplicated by referensiId)
                    val allNotifs = (mitraNotifs + regularNotifs)
                        .distinctBy { it.referensiId.ifEmpty { it.judul + it.pesan } }
                        .sortedByDescending { it.waktu }

                    allNotifs
                }.collect { allNotifs ->
                    adapter.submitList(allNotifs)
                    if (allNotifs.isEmpty()) {
                        binding.layoutEmptyNotif.visibility = View.VISIBLE
                        binding.rvNotifikasi.visibility = View.GONE
                    } else {
                        binding.layoutEmptyNotif.visibility = View.GONE
                        binding.rvNotifikasi.visibility = View.VISIBLE
                    }
                }
            } else {
                // For Staff: Combine their mitra status changes + targeted notifications
                kotlinx.coroutines.flow.combine(
                    app.firebaseDataSource.observeMitraByStaff(user.uid),
                    app.firebaseDataSource.observeNotifikasiByUser(user.uid, false)
                ) { mitraList, notifResult ->

                    // Show recently approved/rejected mitras as notifications
                    val mitraStatusNotifs = mitraList
                        .filter { it.status == "approved" || it.status == "rejected" }
                        .sortedByDescending { it.updatedAt }
                        .map { mitra ->
                            val emoji = if (mitra.status == "approved") "✅" else "❌"
                            val statusText = if (mitra.status == "approved") "Disetujui" else "Ditolak"
                            NotifItem(
                                notifId = "",
                                judul = "$emoji Mitra $statusText",
                                pesan = "Pengajuan mitra ${mitra.namaToko} telah $statusText oleh Admin",
                                waktu = mitra.updatedAt?.toDate() ?: mitra.createdAt?.toDate(),
                                isUnread = true,
                                referensiId = mitra.id
                            )
                        }

                    // Get targeted notifications from notifikasi collection
                    val targetedNotifs = if (notifResult is id.mohamadsuhendy.vanishabakery.utils.Result.Success) {
                        notifResult.data.map { notif ->
                            NotifItem(
                                notifId = notif.id,
                                judul = notif.judul,
                                pesan = notif.pesan,
                                waktu = notif.createdAt?.toDate(),
                                isUnread = !notif.isRead,
                                referensiId = notif.referensiId
                            )
                        }
                    } else emptyList()

                    // Merge and deduplicate
                    (mitraStatusNotifs + targetedNotifs)
                        .distinctBy { it.referensiId.ifEmpty { it.judul + it.pesan } }
                        .sortedByDescending { it.waktu }

                }.collect { staffNotifs ->
                    adapter.submitList(staffNotifs)
                    if (staffNotifs.isEmpty()) {
                        binding.layoutEmptyNotif.visibility = View.VISIBLE
                        binding.rvNotifikasi.visibility = View.GONE
                    } else {
                        binding.layoutEmptyNotif.visibility = View.GONE
                        binding.rvNotifikasi.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class NotifAdapter(private val onClick: (NotifItem) -> Unit) : ListAdapter<NotifItem, NotifAdapter.VH>(Diff()) {
    inner class VH(val binding: ItemNotifikasiBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemNotifikasiBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            tvNotifJudul.text = item.judul
            tvNotifPesan.text = item.pesan
            if (item.isUnread) dotUnread.visible() else dotUnread.gone()
            tvNotifWaktu.text = item.waktu?.let {
                SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id")).format(it)
            } ?: "-"
            
            root.setOnClickListener { onClick(item) }
        }
    }

    class Diff : DiffUtil.ItemCallback<NotifItem>() {
        override fun areItemsTheSame(a: NotifItem, b: NotifItem) = a.referensiId == b.referensiId && a.judul == b.judul
        override fun areContentsTheSame(a: NotifItem, b: NotifItem) = a == b
    }
}
