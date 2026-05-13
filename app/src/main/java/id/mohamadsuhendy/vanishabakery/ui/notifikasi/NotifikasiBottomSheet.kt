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

            app.firebaseDataSource.observeNotifikasiByUser(user.uid, user.isAdmin())
                .collect { notifResult ->
                    val notifs = if (notifResult is id.mohamadsuhendy.vanishabakery.utils.Result.Success) {
                        notifResult.data
                            .filter { !it.isRead } // Hanya tampilkan yang belum dibaca
                            .map { notif ->
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

                    adapter.submitList(notifs)
                    if (notifs.isEmpty()) {
                        binding.layoutEmptyNotif.visibility = View.VISIBLE
                        binding.rvNotifikasi.visibility = View.GONE
                    } else {
                        binding.layoutEmptyNotif.visibility = View.GONE
                        binding.rvNotifikasi.visibility = View.VISIBLE
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
