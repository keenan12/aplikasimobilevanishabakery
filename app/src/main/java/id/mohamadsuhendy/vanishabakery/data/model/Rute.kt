package id.mohamadsuhendy.vanishabakery.data.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class Rute(
    val id: String = "",
    val namaRute: String = "",       // e.g. "Cilandak", "Depok"
    val kode: String = "",           // e.g. "A1", "B2"
    val staffId: String = "",        // UID sales pemilik rute
    val staffNama: String = "",
    val totalMitra: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) : Parcelable
