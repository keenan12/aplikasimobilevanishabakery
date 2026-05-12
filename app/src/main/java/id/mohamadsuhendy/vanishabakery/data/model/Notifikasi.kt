package id.mohamadsuhendy.vanishabakery.data.model

import com.google.firebase.Timestamp

data class Notifikasi(
    val id: String = "",
    val judul: String = "",
    val pesan: String = "",
    val targetUserId: String = "", // empty for all admins
    val referensiId: String = "",
    val tipe: String = "",
    val isRead: Boolean = false,
    val createdAt: Timestamp? = null
)
