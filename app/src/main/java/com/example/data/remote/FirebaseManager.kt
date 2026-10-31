package com.example.data.remote

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.*
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

object FirebaseManager {
  private const val TAG = "FirebaseManager"

  private var db: FirebaseFirestore? = null
  private val auth: FirebaseAuth by lazy { Firebase.auth }
  private val storage: FirebaseStorage by lazy {
    try {
      Firebase.storage
    } catch (e: Exception) {
      FirebaseStorage.getInstance()
    }
  }

  private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
  val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

  private val _userProfile = MutableStateFlow<UserProfile?>(null)
  val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

  private val _resources = MutableStateFlow<List<ResourceItem>>(emptyList())
  val resources: StateFlow<List<ResourceItem>> = _resources.asStateFlow()

  private val _thoughts = MutableStateFlow<List<ThoughtItem>>(emptyList())
  val thoughts: StateFlow<List<ThoughtItem>> = _thoughts.asStateFlow()

  private val _comments = MutableStateFlow<List<CommentItem>>(emptyList())
  val comments: StateFlow<List<CommentItem>> = _comments.asStateFlow()

  private var profileListener: ListenerRegistration? = null
  private var resourcesListener: ListenerRegistration? = null
  private var thoughtsListener: ListenerRegistration? = null
  private var commentsListener: ListenerRegistration? = null

  private var appContext: Context? = null
  private val scope = CoroutineScope(Dispatchers.IO)

