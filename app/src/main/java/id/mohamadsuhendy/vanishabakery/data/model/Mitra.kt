package id.mohamadsuhendy.vanishabakery.data.model

import com.google.firebase.Timestamp
import android.os.Parcelable
import id.mohamadsuhendy.vanishabakery.utils.Constants
import kotlinx.parcelize.Parcelize

@Parcelize
data class Mitra(
    val id: String = "",
    val namaToko: String = "",
    val fotoUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val alamat: String = "",
    val status: String = "pending", // "pending", "approved", "rejected"
    val staffId: String = "",
    val staffNama: String = "",
    val ruteId: String = "",         // ID rute yang dipilih saat tambah mitra
    val ruteNama: String = "",       // Nama rute (Cilandak, Depok, dll)
    val isSynced: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) : Parcelable {
    fun isPending() = status == Constants.STATUS_PENDING
    fun isApproved() = status == Constants.STATUS_APPROVED
    fun isRejected() = status == Constants.STATUS_REJECTED
    fun isDeleted() = status == Constants.STATUS_DELETED
}
