package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.model.*
import com.example.data.remote.FirebaseManager
import com.example.data.remote.SupabaseClient
import com.example.data.repository.LearnerportRepository
import com.example.data.security.AppLockManager
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.LearnerportTheme
import com.example.ui.viewmodel.LearnerportViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private val viewModel: LearnerportViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Supabase and Repository with application context
    SupabaseClient.init(this)
    LearnerportRepository.init(this)

    // Handle deep link if app was launched via OAuth redirect
    handleIncomingDeepLink(intent)

    setContent {
      val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
      val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
      }

      LearnerportTheme(darkTheme = isDark) {
        var showSplash by rememberSaveable { mutableStateOf(true) }

        if (showSplash) {
          // Automatic 2.5s splash screen without any skip button
          SplashScreen(
            onSplashFinished = { showSplash = false }
          )
        } else {
          val isUserAuthenticated by viewModel.isUserAuthenticated.collectAsStateWithLifecycle()
          val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
          val coroutineScope = rememberCoroutineScope()

          // App Lock state
          val isAppLockEnabled = remember { AppLockManager.isAppLockEnabled(this@MainActivity) }
          var isAppUnlocked by rememberSaveable { mutableStateOf(!isAppLockEnabled) }

          // Complete Google Sign-In with Firebase Auth & Credential Manager
          fun performGoogleSignIn(isAutoPrompt: Boolean = false) {
            coroutineScope.launch {
              val serverClientId = try {
                getString(R.string.default_web_client_id)
              } catch (e: Exception) {
                "211717339392-6pg79jftmmdbq7go6iovqqum7o7vounm.apps.googleusercontent.com"
              }

              try {
                val credentialManager = CredentialManager.create(this@MainActivity)
                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId)
                  .build()

                val request = GetCredentialRequest.Builder()
                  .addCredentialOption(signInWithGoogleOption)
                  .build()

                val result = credentialManager.getCredential(this@MainActivity, request)
                val credential = result.credential

                if (credential is CustomCredential &&
                  credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                  val googleIdCred = GoogleIdTokenCredential.createFrom(credential.data)
                  val idToken = googleIdCred.idToken

                  val authResult = FirebaseManager.signInWithGoogleCredential(idToken)
                  authResult.onSuccess { user ->
                    Toast.makeText(
                      this@MainActivity,
                      "Welcome, ${user.displayName ?: "Learner"}!",
                      Toast.LENGTH_SHORT
                    ).show()
                  }.onFailure { error ->
                    Toast.makeText(
                      this@MainActivity,
                      "Google Sign-In failed: ${error.message}",
                      Toast.LENGTH_LONG
                    ).show()
                  }
                  return@launch
                }
              } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                Log.i("MainActivity", "User dismissed Google Sign-In")
              } catch (e: Throwable) {
                if (!isAutoPrompt) {
                  Log.e("MainActivity", "Google Sign-In failed", e)
                  Toast.makeText(
                    this@MainActivity,
                    "Google Sign-In: ${e.localizedMessage ?: "Failed"}",
                    Toast.LENGTH_SHORT
                  ).show()
                } else {
                  Log.d("MainActivity", "Auto One-Tap prompt bypassed: ${e.message}")
                }
              }
            }
          }

          // 1. If user is NOT authenticated -> Show Welcome Auth Screen
          if (!isUserAuthenticated) {
            // Seamless One-Tap Google prompt when entering auth screen
            LaunchedEffect(Unit) {
              performGoogleSignIn(isAutoPrompt = true)
            }

            AuthWelcomeScreen(
              onAuthSuccess = { isNewUserOrIncompleteProfile ->
                // FirebaseManager updates state flows automatically
              },
              onContinueWithGoogle = { performGoogleSignIn(isAutoPrompt = false) }
            )
          }
          // 2. If user is authenticated BUT profile is NOT yet completed -> Show Complete Profile Screen
          else if (!currentUser.isProfileCompleted) {
            val fbUser = FirebaseManager.currentUser.collectAsStateWithLifecycle().value
            CompleteProfileScreen(
              initialDisplayName = currentUser.displayName.ifBlank { fbUser?.displayName ?: "" },
              initialEmail = fbUser?.email ?: SupabaseClient.currentSession.value?.email ?: "",
              onProfileCompleted = {
                // Profile completion merges into Firestore, automatically updating currentUser
              }
            )
          }
          // 3. If App Lock is enabled and not yet unlocked in this session -> Show App Lock PIN Screen
          else if (isAppLockEnabled && !isAppUnlocked) {
            AppLockScreen(
              onUnlocked = { isAppUnlocked = true },
              onResetRequested = {
                viewModel.signOut(this@MainActivity)
                isAppUnlocked = true
              }
            )
          }
          // 4. Main Application Interface
          else {
            val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
            val allResources by viewModel.allResources.collectAsStateWithLifecycle()
            val allThoughts by viewModel.allThoughts.collectAsStateWithLifecycle()
            val allCreators by viewModel.allCreators.collectAsStateWithLifecycle()
            val notifications by viewModel.notifications.collectAsStateWithLifecycle()
            val userAnalytics by viewModel.userAnalytics.collectAsStateWithLifecycle()

            val homeCategory by viewModel.homeCategory.collectAsStateWithLifecycle()
            val savedDownloadsTab by viewModel.savedDownloadsTab.collectAsStateWithLifecycle()

            val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
            val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
            val searchCategory by viewModel.searchCategory.collectAsStateWithLifecycle()

            val activePdfResource by viewModel.activePdfResource.collectAsStateWithLifecycle()
            val pdfCurrentPage by viewModel.pdfCurrentPage.collectAsStateWithLifecycle()
            val pdfZoom by viewModel.pdfZoom.collectAsStateWithLifecycle()

            val selectedDetailResource by viewModel.selectedDetailResource.collectAsStateWithLifecycle()
            val selectedCreator by viewModel.selectedCreator.collectAsStateWithLifecycle()
            val selectedChartResource by viewModel.selectedChartResource.collectAsStateWithLifecycle()

            val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
            val isNotificationsOpen by viewModel.isNotificationsOpen.collectAsStateWithLifecycle()
            val isEditProfileOpen by viewModel.isEditProfileOpen.collectAsStateWithLifecycle()
            val isFollowersOpen by viewModel.isFollowersOpen.collectAsStateWithLifecycle()
            val isFollowingOpen by viewModel.isFollowingOpen.collectAsStateWithLifecycle()
            val isAuthDialogOpen by viewModel.isAuthDialogOpen.collectAsStateWithLifecycle()

            val followers by viewModel.followers.collectAsStateWithLifecycle()
            val following by viewModel.following.collectAsStateWithLifecycle()

            var activeThoughtForComments by remember { mutableStateOf<ThoughtItem?>(null) }
            val unreadNotifsCount = notifications.count { !it.isRead }

            // Handle Back presses gracefully
            BackHandler(
              enabled = activePdfResource != null || selectedDetailResource != null ||
                selectedCreator != null || selectedChartResource != null ||
                isSettingsOpen || isNotificationsOpen || isEditProfileOpen ||
                isFollowersOpen || isFollowingOpen || isAuthDialogOpen ||
                activeThoughtForComments != null || isSearchActive || currentTab != BottomNavTab.HOME
            ) {
              when {
                activePdfResource != null -> viewModel.closePdfReader()
                selectedDetailResource != null -> viewModel.closeDetail()
                selectedCreator != null -> viewModel.closeCreator()
                selectedChartResource != null -> viewModel.closeChartResourceDetail()
                isEditProfileOpen -> viewModel.closeEditProfile()
                isFollowersOpen -> viewModel.closeFollowers()
                isFollowingOpen -> viewModel.closeFollowing()
                isAuthDialogOpen -> viewModel.closeAuthDialog()
                activeThoughtForComments != null -> activeThoughtForComments = null
                isSettingsOpen -> viewModel.closeSettings()
                isNotificationsOpen -> viewModel.closeNotifications()
                isSearchActive -> viewModel.closeSearch()
                currentTab != BottomNavTab.HOME -> viewModel.selectTab(BottomNavTab.HOME)
              }
            }

            Scaffold(
              modifier = Modifier.fillMaxSize(),
              topBar = {
                if (!isSearchActive && activePdfResource == null && selectedDetailResource == null &&
                  selectedCreator == null && selectedChartResource == null && !isSettingsOpen &&
                  !isNotificationsOpen && !isEditProfileOpen && !isFollowersOpen &&
                  !isFollowingOpen && activeThoughtForComments == null
                ) {
                  LearnerportHeader(
                    onSearchClick = { viewModel.openSearch() },
                    onNotificationsClick = { viewModel.openNotifications() },
                    onSettingsClick = { viewModel.openSettings() },
                    unreadNotificationsCount = unreadNotifsCount
                  )
                }
              },
              bottomBar = {
                if (!isSearchActive && activePdfResource == null && selectedDetailResource == null &&
                  selectedCreator == null && selectedChartResource == null && !isSettingsOpen &&
                  !isNotificationsOpen && !isEditProfileOpen && !isFollowersOpen &&
                  !isFollowingOpen && activeThoughtForComments == null
                ) {
                  LearnerportBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.selectTab(it) }
                  )
                }
              }
            ) { innerPadding ->
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(innerPadding)
              ) {
                when (currentTab) {
                  BottomNavTab.HOME -> {
                    HomeScreen(
                      resources = allResources,
                      thoughts = allThoughts,
                      currentUser = currentUser,
                      following = following,
                      followers = followers,
                      activeCategory = homeCategory,
                      onCategorySelected = { viewModel.setHomeCategory(it) },
                      onOpenSearch = { viewModel.openSearch() },
                      onCardClick = { viewModel.openDetail(it) },
                      onReadClick = { viewModel.openPdfReader(it) },
                      onSaveResourceClick = { viewModel.toggleSaveResource(it) },
                      onSaveThoughtClick = { viewModel.toggleSaveThought(it) },
                      onLikeThoughtClick = { viewModel.toggleLikeThought(it) },
                      onCommentThoughtClick = { activeThoughtForComments = it },
                      onCreatorClick = { creatorId ->
                        val creator = allCreators.firstOrNull { it.id == creatorId }
                        if (creator != null) viewModel.openCreator(creator)
                      },
                      onUploadClick = { viewModel.openUploadWithCategory(it) }
                    )
                  }

                  BottomNavTab.SAVED_DOWNLOADS -> {
                    SavedDownloadsScreen(
                      activeTab = savedDownloadsTab,
                      onTabChange = { viewModel.setSavedDownloadsTab(it) },
                      savedResources = allResources.filter { it.isSaved },
                      savedThoughts = allThoughts.filter { it.isSaved },
                      downloadedResources = allResources.filter { it.isDownloaded },
                      onCardClick = { viewModel.openDetail(it) },
                      onReadClick = { viewModel.openPdfReader(it) },
                      onSaveResourceClick = { viewModel.toggleSaveResource(it) },
                      onSaveThoughtClick = { viewModel.toggleSaveThought(it) },
                      onLikeThoughtClick = { viewModel.toggleLikeThought(it) },
                      onCommentThoughtClick = { activeThoughtForComments = it },
                      onCreatorClick = { creatorId ->
                        val creator = allCreators.firstOrNull { it.id == creatorId }
                        if (creator != null) viewModel.openCreator(creator)
                      }
                    )
                  }

                  BottomNavTab.UPLOAD -> {
                    val initialCategory by viewModel.uploadInitialCategory.collectAsStateWithLifecycle()
                    UploadScreen(
                      onPublishResource = { title, desc, rType, subject, topic, level, progLang, lang, tags, year, board, paperType, coverUrl, pdfBytes, pdfFileName, coverBytes, fileSize ->
                        viewModel.publishResource(
                          title, desc, rType, subject, topic, level, progLang, lang, tags, year, board, paperType, coverUrl, pdfBytes, pdfFileName, coverBytes, fileSize
                        )
                      },
                      onPublishThought = { title, content, tags ->
                        viewModel.publishThought(title, content, tags)
                      },
                      initialChoice = initialCategory
                    )
                  }

                  BottomNavTab.CHART -> {
                    ChartScreen(
                      analytics = userAnalytics,
                      userResources = allResources.filter { it.creatorId == currentUser.id },
                      onResourceSelect = { viewModel.openChartResourceDetail(it) }
                    )
                  }

                  BottomNavTab.PROFILE -> {
                    ProfileScreen(
                      user = currentUser,
                      userResources = allResources.filter { it.creatorId == currentUser.id },
                      userThoughts = allThoughts.filter { it.authorId == currentUser.id },
                      onEditProfileClick = { viewModel.openEditProfile() },
                      onFollowersClick = { viewModel.openFollowers() },
                      onFollowingClick = { viewModel.openFollowing() },
                      onCardClick = { viewModel.openDetail(it) },
                      onReadClick = { viewModel.openPdfReader(it) },
                      onSaveResourceClick = { viewModel.toggleSaveResource(it) },
                      onSaveThoughtClick = { viewModel.toggleSaveThought(it) },
                      onLikeThoughtClick = { viewModel.toggleLikeThought(it) },
                      onCommentThoughtClick = { activeThoughtForComments = it }
                    )
                  }
                }

                // Global Search Modal / Screen
                if (isSearchActive) {
                  SearchScreen(
                    searchQuery = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    selectedTab = searchCategory,
                    onTabSelected = { viewModel.setSearchCategory(it) },
                    resources = allResources,
                    thoughts = allThoughts,
                    creators = allCreators,
                    onClose = { viewModel.closeSearch() },
                    onCardClick = { viewModel.openDetail(it) },
                    onReadClick = { viewModel.openPdfReader(it) },
                    onSaveResourceClick = { viewModel.toggleSaveResource(it) },
                    onSaveThoughtClick = { viewModel.toggleSaveThought(it) },
                    onLikeThoughtClick = { viewModel.toggleLikeThought(it) },
                    onCommentThoughtClick = { activeThoughtForComments = it },
                    onCreatorClick = { viewModel.openCreator(it) },
                    onToggleFollow = { viewModel.toggleFollow(it) }
                  )
                }

                // PDF Reader Modal
                activePdfResource?.let { pdfRes ->
                  PdfReaderModal(
                    resource = pdfRes,
                    currentPage = pdfCurrentPage,
                    zoomLevel = pdfZoom,
                    onClose = { viewModel.closePdfReader() },
                    onNextPage = { viewModel.nextPdfPage() },
                    onPrevPage = { viewModel.prevPdfPage() },
                    onZoomIn = { viewModel.zoomInPdf() },
                    onZoomOut = { viewModel.zoomOutPdf() },
                    onResetZoom = { viewModel.resetPdfZoom() },
                    onDownload = {
                      Toast.makeText(this@MainActivity, "Downloading PDF...", Toast.LENGTH_SHORT).show()
                      viewModel.downloadResource(pdfRes.id, onComplete = { success, msg ->
                        runOnUiThread {
                          if (success) {
                            Toast.makeText(this@MainActivity, "Saved to device for offline study!", Toast.LENGTH_SHORT).show()
                          } else {
                            Toast.makeText(this@MainActivity, "Download: ${msg ?: "Failed"}", Toast.LENGTH_SHORT).show()
                          }
                        }
                      })
                    }
                  )
                }

                // Resource Detail Modal
                selectedDetailResource?.let { detailRes ->
                  ResourceDetailModal(
                    resource = detailRes,
                    commentsFlow = viewModel.getCommentsFlow(detailRes.id),
                    onClose = { viewModel.closeDetail() },
                    onReadClick = { viewModel.openPdfReader(detailRes) },
                    onSaveClick = { viewModel.toggleSaveResource(detailRes.id) },
                    onDownloadClick = {
                      Toast.makeText(this@MainActivity, "Downloading ${detailRes.title}...", Toast.LENGTH_SHORT).show()
                      viewModel.downloadResource(detailRes.id, onComplete = { success, msg ->
                        runOnUiThread {
                          if (success) {
                            Toast.makeText(this@MainActivity, "Downloaded! Ready for offline study.", Toast.LENGTH_SHORT).show()
                          } else {
                            Toast.makeText(this@MainActivity, "Download: ${msg ?: "Failed"}", Toast.LENGTH_SHORT).show()
                          }
                        }
                      })
                    },
                    onRateSubmit = { stars -> viewModel.submitRating(detailRes.id, stars) },
                    onAddComment = { text -> viewModel.addComment(detailRes.id, text) },
                    onCreatorClick = {
                      val creator = allCreators.firstOrNull { it.id == detailRes.creatorId }
                      if (creator != null) viewModel.openCreator(creator)
                    }
                  )
                }

                // Creator Profile Modal
                selectedCreator?.let { creator ->
                  CreatorProfileModal(
                    creator = creator,
                    creatorResources = allResources.filter { it.creatorId == creator.id },
                    creatorThoughts = allThoughts.filter { it.authorId == creator.id },
                    onClose = { viewModel.closeCreator() },
                    onToggleFollow = { viewModel.toggleFollow(creator.id) },
                    onCardClick = { viewModel.openDetail(it) },
                    onReadClick = { viewModel.openPdfReader(it) },
                    onSaveResourceClick = { viewModel.toggleSaveResource(it) },
                    onSaveThoughtClick = { viewModel.toggleSaveThought(it) },
                    onLikeThoughtClick = { viewModel.toggleLikeThought(it) },
                    onCommentThoughtClick = { activeThoughtForComments = it }
                  )
                }

                // Resource Analytics Modal (drill down from Chart screen)
                selectedChartResource?.let { chartRes ->
                  ResourceAnalyticsModal(
                    resource = chartRes,
                    commentsFlow = viewModel.getCommentsFlow(chartRes.id),
                    onClose = { viewModel.closeChartResourceDetail() },
                    onDeleteResource = { id -> viewModel.deleteResource(id) }
                  )
                }

                // Thought Comments Modal
                activeThoughtForComments?.let { thought ->
                  ThoughtCommentsModal(
                    thought = thought,
                    commentsFlow = viewModel.getCommentsFlow(thought.id),
                    onClose = { activeThoughtForComments = null },
                    onAddComment = { text -> viewModel.addComment(thought.id, text) }
                  )
                }

                // Settings Screen Modal (opened from top corner icon)
                if (isSettingsOpen) {
                  SettingsScreen(
                    currentUser = currentUser,
                    themeMode = themeMode,
                    onThemeModeChange = { viewModel.setThemeMode(this@MainActivity, it) },
                    onEditProfileClick = { viewModel.openEditProfile() },
                    onLogoutClick = { viewModel.signOut(this@MainActivity) },
                    onClose = { viewModel.closeSettings() }
                  )
                }

                // Followers Modal
                if (isFollowersOpen) {
                  FollowListModal(
                    title = "Followers",
                    users = followers,
                    onClose = { viewModel.closeFollowers() },
                    onUserClick = { user ->
                      viewModel.closeFollowers()
                      viewModel.openCreator(user)
                    },
                    onToggleFollow = { userId -> viewModel.toggleFollow(userId) }
                  )
                }

                // Following Modal
                if (isFollowingOpen) {
                  FollowListModal(
                    title = "Following",
                    users = following,
                    onClose = { viewModel.closeFollowing() },
                    onUserClick = { user ->
                      viewModel.closeFollowing()
                      viewModel.openCreator(user)
                    },
                    onToggleFollow = { userId -> viewModel.toggleFollow(userId) }
                  )
                }

                // Supabase Auth Dialog (if invoked explicitly)
                if (isAuthDialogOpen) {
                  AuthDialog(
                    onDismiss = { viewModel.closeAuthDialog() },
                    onAuthSuccess = { email, uname, disp ->
                      viewModel.handleAuthSuccess(email, uname, disp)
                    }
                  )
                }

                // Notifications Screen Modal
                if (isNotificationsOpen) {
                  NotificationsScreen(
                    notifications = notifications,
                    onClose = { viewModel.closeNotifications() },
                    onNotificationClick = { notif ->
                      viewModel.markNotificationRead(notif.id)
                      if (notif.targetResourceId != null) {
                        val target = allResources.firstOrNull { it.id == notif.targetResourceId }
                        if (target != null) {
                          viewModel.closeNotifications()
                          viewModel.openDetail(target)
                        }
                      }
                    },
                    onMarkAllRead = { viewModel.markAllNotificationsRead() }
                  )
                }

                // Edit Profile Modal
                if (isEditProfileOpen) {
                  EditProfileModal(
                    currentUser = currentUser,
                    onClose = { viewModel.closeEditProfile() },
                    onSave = { dispName, uname, bio, edu, course, inst, avatar ->
                      viewModel.updateProfile(dispName, uname, bio, edu, course, inst, avatar)
                    }
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIncomingDeepLink(intent)
  }

  private fun handleIncomingDeepLink(intent: Intent?) {
    val uri = intent?.data ?: return
    if (uri.scheme == "learnerport") {
      lifecycleScope.launch {
        val result = SupabaseClient.handleOAuthCallback(this@MainActivity, uri)
        result.onSuccess { session ->
          LearnerportRepository.onUserSessionEstablished(session)
          Toast.makeText(this@MainActivity, "Signed in successfully!", Toast.LENGTH_SHORT).show()
        }.onFailure { err ->
          Toast.makeText(this@MainActivity, "OAuth sign-in: ${err.message}", Toast.LENGTH_LONG).show()
        }
      }
    }
  }
}
