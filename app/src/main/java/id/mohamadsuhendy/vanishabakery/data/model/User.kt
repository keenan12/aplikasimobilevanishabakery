package id.mohamadsuhendy.vanishabakery.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val nama: String = "",
    val email: String = "",
    val role: String = "staff", // "admin" or "staff"
    val fotoUrl: String = "",
    val sampulUrl: String = "",
    val isActive: Boolean = true,
    val isOnline: Boolean = false,
    val lastOnline: Timestamp? = null,
    val lastLogin: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun isAdmin() = role == "admin"
    fun isStaff() = role == "staff"
}
