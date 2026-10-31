package com.example.data.repository

import android.content.Context
import com.example.data.model.*
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseUserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object LearnerportRepository {
  private const val THEME_PREFS = "learnerport_theme_prefs"
  private const val KEY_THEME_MODE = "saved_theme_mode"

  private val _currentUser = MutableStateFlow(
    UserProfile(
      id = "",
      username = "",
      displayName = "",
      isCurrentUser = true,
      isProfileCompleted = false
    )
  )

  private val _isUserAuthenticated = MutableStateFlow(false)
  val isUserAuthenticated: StateFlow<Boolean> = _isUserAuthenticated.asStateFlow()

  // Real resources and thoughts (Starts clean; empty states shown when empty)
  private val resourcesList = MutableStateFlow<List<ResourceItem>>(emptyList())
  private val thoughtsList = MutableStateFlow<List<ThoughtItem>>(emptyList())
  private val creatorsList = MutableStateFlow<List<UserProfile>>(emptyList())
  private val followersList = MutableStateFlow<List<UserProfile>>(emptyList())
  private val followingList = MutableStateFlow<List<UserProfile>>(emptyList())

  private val commentsList = MutableStateFlow<List<CommentItem>>(emptyList())
  private val notificationsList = MutableStateFlow<List<NotificationItem>>(emptyList())

  private val themeMode = MutableStateFlow(ThemeMode.LIGHT)
  private var appContext: Context? = null
  private val activeCommentListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()

  fun init(context: Context) {
    appContext = context.applicationContext
    // 1. Load Theme: ALWAYS default to Light Mode unless user explicitly saved Dark Mode
    val themePrefs = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
    val savedTheme = themePrefs.getString(KEY_THEME_MODE, ThemeMode.LIGHT.name)
    themeMode.value = try {
      ThemeMode.valueOf(savedTheme ?: ThemeMode.LIGHT.name)
    } catch (e: Exception) {
      ThemeMode.LIGHT
    }

    // 2. Initialize Firebase Auth & Firestore
    com.example.data.remote.FirebaseManager.init(context)

    // Reactive observation of Firebase user session
    CoroutineScope(Dispatchers.Main).launch {
      com.example.data.remote.FirebaseManager.currentUser.collect { firebaseUser ->
        if (firebaseUser != null) {
          val isVerified = com.example.data.remote.FirebaseManager.isUserVerified(firebaseUser)
          if (isVerified) {
            _isUserAuthenticated.value = true
            val p = com.example.data.remote.FirebaseManager.userProfile.value
            if (p != null) {
              _currentUser.value = p
            } else {
              _currentUser.value = UserProfile(
                id = firebaseUser.uid,
                username = firebaseUser.email?.substringBefore("@") ?: "learner",
                displayName = firebaseUser.displayName ?: "Learner",
                avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
                isCurrentUser = true,
                isProfileCompleted = false
              )
            }
          } else {
            // Unverified email account: require verification before entering app
            _isUserAuthenticated.value = false
          }
        } else {
          val session = SupabaseClient.currentSession.value
          if (session != null && session.userId.isNotBlank()) {
            _isUserAuthenticated.value = true
          } else {
            _isUserAuthenticated.value = false
          }
        }
      }
    }

    CoroutineScope(Dispatchers.Main).launch {
      com.example.data.remote.FirebaseManager.userProfile.collect { profile ->
        if (profile != null) {
          _currentUser.value = profile
        }
      }
    }

    CoroutineScope(Dispatchers.Main).launch {
      com.example.data.remote.FirebaseManager.resources.collect { items ->
        if (items.isNotEmpty()) {
          resourcesList.value = items
        }
      }
    }

    CoroutineScope(Dispatchers.Main).launch {
      com.example.data.remote.FirebaseManager.thoughts.collect { items ->
        if (items.isNotEmpty()) {
          thoughtsList.value = items
        }
      }
    }

    // 3. Load Supabase Auth Session fallback
    SupabaseClient.init(context)
    val session = SupabaseClient.currentSession.value
    if (session != null && session.userId.isNotBlank() && !_isUserAuthenticated.value) {
      _isUserAuthenticated.value = true
      _currentUser.value = UserProfile(
        id = session.userId,
        username = session.username,
        displayName = session.displayName,
        avatarUrl = session.avatarUrl,
        bio = session.bio,
        dateOfBirth = session.dateOfBirth,
        educationalLevel = session.educationalLevel,
        courseOrDegree = session.courseOrDegree,
        fieldOfStudy = session.fieldOfStudy,
        isProfileCompleted = session.isProfileCompleted,
        isCurrentUser = true
      )
    }

    syncWithSupabaseIfConfigured()
  }

  // Exposed Flows
  fun getCurrentUser(): Flow<UserProfile> = _currentUser.asStateFlow()
  fun getThemeMode(): Flow<ThemeMode> = themeMode.asStateFlow()

  fun setThemeMode(context: Context?, mode: ThemeMode) {
    themeMode.value = mode
    if (context != null) {
      context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE).edit()
        .putString(KEY_THEME_MODE, mode.name)
        .apply()
    }
  }

  fun getAllResources(): Flow<List<ResourceItem>> = resourcesList.asStateFlow()
  fun getAllThoughts(): Flow<List<ThoughtItem>> = thoughtsList.asStateFlow()
  fun getAllCreators(): Flow<List<UserProfile>> = creatorsList.asStateFlow()
  fun getFollowers(): Flow<List<UserProfile>> = followersList.asStateFlow()
  fun getFollowing(): Flow<List<UserProfile>> = followingList.asStateFlow()
  fun getNotifications(): Flow<List<NotificationItem>> = notificationsList.asStateFlow()

  fun getResourceById(id: String): ResourceItem? {
    return resourcesList.value.firstOrNull { it.id == id }
  }

  fun getCommentsFor(targetId: String): Flow<List<CommentItem>> {
    if (!activeCommentListeners.containsKey(targetId)) {
      val isResource = resourcesList.value.any { it.id == targetId }
      val reg = if (isResource) {
        com.example.data.remote.FirebaseManager.listenToResourceComments(targetId) { remoteComments ->
          mergeComments(targetId, remoteComments)
        }
      } else {
        com.example.data.remote.FirebaseManager.listenToThoughtComments(targetId) { remoteComments ->
          mergeComments(targetId, remoteComments)
        }
      }
      activeCommentListeners[targetId] = reg
    }
    return commentsList.map { list -> list.filter { it.targetId == targetId }.sortedByDescending { it.timestamp } }
  }

  private fun mergeComments(targetId: String, remoteComments: List<CommentItem>) {
    val existingOther = commentsList.value.filter { it.targetId != targetId }
    val existingMine = commentsList.value.filter { it.targetId == targetId }
    val combinedMap = mutableMapOf<String, CommentItem>()
    remoteComments.forEach { combinedMap[it.id] = it }
    existingMine.forEach { local ->
      if (!combinedMap.containsKey(local.id)) {
        combinedMap[local.id] = local
      }
    }
    commentsList.value = existingOther + combinedMap.values.toList()
  }

  fun toggleSaveResource(resourceId: String) {
    resourcesList.value = resourcesList.value.map { item ->
      if (item.id == resourceId) item.copy(isSaved = !item.isSaved) else item
    }
  }

  fun toggleSaveThought(thoughtId: String) {
    thoughtsList.value = thoughtsList.value.map { item ->
      if (item.id == thoughtId) item.copy(isSaved = !item.isSaved) else item
    }
  }

  fun toggleLikeThought(thoughtId: String) {
    thoughtsList.value = thoughtsList.value.map { item ->
      if (item.id == thoughtId) {
        val newLiked = !item.isLiked
        val newCount = if (newLiked) item.likesCount + 1 else (item.likesCount - 1).coerceAtLeast(0)
        item.copy(isLiked = newLiked, likesCount = newCount)
      } else item
    }
  }

  fun recordDownload(
    resourceId: String,
    onProgress: (Int) -> Unit = {},
    onComplete: (Boolean, String?) -> Unit = { _, _ -> }
  ) {
    val ctx = appContext ?: run {
      onComplete(false, "Context not available")
      return
    }
    val target = resourcesList.value.firstOrNull { it.id == resourceId } ?: run {
      onComplete(false, "Resource not found")
      return
    }

    // Immediately update in-memory state so UI and unit tests reflect the download action synchronously
    resourcesList.value = resourcesList.value.map { item ->
      if (item.id == resourceId) {
        item.copy(
          downloadsCount = item.downloadsCount + 1,
          isDownloaded = true
        )
      } else item
    }

    CoroutineScope(Dispatchers.IO).launch {
      val res = com.example.data.pdf.PdfFileManager.downloadPdfWithProgress(ctx, target) { p ->
        onProgress(p)
      }

      res.onSuccess { downloadedFile ->
        com.example.data.remote.FirebaseManager.incrementDownloadCount(resourceId)

        resourcesList.value = resourcesList.value.map { item ->
          if (item.id == resourceId) {
            item.copy(
              localFilePath = downloadedFile.absolutePath
            )
          } else item
        }
        onComplete(true, null)
      }.onFailure { err ->
        android.util.Log.e("LearnerportRepo", "Failed to download PDF offline", err)
        onComplete(false, err.localizedMessage ?: "Download failed")
      }
    }
  }

  fun recordView(resourceId: String) {
    resourcesList.value = resourcesList.value.map { item ->
      if (item.id == resourceId) item.copy(viewsCount = item.viewsCount + 1) else item
    }
  }

  fun submitRating(resourceId: String, stars: Int) {
    resourcesList.value = resourcesList.value.map { item ->
      if (item.id == resourceId) {
        val hadPreviousRating = item.userRating > 0
        val newRatingsCount = if (hadPreviousRating) item.ratingsCount else item.ratingsCount + 1
        val currentTotal = item.averageRating * item.ratingsCount
        val newTotal = if (hadPreviousRating) currentTotal - item.userRating + stars else currentTotal + stars
        val newAvg = if (newRatingsCount > 0) newTotal / newRatingsCount else stars.toFloat()
        item.copy(
          userRating = stars,
          ratingsCount = newRatingsCount,
          averageRating = (Math.round(newAvg * 10.0) / 10.0).toFloat()
        )
      } else item
    }

    CoroutineScope(Dispatchers.IO).launch {
      com.example.data.remote.FirebaseManager.submitResourceRating(resourceId, stars)
    }
  }

  fun addComment(targetId: String, commentText: String) {
    val user = _currentUser.value
    val newComment = CommentItem(
      id = "comment_" + System.currentTimeMillis(),
      targetId = targetId,
      userId = user.id,
      username = user.username,
      userDisplayName = user.displayName,
      userAvatarUrl = user.avatarUrl,
      commentText = commentText.trim(),
      timestamp = System.currentTimeMillis(),
      formattedDate = "Just now"
    )
    commentsList.value = listOf(newComment) + commentsList.value

    val isResource = resourcesList.value.any { it.id == targetId }
    if (isResource) {
      resourcesList.value = resourcesList.value.map {
        if (it.id == targetId) it.copy(commentsCount = it.commentsCount + 1) else it
      }
      CoroutineScope(Dispatchers.IO).launch {
        com.example.data.remote.FirebaseManager.addResourceComment(targetId, commentText)
      }
    } else {
      thoughtsList.value = thoughtsList.value.map {
        if (it.id == targetId) it.copy(commentsCount = it.commentsCount + 1) else it
      }
      CoroutineScope(Dispatchers.IO).launch {
        com.example.data.remote.FirebaseManager.addComment(targetId, commentText)
      }
    }
  }

  fun toggleFollowUser(userId: String) {
    val currentUser = _currentUser.value
    if (userId == currentUser.id) return // Cannot follow self

    val creators = creatorsList.value.map { user ->
      if (user.id == userId) {
        val willFollow = !user.isFollowedByCurrentUser
        val newFollowers = if (willFollow) user.followersCount + 1 else (user.followersCount - 1).coerceAtLeast(0)
        user.copy(isFollowedByCurrentUser = willFollow, followersCount = newFollowers)
      } else user
    }
    creatorsList.value = creators

    val targetUser = creators.firstOrNull { it.id == userId }
    if (targetUser != null) {
      if (targetUser.isFollowedByCurrentUser) {
        if (followingList.value.none { it.id == userId }) {
          followingList.value = followingList.value + targetUser
        }
      } else {
        followingList.value = followingList.value.filterNot { it.id == userId }
      }
    }

    val totalFollowing = followingList.value.size
    _currentUser.value = currentUser.copy(followingCount = totalFollowing)
  }

  suspend fun uploadAndPublishResource(
    title: String,
    description: String,
    resourceType: ResourceType,
    subject: String,
    topic: String,
    educationalLevel: String,
    programmingLanguage: String?,
    language: String,
    tags: List<String>,
    examinationYear: String? = null,
    boardOrUniversity: String? = null,
    paperType: String? = null,
    coverUrl: String = "",
    pdfBytes: ByteArray,
    pdfFileName: String?,
    coverBytes: ByteArray?,
    fileSize: String,
    onProgress: (stage: String, percent: Int) -> Unit = { _, _ -> }
  ): Result<String> {
    val ctx = appContext
    val fbUser = com.example.data.remote.FirebaseManager.currentUser.value
    val userId = fbUser?.uid ?: _currentUser.value.id.ifBlank { "user_${System.currentTimeMillis()}" }
    val resourceId = "res_" + System.currentTimeMillis()

    // 1. Validation
    if (pdfBytes.isEmpty()) {
      return Result.failure(IllegalArgumentException("PDF file cannot be empty."))
    }
    if (pdfBytes.size > 50 * 1024 * 1024) {
      return Result.failure(IllegalArgumentException("PDF file exceeds the 50MB size limit."))
    }
    if (!com.example.data.util.ImageCompressor.isValidPdfBytes(pdfBytes)) {
      return Result.failure(IllegalArgumentException("The selected file is not a valid PDF document (missing %PDF header)."))
    }

    try {
      // 2. Upload PDF to Firebase Storage (0% - 75% progress)
      onProgress("Uploading PDF document...", 5)
      val uploadPdfRes = com.example.data.remote.FirebaseManager.uploadPdf(
        userId = userId,
        resourceId = resourceId,
        pdfBytes = pdfBytes,
        fileName = pdfFileName ?: "$title.pdf"
      ) { p ->
        val mappedProgress = (p * 0.75).toInt().coerceIn(5, 75)
        onProgress("Uploading PDF document ($p%)...", mappedProgress)
      }

      if (uploadPdfRes.isFailure) {
        val err = uploadPdfRes.exceptionOrNull()
        android.util.Log.e("LearnerportRepo", "PDF Firebase Storage upload failed", err)
        return Result.failure(err ?: java.io.IOException("Failed to upload PDF to cloud storage."))
      }

      val (cloudPdfUrl, pdfStoragePath) = uploadPdfRes.getOrThrow()
      if (cloudPdfUrl.isBlank()) {
        return Result.failure(java.io.IOException("Storage did not return a valid download URL."))
      }

      // 3. Upload Cover Image to Firebase Storage (75% - 90% progress)
      var cloudCoverUrl = coverUrl
      var coverStoragePath = ""

      if (coverBytes != null && coverBytes.isNotEmpty()) {
        onProgress("Compressing and uploading cover image...", 78)
        val compressedCover = com.example.data.util.ImageCompressor.compressCover(coverBytes)
        val uploadCoverRes = com.example.data.remote.FirebaseManager.uploadCoverImage(
          userId = userId,
          resourceId = resourceId,
          imageBytes = compressedCover
        ) { p ->
          val mappedProgress = 75 + (p * 0.15).toInt().coerceIn(0, 15)
          onProgress("Uploading cover image ($p%)...", mappedProgress)
        }

        if (uploadCoverRes.isSuccess) {
          val (cUrl, cPath) = uploadCoverRes.getOrThrow()
          cloudCoverUrl = cUrl
          coverStoragePath = cPath
        } else {
          android.util.Log.w("LearnerportRepo", "Cover upload non-fatal fallback", uploadCoverRes.exceptionOrNull())
        }
      }

      // 4. Save metadata to Firestore (90% - 100% progress)
      onProgress("Saving resource to cloud...", 92)
      val firestoreResult = com.example.data.remote.FirebaseManager.publishResource(
        title = title,
        description = description,
        category = resourceType,
        subject = subject,
        topic = topic,
        educationalLevel = educationalLevel,
        coverUrl = cloudCoverUrl,
        pdfUrl = cloudPdfUrl,
        fileSize = fileSize,
        pdfStoragePath = pdfStoragePath,
        coverStoragePath = coverStoragePath,
        fileName = pdfFileName ?: "$title.pdf",
        customResourceId = resourceId
      )

      if (firestoreResult.isFailure) {
        val err = firestoreResult.exceptionOrNull()
        android.util.Log.e("LearnerportRepo", "Firestore document creation failed", err)
        return Result.failure(err ?: java.io.IOException("Failed to save resource metadata to database."))
      }

      // 5. Cache locally for instant preview and offline availability
      var localPath: String? = null
      if (ctx != null) {
        try {
          val upFile = java.io.File(com.example.data.pdf.PdfFileManager.getUploadedDir(ctx), "$resourceId.pdf")
          upFile.writeBytes(pdfBytes)
          localPath = upFile.absolutePath
          com.example.data.pdf.PdfFileManager.markResourceIdDownloaded(ctx, resourceId, localPath)
        } catch (e: Exception) {
          android.util.Log.w("LearnerportRepo", "Local caching failed", e)
        }
      }

      // 6. Update local repository state
      val user = _currentUser.value
      val newResource = ResourceItem(
        id = resourceId,
        title = title,
        description = description,
        creatorId = user.id.ifBlank { userId },
        creatorUsername = user.username.ifBlank { "learner" },
        creatorDisplayName = user.displayName.ifBlank { "Educator" },
        creatorAvatarUrl = user.avatarUrl,
        resourceType = resourceType,
        subject = subject,
        topic = topic,
        educationalLevel = educationalLevel,
        programmingLanguage = programmingLanguage,
        language = language,
        fileFormat = "PDF",
        fileSize = fileSize,
        publicationDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date()),
        timestamp = System.currentTimeMillis(),
        viewsCount = 1,
        downloadsCount = 0,
        averageRating = 5.0f,
        ratingsCount = 1,
        commentsCount = 0,
        tags = tags,
        pagesContent = listOf(description),
        examinationYear = examinationYear,
        boardOrUniversity = boardOrUniversity,
        paperType = paperType,
        coverUrl = cloudCoverUrl,
        pdfUrl = cloudPdfUrl,
        localFilePath = localPath,
        isDownloaded = true
      )

      resourcesList.value = listOf(newResource) + resourcesList.value
      _currentUser.value = user.copy(publishedCount = user.publishedCount + 1)

      onProgress("Published successfully!", 100)
      return Result.success(resourceId)
    } catch (e: Throwable) {
      android.util.Log.e("LearnerportRepo", "uploadAndPublishResource exception", e)
      return Result.failure(e)
    }
  }

  fun publishResource(
    title: String,
    description: String,
    resourceType: ResourceType,
    subject: String,
    topic: String,
    educationalLevel: String,
    programmingLanguage: String?,
    language: String,
    tags: List<String>,
    examinationYear: String? = null,
    boardOrUniversity: String? = null,
    paperType: String? = null,
    coverUrl: String = "",
    pdfBytes: ByteArray? = null,
    pdfFileName: String? = null,
    coverBytes: ByteArray? = null,
    fileSize: String = "2.8 MB"
  ) {
    val user = _currentUser.value
    val resourceId = "res_" + System.currentTimeMillis()
    val ctx = appContext

    var localPath: String? = null
    if (ctx != null && pdfBytes != null) {
      try {
        val upFile = java.io.File(com.example.data.pdf.PdfFileManager.getUploadedDir(ctx), "$resourceId.pdf")
        upFile.writeBytes(pdfBytes)
        localPath = upFile.absolutePath
        com.example.data.pdf.PdfFileManager.markResourceIdDownloaded(ctx, resourceId, localPath)
      } catch (e: Exception) {
        android.util.Log.w("LearnerportRepo", "Failed to cache local PDF", e)
      }
    }

    val newResource = ResourceItem(
      id = resourceId,
      title = title,
      description = description,
      creatorId = user.id.ifBlank { "user_me" },
      creatorUsername = user.username.ifBlank { "learner" },
      creatorDisplayName = user.displayName.ifBlank { "Educator" },
      creatorAvatarUrl = user.avatarUrl,
      resourceType = resourceType,
      subject = subject,
      topic = topic,
      educationalLevel = educationalLevel,
      programmingLanguage = programmingLanguage,
      language = language,
      fileFormat = "PDF",
      fileSize = fileSize,
      publicationDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date()),
      timestamp = System.currentTimeMillis(),
      viewsCount = 1,
      downloadsCount = 0,
      averageRating = 5.0f,
      ratingsCount = 1,
      commentsCount = 0,
      tags = tags,
      pagesContent = listOf(description),
      examinationYear = examinationYear,
      boardOrUniversity = boardOrUniversity,
      paperType = paperType,
      coverUrl = coverUrl,
      pdfUrl = "",
      localFilePath = localPath,
      isDownloaded = (localPath != null)
    )

    resourcesList.value = listOf(newResource) + resourcesList.value
    _currentUser.value = user.copy(publishedCount = user.publishedCount + 1)

    if (pdfBytes != null) {
      CoroutineScope(Dispatchers.IO).launch {
        try {
          uploadAndPublishResource(
            title, description, resourceType, subject, topic, educationalLevel,
            programmingLanguage, language, tags, examinationYear, boardOrUniversity,
            paperType, coverUrl, pdfBytes, pdfFileName, coverBytes, fileSize
          )
        } catch (e: Throwable) {
          android.util.Log.e("LearnerportRepo", "Async publish background failure", e)
        }
      }
    }
  }

  fun publishThought(title: String?, content: String, tags: List<String>) {
    val user = _currentUser.value
    val newThought = ThoughtItem(
      id = "thought_" + System.currentTimeMillis(),
      authorId = user.id.ifBlank { "user_me" },
      authorUsername = user.username.ifBlank { "learner" },
      authorDisplayName = user.displayName.ifBlank { "Learnerport Author" },
      authorAvatarUrl = user.avatarUrl,
      title = title?.ifBlank { null },
      content = content,
      tags = tags,
      publicationDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date()),
      timestamp = System.currentTimeMillis(),
      likesCount = 0,
      commentsCount = 0,
      isLiked = false,
      isSaved = false
    )

    thoughtsList.value = listOf(newThought) + thoughtsList.value

    // Save to Firestore
    CoroutineScope(Dispatchers.IO).launch {
      com.example.data.remote.FirebaseManager.publishThought(content)
    }
  }

  fun deleteResource(resourceId: String) {
    resourcesList.value = resourcesList.value.filterNot { it.id == resourceId }
    _currentUser.value = _currentUser.value.copy(
      publishedCount = (_currentUser.value.publishedCount - 1).coerceAtLeast(0)
    )
  }

  fun deleteThought(thoughtId: String) {
    thoughtsList.value = thoughtsList.value.filterNot { it.id == thoughtId }
  }

  fun updateCurrentUserProfile(
    displayName: String,
    username: String,
    bio: String,
    educationalLevel: String,
    courseOrDegree: String,
    institution: String,
    avatarUrl: String
  ): Boolean {
    val current = _currentUser.value
    val lockedUsername = if (current.isUsernameLocked || current.isProfileCompleted) current.username else username.trim().lowercase().removePrefix("@")

    _currentUser.value = current.copy(
      displayName = displayName.trim(),
      username = lockedUsername,
      bio = bio.trim(),
      educationalLevel = educationalLevel.trim(),
      courseOrDegree = courseOrDegree.trim(),
      institution = institution.trim(),
      avatarUrl = avatarUrl.ifBlank { current.avatarUrl },
      isUsernameLocked = true
    )

    // Save to Firestore
    CoroutineScope(Dispatchers.IO).launch {
      com.example.data.remote.FirebaseManager.saveUserProfile(
        displayName = displayName.trim(),
        username = lockedUsername,
        dateOfBirth = current.dateOfBirth,
        educationalLevel = educationalLevel.trim(),
        courseOrDegree = courseOrDegree.trim(),
        institution = institution.trim(),
        photoUrl = avatarUrl.ifBlank { current.avatarUrl },
        bio = bio.trim(),
        country = current.country,
        state = current.state,
        connectionType = educationalLevel.trim(),
        connectionDetail = courseOrDegree.trim(),
        isCompleted = true
      )
    }

    // Update session
    val session = SupabaseClient.currentSession.value
    if (session != null) {
      val updatedSession = session.copy(
        displayName = displayName.trim(),
        username = lockedUsername,
        bio = bio.trim(),
        educationalLevel = educationalLevel.trim(),
        courseOrDegree = courseOrDegree.trim(),
        avatarUrl = avatarUrl.ifBlank { session.avatarUrl }
      )
      SupabaseClient.saveSession(null, updatedSession)
    }

    return true
  }

  fun getUserAnalytics(): Flow<UserAnalytics> {
    return resourcesList.map { allResources ->
      val myResources = allResources.filter { it.creatorId == _currentUser.value.id }
      val totalViews = myResources.sumOf { it.viewsCount }
      val totalDownloads = myResources.sumOf { it.downloadsCount }
      val totalRatings = myResources.sumOf { it.ratingsCount }
      val avgRating = if (myResources.isNotEmpty()) {
        val totalAvg = myResources.filter { it.ratingsCount > 0 }.map { it.averageRating }
        if (totalAvg.isNotEmpty()) totalAvg.average().toFloat() else 0f
      } else 0f
      val totalComments = myResources.sumOf { it.commentsCount }

      val dist = mutableMapOf(5 to 0, 4 to 0, 3 to 0, 2 to 0, 1 to 0)
      myResources.forEach { res ->
        when (res.averageRating.toInt()) {
          5 -> dist[5] = (dist[5] ?: 0) + res.ratingsCount
          4 -> dist[4] = (dist[4] ?: 0) + res.ratingsCount
          3 -> dist[3] = (dist[3] ?: 0) + res.ratingsCount
          2 -> dist[2] = (dist[2] ?: 0) + res.ratingsCount
          1 -> dist[1] = (dist[1] ?: 0) + res.ratingsCount
        }
      }

      val myThoughts = thoughtsList.value.filter { it.authorId == _currentUser.value.id }
      val totalLikes = myThoughts.sumOf { it.likesCount }

      UserAnalytics(
        totalPublished = myResources.size + myThoughts.size,
        totalViews = totalViews,
        totalDownloads = totalDownloads,
        totalRatings = totalRatings,
        averageRating = (Math.round(avgRating * 10.0) / 10.0).toFloat(),
        totalComments = totalComments,
        totalLikes = totalLikes,
        ratingDistribution = dist
      )
    }
  }

  fun markAllNotificationsRead() {
    notificationsList.value = notificationsList.value.map { it.copy(isRead = true) }
  }

  fun markNotificationRead(id: String) {
    notificationsList.value = notificationsList.value.map {
      if (it.id == id) it.copy(isRead = true) else it
    }
  }

  fun notifyUserVerified() {
    val firebaseUser = com.example.data.remote.FirebaseManager.currentUser.value
    if (firebaseUser != null) {
      _isUserAuthenticated.value = true
    }
  }

  fun onUserSessionEstablished(session: SupabaseUserSession) {
    _isUserAuthenticated.value = true
    _currentUser.value = UserProfile(
      id = session.userId,
      username = session.username,
      displayName = session.displayName,
      avatarUrl = session.avatarUrl,
      bio = session.bio,
      dateOfBirth = session.dateOfBirth,
      educationalLevel = session.educationalLevel,
      courseOrDegree = session.courseOrDegree,
      fieldOfStudy = session.fieldOfStudy,
      isProfileCompleted = session.isProfileCompleted,
      isCurrentUser = true
    )
    syncWithSupabaseIfConfigured()
  }

  suspend fun signOut(context: Context?) {
    com.example.data.remote.FirebaseManager.signOut()
    SupabaseClient.signOut(context)
    _isUserAuthenticated.value = false
    _currentUser.value = UserProfile(
      id = "",
      username = "",
      displayName = "",
      isCurrentUser = true,
      isProfileCompleted = false
    )
  }

  fun syncWithSupabaseIfConfigured() {
    if (!SupabaseClient.isConfigured()) return

    CoroutineScope(Dispatchers.IO).launch {
      try {
        val url = "${SupabaseClient.supabaseUrl}/rest/v1/resources?select=*&order=created_at.desc&limit=50"
        val request = Request.Builder().url(url).get().build()
        val response = SupabaseClient.okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
          val jsonStr = response.body?.string() ?: "[]"
          val jsonArray = JSONArray(jsonStr)
          if (jsonArray.length() > 0) {
            val remoteResources = mutableListOf<ResourceItem>()
            for (i in 0 until jsonArray.length()) {
              val obj = jsonArray.getJSONObject(i)
              val contentTypeStr = obj.optString("content_type", "NOTES")
              val rType = try { ResourceType.valueOf(contentTypeStr) } catch (e: Exception) { ResourceType.NOTES }
              val pagesJson = obj.optJSONArray("pages_content")
              val pagesList = mutableListOf<String>()
              if (pagesJson != null) {
                for (p in 0 until pagesJson.length()) {
                  pagesList.add(pagesJson.optString(p))
                }
              }

              remoteResources.add(
                ResourceItem(
                  id = obj.optString("id", UUID.randomUUID().toString()),
                  title = obj.optString("title", "Untitled"),
                  description = obj.optString("description", ""),
                  creatorId = obj.optString("uploader_id", "user_remote"),
                  creatorUsername = "educator",
                  creatorDisplayName = "Learnerport Author",
                  resourceType = rType,
                  subject = obj.optString("subject", "General"),
                  topic = obj.optString("topic", "General"),
                  educationalLevel = obj.optString("educational_level", "College"),
                  language = obj.optString("language", "English"),
                  fileFormat = obj.optString("file_type", "PDF"),
                  fileSize = obj.optString("file_size", "2.4 MB"),
                  publicationDate = "Recent",
                  pagesContent = if (pagesList.isNotEmpty()) pagesList else listOf(obj.optString("description", ""))
                )
              )
            }
            if (remoteResources.isNotEmpty()) {
              resourcesList.value = remoteResources
            }
          }
        }
      } catch (e: Throwable) {
        // Fallback silently if offline
      }
    }
  }
}
