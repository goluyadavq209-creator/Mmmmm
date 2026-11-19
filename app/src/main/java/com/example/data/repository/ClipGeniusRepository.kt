package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ConnectedAccount
import com.example.data.model.GeneratedClip
import com.example.data.model.PlanTier
import com.example.data.model.ProcessingLog
import com.example.data.model.PublishJob
import com.example.data.model.SystemConfig
import com.example.data.model.UserProfile
import com.example.data.model.VideoProject
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ClipGeniusRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun isCurrentUserAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email.equals(ADMIN_EMAIL, ignoreCase = true) && user.isEmailVerified
    }

    // --- USER PROFILE ---
    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .catch { error ->
                logFlowError(error, OperationType.GET, path, userId)
                throw error
            }
    }

    suspend fun ensureUserProfileExists(
        userId: String,
        displayName: String,
        email: String,
        photoUrl: String?
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Cross-user profile init rejected." }
            val ref = db.collection("users").document(userId)
            val snap = ref.get().await()
            if (!snap.exists()) {
                val profile = UserProfile(
                    userId = uid,
                    displayName = displayName.ifBlank { "Creator" },
                    email = email,
                    photoUrl = photoUrl,
                    planTier = PlanTier.FREE.name,
                    dailyClipsUsed = 0,
                    dailyClipLimit = PlanTier.FREE.defaultDailyLimit
                )
                ref.set(profile.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserPlan(userId: String, tier: PlanTier, dailyLimit: Int): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireUserId()
            db.collection("users").document(userId).update(
                mapOf(
                    "planTier" to tier.name,
                    "dailyClipLimit" to dailyLimit.coerceIn(1, 1000),
                    "autoPublishEnabled" to tier.supportsAutoPublish,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserPreferences(
        userId: String,
        autoPublishEnabled: Boolean? = null,
        preferredSubtitleLang: String? = null,
        preferredCaptionStyle: String? = null
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Cross-user preference update rejected." }
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (autoPublishEnabled != null) updates["autoPublishEnabled"] = autoPublishEnabled
            if (preferredSubtitleLang != null) updates["preferredSubtitleLang"] = preferredSubtitleLang
            if (preferredCaptionStyle != null) updates["preferredCaptionStyle"] = preferredCaptionStyle
            db.collection("users").document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun incrementUsageStats(
        userId: String,
        clipsAdded: Int = 0,
        publishedAdded: Int = 0,
        storageMbAdded: Double = 0.0
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Cross-user stats update rejected." }
            val ref = db.collection("users").document(userId)
            val snap = ref.get().await()
            val currentDaily = (snap.getLong("dailyClipsUsed") ?: 0L).toInt()
            val currentTotal = (snap.getLong("totalClipsGenerated") ?: 0L).toInt()
            val currentPub = (snap.getLong("totalPublished") ?: 0L).toInt()
            val currentStorage = snap.getDouble("storageUsedMb") ?: 0.0

            ref.update(
                mapOf(
                    "dailyClipsUsed" to (currentDaily + clipsAdded).coerceIn(0, 1000),
                    "totalClipsGenerated" to (currentTotal + clipsAdded).coerceIn(0, 100000),
                    "totalPublished" to (currentPub + publishedAdded).coerceIn(0, 100000),
                    "storageUsedMb" to (currentStorage + storageMbAdded).coerceIn(0.0, 100000.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- VIDEO PROJECTS ---
    fun observeProjects(userId: String): Flow<List<VideoProject>> {
        val path = "users/$userId/projects"
        return db.collection("users").document(userId).collection("projects")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(VideoProject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    suspend fun createProject(project: VideoProject): Result<String> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("users").document(uid).collection("projects").document()
        return try {
            val payload = project.copy(userId = uid).toCreateMap()
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProjectProgress(
        projectId: String,
        status: String,
        progressPercent: Int,
        currentStage: String,
        durationSeconds: Int? = null,
        transcript: String? = null,
        summary: String? = null,
        clipCount: Int? = null,
        errorMessage: String? = null
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to status,
                "progressPercent" to progressPercent.coerceIn(0, 100),
                "currentStage" to currentStage.take(160),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (durationSeconds != null) updates["durationSeconds"] = durationSeconds.coerceIn(0, 86400)
            if (transcript != null) updates["transcript"] = transcript.take(40000)
            if (summary != null) updates["summary"] = summary.take(2000)
            if (clipCount != null) updates["clipCount"] = clipCount.coerceIn(0, 50)
            if (errorMessage != null) updates["errorMessage"] = errorMessage.take(1000)
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- GENERATED CLIPS ---
    fun observeClips(userId: String): Flow<List<GeneratedClip>> {
        val path = "users/$userId/clips"
        return db.collection("users").document(userId).collection("clips")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(GeneratedClip::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    suspend fun createClips(clips: List<GeneratedClip>): Result<List<String>> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val colRef = db.collection("users").document(uid).collection("clips")
        return try {
            val batch = db.batch()
            val createdIds = mutableListOf<String>()
            clips.forEach { clip ->
                val ref = colRef.document()
                createdIds.add(ref.id)
                batch.set(ref, clip.copy(userId = uid).toCreateMap())
            }
            batch.commit().await()
            Result.success(createdIds)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateClip(clipId: String, fields: Map<String, Any>): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("users").document(uid).collection("clips").document(clipId)
        return try {
            require(clipId.isNotBlank()) { "Clip ID must not be blank" }
            val payload = fields.toMutableMap()
            payload["updatedAt"] = FieldValue.serverTimestamp()
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteClip(clipId: String): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("users").document(uid).collection("clips").document(clipId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- CONNECTED SOCIAL ACCOUNTS ---
    fun observeSocialAccounts(userId: String): Flow<List<ConnectedAccount>> {
        val path = "users/$userId/social_accounts"
        return db.collection("users").document(userId).collection("social_accounts")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ConnectedAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    suspend fun addSocialAccount(account: ConnectedAccount): Result<String> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("users").document(uid).collection("social_accounts").document()
        return try {
            docRef.set(account.copy(userId = uid).toCreateMap()).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateSocialAccount(
        accountId: String,
        privacyDefault: String,
        autoPublish: Boolean
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("users").document(uid).collection("social_accounts").document(accountId)
        return try {
            docRef.update(
                mapOf(
                    "privacyDefault" to privacyDefault,
                    "autoPublish" to autoPublish,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun removeSocialAccount(accountId: String): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("users").document(uid).collection("social_accounts").document(accountId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- PUBLISH JOBS ---
    fun observePublishJobs(userId: String): Flow<List<PublishJob>> {
        val path = "users/$userId/publish_jobs"
        return db.collection("users").document(userId).collection("publish_jobs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PublishJob::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    suspend fun createPublishJob(job: PublishJob): Result<String> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("users").document(uid).collection("publish_jobs").document()
        return try {
            docRef.set(job.copy(userId = uid).toCreateMap()).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updatePublishJobStatus(
        jobId: String,
        status: String,
        platformPostUrl: String = "",
        errorMessage: String = ""
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("users").document(uid).collection("publish_jobs").document(jobId)
        return try {
            docRef.update(
                mapOf(
                    "status" to status,
                    "platformPostUrl" to platformPostUrl.take(500),
                    "errorMessage" to errorMessage.take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- TELEMETRY LOGS & SYSTEM CONFIG (CREATOR + ADMIN) ---
    suspend fun logProcessingEvent(
        eventType: String,
        status: String,
        message: String,
        clipsCount: Int = 0,
        durationMs: Long = 0L
    ): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("admin_logs").document()
        return try {
            val log = ProcessingLog(
                userId = user.uid,
                userEmail = user.email.orEmpty(),
                eventType = eventType,
                status = status,
                message = message,
                clipsCount = clipsCount,
                durationMs = durationMs
            )
            docRef.set(log.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeSystemConfig(): Flow<SystemConfig> {
        val path = "system_config/global"
        return db.collection("system_config").document("global")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(SystemConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: SystemConfig()
                } else {
                    SystemConfig()
                }
            }
            .catch { error ->
                val uid = auth.currentUser?.uid
                if (uid != null) logFlowError(error, OperationType.GET, path, uid)
                emit(SystemConfig())
            }
    }

    suspend fun saveSystemConfig(config: SystemConfig): Result<Unit> {
        val path = "system_config/global"
        return try {
            requireUserId()
            db.collection("system_config").document("global")
                .set(config.toWriteMap())
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeUserLogs(userId: String): Flow<List<ProcessingLog>> {
        val path = "admin_logs"
        return db.collection("admin_logs")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ProcessingLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeAllLogsForAdmin(adminUid: String): Flow<List<ProcessingLog>> {
        val path = "admin_logs"
        return db.collection("admin_logs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ProcessingLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, adminUid)
                throw error
            }
    }

    fun observeAllUsersForAdmin(adminUid: String): Flow<List<UserProfile>> {
        val path = "users"
        return db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, adminUid)
                throw error
            }
    }

    private fun logFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    companion object {
        const val ADMIN_EMAIL = "maansinghyadav095@gmail.com"
    }
}
