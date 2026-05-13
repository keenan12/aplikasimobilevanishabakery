package id.mohamadsuhendy.vanishabakery.data.remote

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import id.mohamadsuhendy.vanishabakery.data.model.*
import id.mohamadsuhendy.vanishabakery.utils.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import id.mohamadsuhendy.vanishabakery.utils.Result
import java.util.UUID

class FirebaseDataSource {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // ======================== AUTH / USER ========================

    suspend fun getUserById(uid: String): User? {
        return try {
            val doc = db.collection(Constants.COLLECTION_USERS).document(uid).get().await()
            doc.toObject(User::class.java)?.copy(uid = doc.id)
        } catch (e: Exception) { null }
    }

    fun observeUserById(uid: String): Flow<User?> = callbackFlow {
        val listener = db.collection(Constants.COLLECTION_USERS).document(uid)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObject(User::class.java)?.copy(uid = snap.id))
            }
        awaitClose { listener.remove() }
    }

    suspend fun getAllUsers(): List<User> {
        return try {
            db.collection(Constants.COLLECTION_USERS).get().await()
                .documents.mapNotNull { it.toObject(User::class.java)?.copy(uid = it.id) }
        } catch (e: Exception) { emptyList() }
    }

    fun observeAllUsers(): Flow<List<User>> = callbackFlow {
        val listener = db.collection(Constants.COLLECTION_USERS)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { 
                    it.toObject(User::class.java)?.copy(uid = it.id) 
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateUserActive(uid: String, isActive: Boolean) {
        db.collection(Constants.COLLECTION_USERS).document(uid)
            .update("isActive", isActive).await()
    }

    suspend fun updateUserPhoto(uid: String, photoUrl: String) {
        db.collection(Constants.COLLECTION_USERS).document(uid)
            .update("fotoUrl", photoUrl, "updatedAt", Timestamp.now()).await()
    }

    suspend fun updateUserSampul(uid: String, sampulUrl: String) {
        db.collection(Constants.COLLECTION_USERS).document(uid)
            .update("sampulUrl", sampulUrl, "updatedAt", Timestamp.now()).await()
    }

    // ======================== RUTE ========================

    /** Admin: observe semua rute aktif */
    fun observeAllRute(): Flow<List<Rute>> = callbackFlow {
        val listener = db.collection(Constants.COLLECTION_RUTE)
            .orderBy("namaRute", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull {
                    val rute = it.toObject(Rute::class.java)?.copy(id = it.id)
                    if (rute?.isActive == true) rute else null
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /** Sales: observe hanya rute miliknya (isolasi data) */
    fun observeRuteByStaff(staffId: String): Flow<List<Rute>> = callbackFlow {
        val listener = db.collection(Constants.COLLECTION_RUTE)
            .whereEqualTo("staffId", staffId)
            // Removed orderBy to prevent Firestore index requirements
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull {
                    val rute = it.toObject(Rute::class.java)?.copy(id = it.id)
                    if (rute?.isActive == true) rute else null
                }?.sortedBy { it.namaRute } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addRute(rute: Rute): String {
        val docRef = db.collection(Constants.COLLECTION_RUTE).add(rute).await()
        return docRef.id
    }

    suspend fun updateRute(ruteId: String, namaRute: String, kode: String) {
        db.collection(Constants.COLLECTION_RUTE).document(ruteId)
            .update(
                "namaRute", namaRute,
                "kode", kode,
                "updatedAt", Timestamp.now()
            ).await()
    }

    suspend fun updateRuteTotalMitra(ruteId: String, total: Int) {
        db.collection(Constants.COLLECTION_RUTE).document(ruteId)
            .update("totalMitra", total, "updatedAt", Timestamp.now()).await()
    }

    suspend fun deleteRute(ruteId: String) {
        db.collection(Constants.COLLECTION_RUTE).document(ruteId).delete().await()
    }

    // ======================== MITRA ========================

    /** Admin: observe semua mitra */
    fun observeAllMitra(): Flow<List<Mitra>> = callbackFlow {
        val listener = db.collection(Constants.COLLECTION_MITRA)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull {
                    it.toObject(Mitra::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /** Sales: observe hanya mitra miliknya (isolasi data) */
    fun observeMitraByStaff(staffId: String): Flow<List<Mitra>> = callbackFlow {
        val listener = db.collection(Constants.COLLECTION_MITRA)
            .whereEqualTo("staffId", staffId)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull {
                    it.toObject(Mitra::class.java)?.copy(id = it.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /** Admin: filter by status (untuk halaman approval) */
    fun observeMitraByStatus(status: String): Flow<List<Mitra>> = callbackFlow {
        val listener = db.collection(Constants.COLLECTION_MITRA)
            .whereEqualTo("status", status)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull {
                    it.toObject(Mitra::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /** Sales: filter mitra miliknya by rute */
    fun observeMitraByStaffAndRute(staffId: String, ruteId: String): Flow<List<Mitra>> = callbackFlow {
        val listener = db.collection(Constants.COLLECTION_MITRA)
            .whereEqualTo("staffId", staffId)
            .whereEqualTo("ruteId", ruteId)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull {
                    it.toObject(Mitra::class.java)?.copy(id = it.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getMitraById(id: String): Mitra? {
        return try {
            val doc = db.collection(Constants.COLLECTION_MITRA).document(id).get().await()
            doc.toObject(Mitra::class.java)?.copy(id = doc.id)
        } catch (e: Exception) { null }
    }

    suspend fun addMitra(mitra: Mitra): String {
        val docRef = db.collection(Constants.COLLECTION_MITRA).add(mitra).await()
        return docRef.id
    }

    suspend fun updateMitraStatus(mitraId: String, status: String) {
        db.collection(Constants.COLLECTION_MITRA).document(mitraId)
            .update("status", status, "updatedAt", Timestamp.now()).await()
    }

    suspend fun deleteMitra(mitraId: String) {
        db.collection(Constants.COLLECTION_MITRA).document(mitraId).delete().await()
    }

    // ======================== PENGIRIMAN ========================

    /** Admin: observe semua pengiriman */
    fun observeAllPengiriman(): Flow<List<Pengiriman>> = callbackFlow {
        val listener = db.collection(Constants.COLLECTION_PENGIRIMAN)
            .orderBy("tanggal", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull {
                    it.toObject(Pengiriman::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /** Sales: observe hanya pengiriman miliknya (isolasi data) */
    fun observePengirimanByStaff(staffId: String): Flow<List<Pengiriman>> = callbackFlow {
        val listener = db.collection(Constants.COLLECTION_PENGIRIMAN)
            .whereEqualTo("staffId", staffId)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull {
                    it.toObject(Pengiriman::class.java)?.copy(id = it.id)
                }?.sortedByDescending { it.tanggal } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addPengiriman(pengiriman: Pengiriman): String {
        val docRef = db.collection(Constants.COLLECTION_PENGIRIMAN).add(pengiriman).await()
        return docRef.id
    }

    // ======================== PENJUALAN ========================

    /** Admin: observe semua penjualan */
    fun observeAllPenjualan(): Flow<List<Penjualan>> = callbackFlow {
        val listener = db.collection(Constants.COLLECTION_PENJUALAN)
            .orderBy("tanggalNota", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull {
                    it.toObject(Penjualan::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /** Observe penjualan untuk periode tertentu (untuk heatmap & laporan) */
    fun observePenjualanByPeriode(startTs: com.google.firebase.Timestamp, endTs: com.google.firebase.Timestamp): Flow<List<Penjualan>> = callbackFlow {
        val listener = db.collection(Constants.COLLECTION_PENJUALAN)
            .whereGreaterThanOrEqualTo("tanggalNota", startTs)
            .whereLessThan("tanggalNota", endTs)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull {
                    it.toObject(Penjualan::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addPenjualan(penjualan: Penjualan): String {
        val docRef = db.collection(Constants.COLLECTION_PENJUALAN).add(penjualan).await()
        return docRef.id
    }

    /** Hapus dokumen penjualan dari Firestore */
    suspend fun deletePenjualan(docId: String) {
        db.collection(Constants.COLLECTION_PENJUALAN).document(docId).delete().await()
    }

    /** Update field kirim/retur/terjual/totalHarga di Firestore */
    suspend fun updatePenjualan(docId: String, kirim: Int, retur: Int, terjual: Int, totalHarga: Int) {
        db.collection(Constants.COLLECTION_PENJUALAN).document(docId).update(
            mapOf(
                "jumlahKirim" to kirim,
                "jumlahRetur" to retur,
                "jumlahTerjual" to terjual,
                "totalHarga" to totalHarga
            )
        ).await()
    }

    // ======================== LOG AKTIVITAS ========================

    /** Admin: observe semua log */
    fun observeLogAktivitas(): Flow<Result<List<LogAktivitas>>> = callbackFlow {
        val listener = db.collection(Constants.COLLECTION_LOG_AKTIVITAS)
            .limit(100)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(Result.Error(error.message ?: "Unknown Error"))
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull {
                    try {
                        it.toObject(LogAktivitas::class.java)?.copy(id = it.id)
                    } catch (e: Exception) { null }
                } ?: emptyList()
                
                // Sort in-memory to avoid hiding docs missing 'createdAt'
                val sorted = list.sortedByDescending { it.createdAt }
                trySend(Result.Success(sorted))
            }
        awaitClose { listener.remove() }
    }

    /** Sales: observe hanya log miliknya */
    fun observeLogAktivitasByUser(userId: String): Flow<Result<List<LogAktivitas>>> = callbackFlow {
        val listener = db.collection(Constants.COLLECTION_LOG_AKTIVITAS)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(Result.Error(error.message ?: "Unknown Error"))
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull {
                    it.toObject(LogAktivitas::class.java)?.copy(id = it.id)
                } ?: emptyList()
                
                // Sort in-memory to avoid hiding docs missing 'createdAt'
                val sorted = list.sortedByDescending { it.createdAt }
                trySend(Result.Success(sorted))
            }
        awaitClose { listener.remove() }
    }

    suspend fun addLogAktivitas(log: LogAktivitas) {
        db.collection(Constants.COLLECTION_LOG_AKTIVITAS).add(log).await()
    }

    suspend fun deleteLogAktivitas(logId: String) {
        if (logId.isNotEmpty()) {
            db.collection(Constants.COLLECTION_LOG_AKTIVITAS).document(logId).delete().await()
        }
    }

    // ======================== PRODUK = :D ========================

    fun observeAllProduk(): Flow<List<Produk>> = callbackFlow {
        val listener = db.collection(Constants.COLLECTION_PRODUK)
            .orderBy("nama", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull {
                    it.toObject(Produk::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addProduk(produk: Produk): String {
        val docRef = db.collection(Constants.COLLECTION_PRODUK).add(produk).await()
        return docRef.id
    }

    suspend fun updateProduk(produk: Produk) {
        db.collection(Constants.COLLECTION_PRODUK).document(produk.id)
            .set(produk).await()
    }

    suspend fun deleteProduk(produkId: String) {
        db.collection(Constants.COLLECTION_PRODUK).document(produkId).delete().await()
    }

    // ======================== STORAGE ========================

    suspend fun uploadMitraPhoto(localUri: Uri, mitraId: String): String {
        val ref = storage.reference
            .child("${Constants.STORAGE_MITRA_PHOTOS}/$mitraId.jpg")
        ref.putFile(localUri).await()
        return ref.downloadUrl.await().toString()
    }

    /** Upload profile photo using raw bytes — avoids FileProvider URI resolution issues */
    /** 
     * ALTERNATIVE: Use Firestore to store small images as Base64 
     * to avoid needing Firebase Storage (and bank account upgrade)
     */
    suspend fun uploadProfilePhotoBytes(bytes: ByteArray, userId: String): String {
        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
        return "data:image/jpeg;base64,$base64"
    }

    suspend fun uploadProdukPhotoBytes(bytes: ByteArray, produkId: String): String {
        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
        return "data:image/jpeg;base64,$base64"
    }

    /** Legacy: upload via URI (kept for mitra photo) */
    suspend fun uploadProfilePhoto(localUri: Uri, userId: String): String {
        val ref = storage.reference
            .child("${Constants.STORAGE_PROFILE_PHOTOS}/$userId.jpg")
        ref.putFile(localUri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun uploadProdukPhoto(localUri: Uri, produkId: String): String {
        val ref = storage.reference
            .child("produk_photos/${produkId}_${System.currentTimeMillis()}.jpg")
        ref.putFile(localUri).await()
        return ref.downloadUrl.await().toString()
    }

    // ======================== NOTIFIKASI ========================

    /** Observe pending mitras directly - used as notifications for admin */
    fun observePendingMitra(): kotlinx.coroutines.flow.Flow<List<Mitra>> = callbackFlow {
        val listener = db.collection(Constants.COLLECTION_MITRA)
            .whereEqualTo("status", Constants.STATUS_PENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull {
                    try { it.toObject(Mitra::class.java)?.copy(id = it.id) } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(list.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }


    fun observeNotifikasiByUser(userId: String, isAdmin: Boolean): Flow<Result<List<Notifikasi>>> = callbackFlow {
        val listener = db.collection(Constants.COLLECTION_NOTIFIKASI)
            .limit(50)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(Result.Error(error.message ?: "Unknown Error"))
                    return@addSnapshotListener
                }
                
                val list = snap?.documents?.mapNotNull {
                    try {
                        it.toObject(Notifikasi::class.java)?.copy(id = it.id)
                    } catch (e: Exception) { null }
                } ?: emptyList()

                val filtered = if (isAdmin) list else list.filter { it.targetUserId == userId || it.targetUserId.isEmpty() }
                
                // Sort in-memory to avoid hiding docs missing 'createdAt'
                val sorted = filtered.sortedByDescending { it.createdAt }
                trySend(Result.Success(sorted))
            }
        awaitClose { listener.remove() }
    }

    suspend fun addNotifikasi(notif: Notifikasi) {
        db.collection(Constants.COLLECTION_NOTIFIKASI).add(notif).await()
    }

    suspend fun markAllNotifAsRead(userId: String, isAdmin: Boolean) {
        try {
            val query = if (isAdmin) {
                db.collection(Constants.COLLECTION_NOTIFIKASI)
                    .whereIn("targetUserId", listOf("", userId))
                    .whereEqualTo("isRead", false)
            } else {
                db.collection(Constants.COLLECTION_NOTIFIKASI)
                    .whereEqualTo("targetUserId", userId)
                    .whereEqualTo("isRead", false)
            }

            val docs = query.get().await()
            if (docs.isEmpty) return
            
            db.runBatch { batch ->
                for (doc in docs) {
                    batch.update(doc.reference, "isRead", true)
                }
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ======================== STOK RUTE ========================

    fun observeStokRuteByPeriode(ruteId: String, startTs: Timestamp, endTs: Timestamp): Flow<List<StokRute>> = callbackFlow {
        val listener = db.collection(Constants.COLLECTION_STOK_RUTE)
            .whereEqualTo("ruteId", ruteId)
            .whereGreaterThanOrEqualTo("tanggal", startTs)
            .whereLessThan("tanggal", endTs)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull {
                    it.toObject(StokRute::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addStokRute(stokRute: StokRute): String {
        val docRef = db.collection(Constants.COLLECTION_STOK_RUTE).add(stokRute).await()
        return docRef.id
    }

    fun observeAllStokRuteByPeriode(startTs: Timestamp, endTs: Timestamp): Flow<List<StokRute>> = callbackFlow {
        val listener = db.collection(Constants.COLLECTION_STOK_RUTE)
            .whereGreaterThanOrEqualTo("tanggal", startTs)
            .whereLessThan("tanggal", endTs)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull {
                    it.toObject(StokRute::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }
}