  fun init(context: Context) {
    appContext = context.applicationContext
    try {
      if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
        try {
          com.google.firebase.FirebaseApp.initializeApp(context)
        } catch (e: Exception) {
          Log.w(TAG, "FirebaseApp.initializeApp non-fatal in test environment", e)
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "FirebaseApp check non-fatal", e)
    }

    try {
      val databaseId = context.getString(R.string.firestore_database_id)
      db = FirebaseFirestore.getInstance(databaseId)
      Log.i(TAG, "FirebaseFirestore initialized with custom databaseId: $databaseId")
    } catch (e: Exception) {
      try {
        db = FirebaseFirestore.getInstance()
      } catch (ex: Throwable) {
        Log.w(TAG, "Firestore initialization fallback", ex)
      }
    }

    try {
      // Attach AuthStateListener
      auth.addAuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _currentUser.value = user
        if (user != null) {
          attachUserListeners(user.uid)
        } else {
          detachUserListeners()
        }
      }

      // Initial check
      auth.currentUser?.let { user ->
        _currentUser.value = user
        attachUserListeners(user.uid)
      }
    } catch (e: Throwable) {
      Log.w(TAG, "FirebaseAuth initialization in test environment", e)
    }
  }

  fun getFirestore(): FirebaseFirestore {
    return db ?: FirebaseFirestore.getInstance()
  }

  private fun attachUserListeners(uid: String) {
    val firestore = db ?: return

    // 1. User Profile listener
    profileListener?.remove()
    profileListener = firestore.collection("users").document(uid)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.w(TAG, "Listen to user profile failed", error)
          return@addSnapshotListener
        }
        if (snapshot != null && snapshot.exists()) {
          val data = snapshot.data ?: return@addSnapshotListener
          val profile = UserProfile(
            id = uid,
            username = data["username"] as? String ?: "",
            displayName = data["displayName"] as? String ?: "",
            bio = data["bio"] as? String ?: "",
            avatarUrl = data["photoUrl"] as? String ?: "",
            dateOfBirth = data["dateOfBirth"] as? String ?: "",
            educationalLevel = data["educationLevel"] as? String ?: "",
            courseOrDegree = data["course"] as? String ?: "",
            fieldOfStudy = data["fieldOfStudy"] as? String ?: "",
            institution = data["institution"] as? String ?: "",
            country = data["country"] as? String ?: "",
            state = data["state"] as? String ?: "",
            connectionType = data["connectionType"] as? String ?: "",
            connectionDetail = data["connectionDetail"] as? String ?: "",
            isProfileCompleted = data["isProfileCompleted"] as? Boolean ?: false,
            isUsernameLocked = data["isUsernameLocked"] as? Boolean ?: (data["isProfileCompleted"] as? Boolean ?: false),
            followersCount = (data["followersCount"] as? Number)?.toInt() ?: 0,
            followingCount = (data["followingCount"] as? Number)?.toInt() ?: 0,
            publishedCount = (data["publishedCount"] as? Number)?.toInt() ?: 0,
            isCurrentUser = true
          )
          _userProfile.value = profile
        }
      }

    // 2. Resources listener
    resourcesListener?.remove()
    resourcesListener = firestore.collection("resources")
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.w(TAG, "Listen to resources failed", error)
          return@addSnapshotListener
        }
        if (snapshot != null) {
          val items = snapshot.documents.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            val categoryStr = d["category"] as? String ?: "NOTES"
            val resType = try {
              ResourceType.valueOf(categoryStr)
            } catch (e: Exception) {
              ResourceType.NOTES
            }
            val resId = doc.id
            val ctx = appContext
            val isDownloaded = if (ctx != null) com.example.data.pdf.PdfFileManager.isPdfDownloaded(ctx, resId) else false
            val localPath = if (ctx != null) com.example.data.pdf.PdfFileManager.getPersistedDownloadPath(ctx, resId) else null

            ResourceItem(
              id = resId,
              title = d["title"] as? String ?: "Untitled",
              description = d["description"] as? String ?: "",
              creatorId = d["authorId"] as? String ?: "",
              creatorUsername = d["authorUsername"] as? String ?: "",
              creatorDisplayName = d["authorName"] as? String ?: "Learner",
              creatorAvatarUrl = d["authorAvatar"] as? String ?: "",
              resourceType = resType,
              subject = d["subject"] as? String ?: "General",
              topic = d["topic"] as? String ?: "",
              educationalLevel = d["educationalLevel"] as? String ?: "General",
              fileSize = d["fileSize"] as? String ?: "2.4 MB",
              publicationDate = d["publicationDate"] as? String ?: "Recent",
              viewsCount = (d["viewsCount"] as? Number)?.toInt() ?: 0,
              downloadsCount = (d["downloadsCount"] as? Number)?.toInt() ?: 0,
              averageRating = (d["averageRating"] as? Number)?.toFloat() ?: 0.0f,
              ratingsCount = (d["ratingsCount"] as? Number)?.toInt() ?: 0,
              commentsCount = (d["commentsCount"] as? Number)?.toInt() ?: 0,
              coverUrl = d["coverUrl"] as? String ?: "",
              pdfUrl = d["pdfUrl"] as? String ?: "",
              isDownloaded = isDownloaded,
              localFilePath = localPath
            )
          }
          _resources.value = items
        }
      }

    // 3. Thoughts listener
    thoughtsListener?.remove()
    thoughtsListener = firestore.collection("thoughts")
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.w(TAG, "Listen to thoughts failed", error)
          return@addSnapshotListener
        }
        if (snapshot != null) {
          val items = snapshot.documents.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            ThoughtItem(
              id = doc.id,
              authorId = d["authorId"] as? String ?: "",
              authorUsername = d["authorUsername"] as? String ?: "",
              authorDisplayName = d["authorName"] as? String ?: "Learner",
              authorAvatarUrl = d["authorAvatar"] as? String ?: "",
              content = d["content"] as? String ?: "",
              publicationDate = d["publicationDate"] as? String ?: "Recent",
              likesCount = (d["likesCount"] as? Number)?.toInt() ?: 0,
              commentsCount = (d["commentsCount"] as? Number)?.toInt() ?: 0
            )
          }
          _thoughts.value = items
        }
      }
  }

  private fun detachUserListeners() {
    profileListener?.remove()
    profileListener = null
    resourcesListener?.remove()
    resourcesListener = null
    thoughtsListener?.remove()
    thoughtsListener = null
    commentsListener?.remove()
    commentsListener = null
    _userProfile.value = null
  }

  // --- Authentication with Google Credential ---
  suspend fun signInWithGoogleCredential(idToken: String): Result<FirebaseUser> {
    return try {
      val credential = GoogleAuthProvider.getCredential(idToken, null)
      val authResult = auth.signInWithCredential(credential).awaitResult()
      val user = authResult.user ?: throw IllegalStateException("FirebaseUser is null after sign-in")

      // Ensure Firestore user document exists without overwriting returning user's data
      val firestore = getFirestore()
      val userDoc = firestore.collection("users").document(user.uid).get().awaitResult()

      if (!userDoc.exists()) {
        val initialProfile = hashMapOf<String, Any>(
          "uid" to user.uid,
          "email" to (user.email ?: ""),
          "displayName" to (user.displayName ?: "Learner"),
          "photoUrl" to (user.photoUrl?.toString() ?: ""),
          "username" to (user.email?.substringBefore("@") ?: "learner_${user.uid.take(5)}"),
          "isProfileCompleted" to false,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("users").document(user.uid).set(initialProfile, SetOptions.merge()).awaitResult()
      }

      Result.success(user)
    } catch (e: Throwable) {
      Log.e(TAG, "signInWithGoogleCredential error", e)
      Result.failure(e)
    }
  }

  // --- Phone Number Authentication (Real 6-Digit SMS OTP) ---
  fun sendPhoneOtp(
    activity: Activity,
    phoneNumber: String,
    resendToken: PhoneAuthProvider.ForceResendingToken? = null,
    onCodeSent: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken) -> Unit,
    onVerificationCompleted: (PhoneAuthCredential) -> Unit,
    onError: (String) -> Unit
  ) {
    try {
      val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
        .setPhoneNumber(phoneNumber.trim())
        .setTimeout(60L, TimeUnit.SECONDS)
        .setActivity(activity)
        .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
          override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            Log.i(TAG, "Phone auto-verification completed for $phoneNumber")
            onVerificationCompleted(credential)
          }

          override fun onVerificationFailed(e: FirebaseException) {
            Log.e(TAG, "Phone verification failed for $phoneNumber", e)
            val msg = when {
              e.message?.contains("BILLING_NOT_ENABLED", ignoreCase = true) == true ->
                "SMS delivery quota exceeded or billing not enabled on Firebase. Please enable Phone Authentication in Firebase Console."
              e.message?.contains("TOO_LONG", ignoreCase = true) == true || e.message?.contains("TOO_SHORT", ignoreCase = true) == true ->
                "Invalid phone number length. Please include correct country code (e.g. +91)."
              e.message?.contains("format", ignoreCase = true) == true ->
                "Invalid phone number format. Please provide E.164 format, e.g., +919876543210."
              e.message?.contains("quota", ignoreCase = true) == true ->
                "SMS quota exceeded. Please try again later or use Google Sign-In."
              else -> e.localizedMessage ?: "Failed to send SMS code. Please try again."
            }
            onError(msg)
          }

          override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
          ) {
            Log.i(TAG, "Phone SMS 6-digit code dispatched to $phoneNumber")
            onCodeSent(verificationId, token)
          }
        })

      if (resendToken != null) {
        optionsBuilder.setForceResendingToken(resendToken)
      }

      PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    } catch (e: Throwable) {
      Log.e(TAG, "sendPhoneOtp error", e)
      onError(e.localizedMessage ?: "Failed to initiate phone verification.")
    }
  }

  suspend fun signInWithPhoneOtp(verificationId: String, otpCode: String): Result<FirebaseUser> {
    return try {
      val credential = PhoneAuthProvider.getCredential(verificationId, otpCode.trim())
      signInWithPhoneCredential(credential)
    } catch (e: Throwable) {
      Log.e(TAG, "signInWithPhoneOtp error", e)
      Result.failure(e)
    }
  }

  suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): Result<FirebaseUser> {
    return try {
      val authResult = auth.signInWithCredential(credential).awaitResult()
      val user = authResult.user ?: throw IllegalStateException("FirebaseUser is null after phone sign-in")

      val firestore = getFirestore()
      val userDoc = firestore.collection("users").document(user.uid).get().awaitResult()

      if (!userDoc.exists()) {
        val phone = user.phoneNumber ?: ""
        val safeSuffix = if (phone.length >= 4) phone.takeLast(4) else user.uid.take(4)
        val initialProfile = hashMapOf<String, Any>(
          "uid" to user.uid,
          "phoneNumber" to phone,
          "email" to (user.email ?: ""),
          "displayName" to (user.displayName?.takeIf { it.isNotBlank() } ?: "Learner"),
          "photoUrl" to (user.photoUrl?.toString() ?: ""),
          "username" to "learner_$safeSuffix",
          "isProfileCompleted" to false,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("users").document(user.uid).set(initialProfile, SetOptions.merge()).awaitResult()
      }

      Result.success(user)
    } catch (e: Throwable) {
      Log.e(TAG, "signInWithPhoneCredential error", e)
      Result.failure(e)
    }
  }

  // --- Email & Password Sign Up ---
  suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<FirebaseUser> {
    return try {
      val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).awaitResult()
      val user = authResult.user ?: throw IllegalStateException("FirebaseUser is null after sign up")

      try {
        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
          .setDisplayName(displayName.trim())
          .build()
        user.updateProfile(profileUpdates).awaitResult()
      } catch (e: Exception) {
        Log.w(TAG, "Failed to update display name on FirebaseUser", e)
      }

      val firestore = getFirestore()
      val baseUsername = email.substringBefore("@").replace(".", "_").lowercase()
      val initialProfile = hashMapOf<String, Any>(
        "uid" to user.uid,
        "email" to email.trim(),
        "displayName" to displayName.trim(),
        "photoUrl" to "",
        "username" to baseUsername,
        "isProfileCompleted" to false,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      firestore.collection("users").document(user.uid).set(initialProfile, SetOptions.merge()).awaitResult()

      // Automatically send verification email immediately upon sign up
      try {
        user.sendEmailVerification().awaitResult()
        Log.i(TAG, "Initial verification email dispatched to ${user.email}")
      } catch (e: Exception) {
        Log.w(TAG, "Failed to send initial verification email", e)
      }

      Result.success(user)
    } catch (e: Throwable) {
      Log.e(TAG, "signUpWithEmail error", e)
      Result.failure(e)
    }
  }

  // --- Send Email Verification ---
  suspend fun sendEmailVerification(): Result<Unit> {
    val user = auth.currentUser ?: return Result.failure(IllegalStateException("No authenticated user"))
    return try {
      user.sendEmailVerification().awaitResult()
      Log.i(TAG, "Verification email sent to ${user.email}")
      Result.success(Unit)
    } catch (e: Throwable) {
      Log.e(TAG, "sendEmailVerification error", e)
      Result.failure(e)
    }
  }

  // --- Reload current user and check if email is verified ---
  suspend fun reloadAndCheckEmailVerified(): Result<Boolean> {
    val user = auth.currentUser ?: return Result.failure(IllegalStateException("No authenticated user"))
    return try {
      user.reload().awaitResult()
      val isVerified = auth.currentUser?.isEmailVerified == true
      Result.success(isVerified)
    } catch (e: Throwable) {
      Log.e(TAG, "reloadAndCheckEmailVerified error", e)
      Result.failure(e)
    }
  }

  // --- Email & Password Sign In ---
  suspend fun signInWithEmail(email: String, password: String): Result<Pair<FirebaseUser, Boolean>> {
    return try {
      val authResult = auth.signInWithEmailAndPassword(email.trim(), password).awaitResult()
      val user = authResult.user ?: throw IllegalStateException("FirebaseUser is null after sign in")
      try {
        user.reload().awaitResult()
      } catch (e: Exception) {
        Log.w(TAG, "User reload failed during sign in", e)
      }
      val isVerified = user.isEmailVerified
      Result.success(Pair(user, isVerified))
    } catch (e: Throwable) {
      Log.e(TAG, "signInWithEmail error", e)
      Result.failure(e)
    }
  }

  // Check if a user is verified (Google and Phone accounts are automatically verified)
  fun isUserVerified(user: FirebaseUser?): Boolean {
    if (user == null) return false
    if (user.providerData.any { it.providerId == "google.com" }) return true
    if (user.providerData.any { it.providerId == "phone" } || !user.phoneNumber.isNullOrBlank()) return true
    return user.isEmailVerified
  }

  // --- Send Password Reset Email ---
  suspend fun sendPasswordReset(email: String): Result<Unit> {
    return try {
      auth.sendPasswordResetEmail(email.trim()).awaitResult()
      Result.success(Unit)
    } catch (e: Throwable) {
      Log.e(TAG, "sendPasswordReset error", e)
      Result.failure(e)
    }
  }

  // --- Check if User Profile is completed ---
  suspend fun checkIsProfileCompleted(uid: String): Boolean {
    return try {
      val firestore = getFirestore()
      val doc = firestore.collection("users").document(uid).get().awaitResult()
      doc.getBoolean("isProfileCompleted") ?: false
    } catch (e: Exception) {
      false
    }
  }

  // --- Check username availability ---
  suspend fun checkUsernameAvailable(username: String): Boolean {
    return try {
      val firestore = getFirestore()
      val snapshot = firestore.collection("users")
        .whereEqualTo("username", username.trim().lowercase())
        .limit(2)
        .get()
        .awaitResult()

      val currentUid = auth.currentUser?.uid
      if (snapshot.isEmpty) {
        true
      } else {
        // Available if the only match is the current user
        snapshot.documents.all { it.id == currentUid }
      }
    } catch (e: Exception) {
      Log.w(TAG, "checkUsernameAvailable failed, assuming available", e)
      true
    }
  }

  // --- Save / Update User Profile ---
  suspend fun saveUserProfile(
    displayName: String,
    username: String,
    dateOfBirth: String,
    educationalLevel: String,
    courseOrDegree: String,
    institution: String = "",
    fieldOfStudy: String = "",
    photoUrl: String = "",
    bio: String = "",
    country: String = "",
    state: String = "",
    connectionType: String = "",
    connectionDetail: String = "",
    isCompleted: Boolean = true
  ): Result<Unit> {
    val user = auth.currentUser ?: return Result.failure(IllegalStateException("User not authenticated"))
    val firestore = getFirestore()

    return try {
      val data = hashMapOf<String, Any>(
        "uid" to user.uid,
        "displayName" to displayName.trim(),
        "username" to username.trim().lowercase(),
        "dateOfBirth" to dateOfBirth,
        "educationLevel" to educationalLevel,
        "course" to courseOrDegree,
        "institution" to institution,
        "fieldOfStudy" to fieldOfStudy,
        "bio" to bio,
        "country" to country.trim(),
        "state" to state.trim(),
        "connectionType" to connectionType.trim(),
        "connectionDetail" to connectionDetail.trim(),
        "isProfileCompleted" to isCompleted,
        "isUsernameLocked" to true,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (photoUrl.isNotBlank()) {
        data["photoUrl"] = photoUrl
      }

      firestore.collection("users").document(user.uid)
        .set(data, SetOptions.merge())
        .awaitResult()

      Result.success(Unit)
    } catch (e: Throwable) {
      Log.e(TAG, "saveUserProfile failed", e)
      Result.failure(e)
    }
  }

  // --- Cloud Storage: Upload PDF ---
  suspend fun uploadPdf(
    userId: String,
    resourceId: String,
    pdfBytes: ByteArray,
    fileName: String = "document.pdf",
    onProgress: (Int) -> Unit = {}
  ): Result<Pair<String, String>> = suspendCancellableCoroutine { continuation ->
    val cleanName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"
    val storagePath = "resources/$userId/$resourceId/$cleanName"
    val ref = storage.reference.child(storagePath)
    val metadata = StorageMetadata.Builder()
      .setContentType("application/pdf")
      .setCustomMetadata("uploadedBy", userId)
      .setCustomMetadata("resourceId", resourceId)
      .build()

    val uploadTask = ref.putBytes(pdfBytes, metadata)

    uploadTask.addOnProgressListener { taskSnapshot ->
      val total = taskSnapshot.totalByteCount
      if (total > 0) {
        val percent = ((taskSnapshot.bytesTransferred * 100) / total).toInt()
        onProgress(percent.coerceIn(0, 100))
      }
    }

    uploadTask.addOnSuccessListener {
      ref.downloadUrl.addOnSuccessListener { uri ->
        Log.i(TAG, "PDF successfully uploaded to Firebase Storage: $storagePath")
        continuation.resume(Result.success(Pair(uri.toString(), storagePath)))
      }.addOnFailureListener { ex ->
        Log.e(TAG, "Failed to get download URL for PDF", ex)
        continuation.resume(Result.failure(ex))
      }
    }.addOnFailureListener { ex ->
      Log.e(TAG, "Firebase Storage upload failed for PDF: $storagePath", ex)
      continuation.resume(Result.failure(ex))
    }

    continuation.invokeOnCancellation {
      uploadTask.cancel()
    }
  }

  // --- Cloud Storage: Upload Cover Image ---
  suspend fun uploadCoverImage(
    userId: String,
    resourceId: String,
    imageBytes: ByteArray,
    mimeType: String = "image/jpeg",
    onProgress: (Int) -> Unit = {}
  ): Result<Pair<String, String>> = suspendCancellableCoroutine { continuation ->
    val storagePath = "covers/$userId/$resourceId/cover.jpg"
    val ref = storage.reference.child(storagePath)
    val metadata = StorageMetadata.Builder()
      .setContentType(mimeType)
      .setCustomMetadata("uploadedBy", userId)
      .setCustomMetadata("resourceId", resourceId)
      .build()

    val uploadTask = ref.putBytes(imageBytes, metadata)

    uploadTask.addOnProgressListener { taskSnapshot ->
      val total = taskSnapshot.totalByteCount
      if (total > 0) {
        val percent = ((taskSnapshot.bytesTransferred * 100) / total).toInt()
        onProgress(percent.coerceIn(0, 100))
      }
    }

    uploadTask.addOnSuccessListener {
      ref.downloadUrl.addOnSuccessListener { uri ->
        Log.i(TAG, "Cover image successfully uploaded to Firebase Storage: $storagePath")
        continuation.resume(Result.success(Pair(uri.toString(), storagePath)))
      }.addOnFailureListener { ex ->
        Log.e(TAG, "Failed to get download URL for cover", ex)
        continuation.resume(Result.failure(ex))
      }
    }.addOnFailureListener { ex ->
      Log.e(TAG, "Firebase Storage upload failed for cover: $storagePath", ex)
      continuation.resume(Result.failure(ex))
    }

    continuation.invokeOnCancellation {
      uploadTask.cancel()
    }
  }

  // --- Cloud Storage: Upload Profile Avatar ---
  suspend fun uploadAvatarImage(
    userId: String,
    imageBytes: ByteArray,
    mimeType: String = "image/jpeg",
    onProgress: (Int) -> Unit = {}
  ): Result<Pair<String, String>> = suspendCancellableCoroutine { continuation ->
    val storagePath = "avatars/$userId/avatar.jpg"
    val ref = storage.reference.child(storagePath)
    val metadata = StorageMetadata.Builder()
      .setContentType(mimeType)
      .setCustomMetadata("uploadedBy", userId)
      .build()

    val uploadTask = ref.putBytes(imageBytes, metadata)

    uploadTask.addOnProgressListener { taskSnapshot ->
      val total = taskSnapshot.totalByteCount
      if (total > 0) {
        val percent = ((taskSnapshot.bytesTransferred * 100) / total).toInt()
        onProgress(percent.coerceIn(0, 100))
      }
    }

    uploadTask.addOnSuccessListener {
      ref.downloadUrl.addOnSuccessListener { uri ->
        Log.i(TAG, "Avatar successfully uploaded to Firebase Storage: $storagePath")
        continuation.resume(Result.success(Pair(uri.toString(), storagePath)))
      }.addOnFailureListener { ex ->
        Log.e(TAG, "Failed to get download URL for avatar", ex)
        continuation.resume(Result.failure(ex))
      }
    }.addOnFailureListener { ex ->
      Log.e(TAG, "Firebase Storage upload failed for avatar: $storagePath", ex)
      continuation.resume(Result.failure(ex))
    }

    continuation.invokeOnCancellation {
      uploadTask.cancel()
    }
  }

  // --- Publish Resource ---
  suspend fun publishResource(
    title: String,
    description: String,
    category: ResourceType,
    subject: String,
    topic: String,
    educationalLevel: String,
    coverUrl: String = "",
    pdfUrl: String = "",
    fileSize: String = "2.4 MB",
    pdfStoragePath: String = "",
    coverStoragePath: String = "",
    fileName: String = "document.pdf",
    customResourceId: String? = null
  ): Result<String> {
    val user = auth.currentUser ?: return Result.failure(IllegalStateException("User not authenticated"))
    val currentProfile = _userProfile.value
    val firestore = getFirestore()

    return try {
      val docRef = if (!customResourceId.isNullOrBlank()) {
        firestore.collection("resources").document(customResourceId)
      } else {
        firestore.collection("resources").document()
      }
      val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
      val data = hashMapOf<String, Any>(
        "id" to docRef.id,
        "title" to title.trim(),
        "description" to description.trim(),
        "category" to category.name,
        "subject" to subject,
        "topic" to topic,
        "educationalLevel" to educationalLevel,
        "authorId" to user.uid,
        "authorName" to (currentProfile?.displayName ?: user.displayName ?: "Learner"),
        "authorUsername" to (currentProfile?.username ?: "learner"),
        "authorAvatar" to (currentProfile?.avatarUrl ?: user.photoUrl?.toString() ?: ""),
        "coverUrl" to coverUrl,
        "coverStoragePath" to coverStoragePath,
        "pdfUrl" to pdfUrl,
        "pdfStoragePath" to pdfStoragePath,
        "fileName" to fileName,
        "fileSize" to fileSize,
        "publicationDate" to sdf.format(Date()),
        "uploadTimestamp" to System.currentTimeMillis(),
        "viewsCount" to 0,
        "downloadsCount" to 0,
        "likesCount" to 0,
        "averageRating" to 0.0f,
        "ratingsCount" to 0,
        "commentsCount" to 0,
        "createdAt" to FieldValue.serverTimestamp()
      )

      docRef.set(data).awaitResult()
      Result.success(docRef.id)
    } catch (e: Throwable) {
      Log.e(TAG, "publishResource failed", e)
      Result.failure(e)
    }
  }

  // --- Add Comment to Resource ---
  suspend fun addResourceComment(resourceId: String, commentText: String): Result<String> {
    val user = auth.currentUser ?: return Result.failure(IllegalStateException("User not authenticated"))
    val currentProfile = _userProfile.value
    val firestore = getFirestore()

    return try {
      val docRef = firestore.collection("resources").document(resourceId)
        .collection("comments").document()
      val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
      val data = hashMapOf<String, Any>(
        "id" to docRef.id,
        "resourceId" to resourceId,
        "authorId" to user.uid,
        "authorName" to (currentProfile?.displayName ?: user.displayName ?: "Learner"),
        "authorAvatar" to (currentProfile?.avatarUrl ?: user.photoUrl?.toString() ?: ""),
        "content" to commentText.trim(),
        "formattedDate" to sdf.format(Date()),
        "createdAt" to FieldValue.serverTimestamp()
      )

      docRef.set(data).awaitResult()

      firestore.collection("resources").document(resourceId)
        .update("commentsCount", FieldValue.increment(1))

      Result.success(docRef.id)
    } catch (e: Throwable) {
      Log.e(TAG, "addResourceComment failed", e)
      Result.failure(e)
    }
  }

  // --- Submit Resource Rating (1 to 5 stars) ---
  suspend fun submitResourceRating(resourceId: String, rating: Int): Result<Unit> {
    val user = auth.currentUser ?: return Result.failure(IllegalStateException("User not authenticated"))
    val firestore = getFirestore()

    return try {
      val ratingDoc = firestore.collection("resources").document(resourceId)
        .collection("ratings").document(user.uid)
      val data = hashMapOf<String, Any>(
        "userId" to user.uid,
        "rating" to rating.coerceIn(1, 5),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      ratingDoc.set(data, SetOptions.merge()).awaitResult()

      val snapshot = firestore.collection("resources").document(resourceId)
        .collection("ratings").get().awaitResult()
      val allRatings = snapshot.documents.mapNotNull { (it.data?.get("rating") as? Number)?.toDouble() }
      val avg = if (allRatings.isNotEmpty()) ((allRatings.average() * 10.0).let { Math.round(it) / 10.0 }).toFloat() else rating.toFloat()
      val count = allRatings.size

      firestore.collection("resources").document(resourceId)
        .update(
          mapOf(
            "averageRating" to avg,
            "ratingsCount" to count
          )
        ).awaitResult()

      Result.success(Unit)
    } catch (e: Throwable) {
      Log.e(TAG, "submitResourceRating failed", e)
      Result.failure(e)
    }
  }

  // --- Increment Download Count ---
  suspend fun incrementDownloadCount(resourceId: String) {
    try {
      getFirestore().collection("resources").document(resourceId)
        .update("downloadsCount", FieldValue.increment(1))
    } catch (e: Exception) {
      Log.w(TAG, "incrementDownloadCount failed", e)
    }
  }

  // --- Publish Thought ---
  suspend fun publishThought(content: String): Result<String> {
    val user = auth.currentUser ?: return Result.failure(IllegalStateException("User not authenticated"))
    val currentProfile = _userProfile.value
    val firestore = getFirestore()

    return try {
      val docRef = firestore.collection("thoughts").document()
      val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
      val data = hashMapOf<String, Any>(
        "id" to docRef.id,
        "content" to content.trim(),
        "authorId" to user.uid,
        "authorName" to (currentProfile?.displayName ?: user.displayName ?: "Learner"),
        "authorUsername" to (currentProfile?.username ?: "learner"),
        "authorAvatar" to (currentProfile?.avatarUrl ?: user.photoUrl?.toString() ?: ""),
        "publicationDate" to sdf.format(Date()),
        "likesCount" to 0,
        "commentsCount" to 0,
        "createdAt" to FieldValue.serverTimestamp()
      )

      docRef.set(data).awaitResult()
      Result.success(docRef.id)
    } catch (e: Throwable) {
      Log.e(TAG, "publishThought failed", e)
      Result.failure(e)
    }
  }

  // --- Add Comment to Thought ---
  suspend fun addComment(thoughtId: String, commentText: String): Result<String> {
    val user = auth.currentUser ?: return Result.failure(IllegalStateException("User not authenticated"))
    val currentProfile = _userProfile.value
    val firestore = getFirestore()

    return try {
      val docRef = firestore.collection("thoughts").document(thoughtId)
        .collection("comments").document()
      val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
      val data = hashMapOf<String, Any>(
        "id" to docRef.id,
        "thoughtId" to thoughtId,
        "authorId" to user.uid,
        "authorName" to (currentProfile?.displayName ?: user.displayName ?: "Learner"),
        "authorAvatar" to (currentProfile?.avatarUrl ?: user.photoUrl?.toString() ?: ""),
        "content" to commentText.trim(),
        "formattedDate" to sdf.format(Date()),
        "createdAt" to FieldValue.serverTimestamp()
      )

      docRef.set(data).awaitResult()

      // Increment commentsCount on thought
      firestore.collection("thoughts").document(thoughtId)
        .update("commentsCount", FieldValue.increment(1))

      Result.success(docRef.id)
    } catch (e: Throwable) {
      Log.e(TAG, "addComment failed", e)
      Result.failure(e)
    }
  }

  // --- Live Comments Listeners ---
  fun listenToResourceComments(resourceId: String, onUpdate: (List<CommentItem>) -> Unit): ListenerRegistration {
    val firestore = getFirestore()
    return firestore.collection("resources").document(resourceId)
      .collection("comments")
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.w(TAG, "listenToResourceComments error", error)
          return@addSnapshotListener
        }
        if (snapshot != null) {
          val comments = snapshot.documents.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            val time = (d["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
            CommentItem(
              id = doc.id,
              targetId = resourceId,
              userId = d["authorId"] as? String ?: "",
              username = (d["authorName"] as? String)?.replace(" ", "_")?.lowercase() ?: "learner",
              userDisplayName = d["authorName"] as? String ?: "Learner",
              userAvatarUrl = d["authorAvatar"] as? String ?: "",
              commentText = d["content"] as? String ?: "",
              timestamp = time,
              formattedDate = d["formattedDate"] as? String ?: "Recent"
            )
          }.sortedByDescending { it.timestamp }
          onUpdate(comments)
        }
      }
  }

  fun listenToThoughtComments(thoughtId: String, onUpdate: (List<CommentItem>) -> Unit): ListenerRegistration {
    val firestore = getFirestore()
    return firestore.collection("thoughts").document(thoughtId)
      .collection("comments")
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.w(TAG, "listenToThoughtComments error", error)
          return@addSnapshotListener
        }
        if (snapshot != null) {
          val comments = snapshot.documents.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            val time = (d["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
            CommentItem(
              id = doc.id,
              targetId = thoughtId,
              userId = d["authorId"] as? String ?: "",
              username = (d["authorName"] as? String)?.replace(" ", "_")?.lowercase() ?: "learner",
              userDisplayName = d["authorName"] as? String ?: "Learner",
              userAvatarUrl = d["authorAvatar"] as? String ?: "",
              commentText = d["content"] as? String ?: "",
              timestamp = time,
              formattedDate = d["formattedDate"] as? String ?: "Recent"
            )
          }.sortedByDescending { it.timestamp }
          onUpdate(comments)
        }
      }
  }

  // --- Sign Out ---
  fun signOut() {
    auth.signOut()
    detachUserListeners()
    _currentUser.value = null
    _userProfile.value = null
  }
}

// Coroutine Task extension helper
suspend fun <T> Task<T>.awaitResult(): T =
  suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
      continuation.resume(result)
    }
    addOnFailureListener { exception ->
      continuation.resumeWith(Result.failure(exception))
    }
  }
