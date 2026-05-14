package id.mohamadsuhendy.vanishabakery.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import id.mohamadsuhendy.vanishabakery.data.model.User
import id.mohamadsuhendy.vanishabakery.data.remote.FirebaseDataSource
import id.mohamadsuhendy.vanishabakery.utils.Constants
import id.mohamadsuhendy.vanishabakery.utils.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val firebaseDataSource: FirebaseDataSource
) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser
    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.Error("UID tidak ditemukan")
            val user = firebaseDataSource.getUserById(uid)
                ?: return Result.Error("Data akun tidak ditemukan di sistem")
            if (!user.isActive) return Result.Error("Akun Anda telah dinonaktifkan oleh Admin")
            // Write login metadata (Non-blocking: allow login even if this fails)
            try {
                db.collection(Constants.COLLECTION_USERS).document(uid)
                    .update(
                        "lastLogin", com.google.firebase.Timestamp.now(),
                        "isOnline", true,
                        "lastOnline", com.google.firebase.Timestamp.now()
                    ).await()
            } catch (e: Exception) {
                // Silently fail or log metadata update error
            }
            Result.Success(user)
        } catch (e: Exception) {
            val rawMessage = e.message ?: ""
            val msg = when {
                rawMessage.contains("INVALID_LOGIN_CREDENTIALS") ||
                rawMessage.contains("INVALID_PASSWORD") ||
                rawMessage.contains("password is invalid") ||
                rawMessage.contains("no user record") ||
                rawMessage.contains("user-not-found") ->
                    "sandi atau username salah"
                rawMessage.contains("badly formatted") ||
                rawMessage.contains("invalid-email") ->
                    "Format email $email tidak valid."
                rawMessage.contains("too-many-requests") ||
                rawMessage.contains("blocked all requests") ->
                    "Terlalu banyak percobaan. Tunggu sebentar."
                rawMessage.contains("network") ->
                    "Koneksi internet bermasalah."
                else -> "Gagal ($email): ${e.localizedMessage ?: "Cek email/sandi"}"
            }
            Result.Error(msg, e)
        }
    }

    fun logout() = auth.signOut()

    suspend fun getCurrentUserData(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return firebaseDataSource.getUserById(uid)
    }

    fun observeCurrentUser(): Flow<User?> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(null)
        return firebaseDataSource.observeUserById(uid)
    }

    suspend fun getAllUsers(): List<User> = firebaseDataSource.getAllUsers()

    fun observeAllUsers(): Flow<List<User>> = firebaseDataSource.observeAllUsers()

    suspend fun createUser(email: String, password: String, nama: String, role: String): Result<String> {
        return try {
            // Use secondary FirebaseApp to avoid logging out the current admin
            val context = com.google.firebase.FirebaseApp.getInstance().applicationContext
            val options = com.google.firebase.FirebaseApp.getInstance().options
            val secondaryApp = try {
                com.google.firebase.FirebaseApp.getInstance("SecondaryApp")
            } catch (e: Exception) {
                com.google.firebase.FirebaseApp.initializeApp(context, options, "SecondaryApp")
            }
            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp!!)
            
            val result = secondaryAuth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.Error("Gagal membuat user")
            
            val user = User(
                uid = uid, nama = nama, email = email, role = role, isActive = true,
                createdAt = com.google.firebase.Timestamp.now()
            )
            db.collection(Constants.COLLECTION_USERS).document(uid).set(user).await()
            
            // Log out the secondary instance so it's clean for the next creation
            secondaryAuth.signOut()
            
            Result.Success(uid)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Gagal membuat user", e)
        }
    }

    suspend fun setUserActive(uid: String, isActive: Boolean): Result<Unit> {
        return try {
            firebaseDataSource.updateUserActive(uid, isActive)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Gagal memperbarui status user", e)
        }
    }

    suspend fun updateProfilePhoto(uid: String, photoUrl: String) {
        firebaseDataSource.updateUserPhoto(uid, photoUrl)
    }

    suspend fun updateSampulPhoto(uid: String, sampulUrl: String) {
        firebaseDataSource.updateUserSampul(uid, sampulUrl)
    }

    suspend fun updateOnlineStatus(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        try {
            db.collection(Constants.COLLECTION_USERS).document(uid)
                .update(
                    "isOnline", isOnline,
                    "lastOnline", com.google.firebase.Timestamp.now()
                )
        } catch (e: Exception) {
            // ignore error for status update
        }
    }

    suspend fun addLog(log: id.mohamadsuhendy.vanishabakery.data.model.LogAktivitas) {
        firebaseDataSource.addLogAktivitas(log)
    }
}
