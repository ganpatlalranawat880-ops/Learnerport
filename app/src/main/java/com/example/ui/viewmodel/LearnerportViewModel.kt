package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.remote.SupabaseClient
import com.example.data.repository.LearnerportRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LearnerportViewModel : ViewModel() {

  private val repository = LearnerportRepository

  private val _currentTab = MutableStateFlow(BottomNavTab.HOME)
  val currentTab: StateFlow<BottomNavTab> = _currentTab.asStateFlow()

  // Four strict categories for Home page
  private val _homeCategory = MutableStateFlow("Notes")
  val homeCategory: StateFlow<String> = _homeCategory.asStateFlow()

  private val _savedDownloadsTab = MutableStateFlow(0) // 0: Saved, 1: Downloads
  val savedDownloadsTab: StateFlow<Int> = _savedDownloadsTab.asStateFlow()

  // Search state
  private val _isSearchActive = MutableStateFlow(false)
  val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _searchCategory = MutableStateFlow(SearchTab.NOTES)
  val searchCategory: StateFlow<SearchTab> = _searchCategory.asStateFlow()

  // Modals & Sub-screens
  private val _activePdfResource = MutableStateFlow<ResourceItem?>(null)
  val activePdfResource: StateFlow<ResourceItem?> = _activePdfResource.asStateFlow()

  private val _pdfCurrentPage = MutableStateFlow(0)
  val pdfCurrentPage: StateFlow<Int> = _pdfCurrentPage.asStateFlow()

  private val _pdfZoom = MutableStateFlow(1.0f)
  val pdfZoom: StateFlow<Float> = _pdfZoom.asStateFlow()

  private val _selectedDetailResource = MutableStateFlow<ResourceItem?>(null)
  val selectedDetailResource: StateFlow<ResourceItem?> = _selectedDetailResource.asStateFlow()

  private val _selectedCreator = MutableStateFlow<UserProfile?>(null)
  val selectedCreator: StateFlow<UserProfile?> = _selectedCreator.asStateFlow()

  private val _selectedChartResource = MutableStateFlow<ResourceItem?>(null)
  val selectedChartResource: StateFlow<ResourceItem?> = _selectedChartResource.asStateFlow()

  private val _isSettingsOpen = MutableStateFlow(false)
  val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

  private val _isNotificationsOpen = MutableStateFlow(false)
  val isNotificationsOpen: StateFlow<Boolean> = _isNotificationsOpen.asStateFlow()

  private val _isEditProfileOpen = MutableStateFlow(false)
  val isEditProfileOpen: StateFlow<Boolean> = _isEditProfileOpen.asStateFlow()

  private val _isAuthDialogOpen = MutableStateFlow(false)
  val isAuthDialogOpen: StateFlow<Boolean> = _isAuthDialogOpen.asStateFlow()

  // Followers & Following Modals
  private val _isFollowersOpen = MutableStateFlow(false)
  val isFollowersOpen: StateFlow<Boolean> = _isFollowersOpen.asStateFlow()

  private val _isFollowingOpen = MutableStateFlow(false)
  val isFollowingOpen: StateFlow<Boolean> = _isFollowingOpen.asStateFlow()

  // Upload screen initial target category
  private val _uploadInitialCategory = MutableStateFlow<ResourceType?>(null)
  val uploadInitialCategory: StateFlow<ResourceType?> = _uploadInitialCategory.asStateFlow()

  // Repository-backed flows
  val isUserAuthenticated: StateFlow<Boolean> = repository.isUserAuthenticated

  val currentUser: StateFlow<UserProfile> = repository.getCurrentUser()
    .stateIn(
      viewModelScope,
      SharingStarted.Eagerly,
      UserProfile("", "", "", isCurrentUser = true, isProfileCompleted = false)
    )

  val themeMode: StateFlow<ThemeMode> = repository.getThemeMode()
    .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.LIGHT)

  val allResources: StateFlow<List<ResourceItem>> = repository.getAllResources()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val allThoughts: StateFlow<List<ThoughtItem>> = repository.getAllThoughts()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val allCreators: StateFlow<List<UserProfile>> = repository.getAllCreators()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val followers: StateFlow<List<UserProfile>> = repository.getFollowers()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val following: StateFlow<List<UserProfile>> = repository.getFollowing()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val notifications: StateFlow<List<NotificationItem>> = repository.getNotifications()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val userAnalytics: StateFlow<UserAnalytics> = repository.getUserAnalytics()
    .stateIn(viewModelScope, SharingStarted.Eagerly, UserAnalytics())

  fun selectTab(tab: BottomNavTab) {
    _currentTab.value = tab
    _isSearchActive.value = false
    _isSettingsOpen.value = false
    _isNotificationsOpen.value = false
    _isFollowersOpen.value = false
    _isFollowingOpen.value = false
  }

  fun setHomeCategory(category: String) {
    _homeCategory.value = category
  }

  fun setSavedDownloadsTab(tabIndex: Int) {
    _savedDownloadsTab.value = tabIndex
  }

  fun openSearch(initialCategory: SearchTab = SearchTab.NOTES) {
    _searchCategory.value = initialCategory
    _isSearchActive.value = true
  }

  fun closeSearch() {
    _isSearchActive.value = false
    _searchQuery.value = ""
  }

  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setSearchCategory(tab: SearchTab) {
    _searchCategory.value = tab
  }

  fun openUploadWithCategory(resourceType: ResourceType?) {
    _uploadInitialCategory.value = resourceType
    _currentTab.value = BottomNavTab.UPLOAD
  }

  fun openPdfReader(resource: ResourceItem) {
    repository.recordView(resource.id)
    _activePdfResource.value = resource
    _pdfCurrentPage.value = 0
    _pdfZoom.value = 1.0f
  }

  fun closePdfReader() {
    _activePdfResource.value = null
  }

  fun nextPdfPage() {
    val total = _activePdfResource.value?.pagesContent?.size ?: 1
    if (_pdfCurrentPage.value < total - 1) {
      _pdfCurrentPage.value += 1
    }
  }

  fun prevPdfPage() {
    if (_pdfCurrentPage.value > 0) {
      _pdfCurrentPage.value -= 1
    }
  }

  fun zoomInPdf() {
    if (_pdfZoom.value < 2.0f) _pdfZoom.value += 0.2f
  }

  fun zoomOutPdf() {
    if (_pdfZoom.value > 0.8f) _pdfZoom.value -= 0.2f
  }

  fun resetPdfZoom() {
    _pdfZoom.value = 1.0f
  }

  fun openDetail(resource: ResourceItem) {
    repository.recordView(resource.id)
    _selectedDetailResource.value = repository.getResourceById(resource.id) ?: resource
  }

  fun closeDetail() {
    _selectedDetailResource.value = null
  }

  fun openCreator(user: UserProfile) {
    _selectedCreator.value = user
  }

  fun closeCreator() {
    _selectedCreator.value = null
  }

  fun openChartResourceDetail(resource: ResourceItem) {
    _selectedChartResource.value = resource
  }

  fun closeChartResourceDetail() {
    _selectedChartResource.value = null
  }

  fun openSettings() {
    _isSettingsOpen.value = true
  }

  fun closeSettings() {
    _isSettingsOpen.value = false
  }

  fun openNotifications() {
    _isNotificationsOpen.value = true
  }

  fun closeNotifications() {
    _isNotificationsOpen.value = false
  }

  fun openEditProfile() {
    _isEditProfileOpen.value = true
  }

  fun closeEditProfile() {
    _isEditProfileOpen.value = false
  }

  fun openFollowers() {
    _isFollowersOpen.value = true
  }

  fun closeFollowers() {
    _isFollowersOpen.value = false
  }

  fun openFollowing() {
    _isFollowingOpen.value = true
  }

  fun closeFollowing() {
    _isFollowingOpen.value = false
  }

  fun toggleSaveResource(resourceId: String) {
    repository.toggleSaveResource(resourceId)
    if (_selectedDetailResource.value?.id == resourceId) {
      _selectedDetailResource.value = repository.getResourceById(resourceId)
    }
  }

  fun toggleSaveThought(thoughtId: String) {
    repository.toggleSaveThought(thoughtId)
  }

  fun toggleLikeThought(thoughtId: String) {
    repository.toggleLikeThought(thoughtId)
  }

  fun downloadResource(
    resourceId: String,
    onProgress: (Int) -> Unit = {},
    onComplete: (Boolean, String?) -> Unit = { _, _ -> }
  ) {
    repository.recordDownload(resourceId, onProgress, onComplete)
    if (_selectedDetailResource.value?.id == resourceId) {
      _selectedDetailResource.value = repository.getResourceById(resourceId)
    }
  }

  fun submitRating(resourceId: String, stars: Int) {
    repository.submitRating(resourceId, stars)
    if (_selectedDetailResource.value?.id == resourceId) {
      _selectedDetailResource.value = repository.getResourceById(resourceId)
    }
  }

  fun addComment(targetId: String, text: String) {
    if (text.isNotBlank()) {
      repository.addComment(targetId, text)
      if (_selectedDetailResource.value?.id == targetId) {
        _selectedDetailResource.value = repository.getResourceById(targetId)
      }
    }
  }

  fun getCommentsFlow(targetId: String) = repository.getCommentsFor(targetId)

  fun toggleFollow(userId: String) {
    repository.toggleFollowUser(userId)
    if (_selectedCreator.value?.id == userId) {
      val updated = allCreators.value.firstOrNull { it.id == userId }
      if (updated != null) _selectedCreator.value = updated
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
    repository.publishResource(
      title = title,
      description = description,
      resourceType = resourceType,
      subject = subject,
      topic = topic,
      educationalLevel = educationalLevel,
      programmingLanguage = programmingLanguage,
      language = language,
      tags = tags,
      examinationYear = examinationYear,
      boardOrUniversity = boardOrUniversity,
      paperType = paperType,
      coverUrl = coverUrl,
      pdfBytes = pdfBytes,
      pdfFileName = pdfFileName,
      coverBytes = coverBytes,
      fileSize = fileSize
    )
    _currentTab.value = BottomNavTab.HOME
  }

  fun publishThought(title: String?, content: String, tags: List<String>) {
    repository.publishThought(title, content, tags)
    _currentTab.value = BottomNavTab.HOME
  }

  fun deleteResource(id: String) {
    repository.deleteResource(id)
    if (_selectedChartResource.value?.id == id) {
      _selectedChartResource.value = null
    }
    if (_selectedDetailResource.value?.id == id) {
      _selectedDetailResource.value = null
    }
  }

  fun updateProfile(
    displayName: String,
    username: String,
    bio: String,
    educationalLevel: String,
    courseOrDegree: String,
    institution: String,
    avatarUrl: String
  ): Boolean {
    val success = repository.updateCurrentUserProfile(
      displayName,
      username,
      bio,
      educationalLevel,
      courseOrDegree,
      institution,
      avatarUrl
    )
    if (success) {
      _isEditProfileOpen.value = false
    }
    return success
  }

  fun setThemeMode(context: Context, mode: ThemeMode) {
    repository.setThemeMode(context, mode)
  }

  fun markAllNotificationsRead() {
    repository.markAllNotificationsRead()
  }

  fun markNotificationRead(id: String) {
    repository.markNotificationRead(id)
  }

  fun openAuthDialog() {
    _isAuthDialogOpen.value = true
  }

  fun closeAuthDialog() {
    _isAuthDialogOpen.value = false
  }

  fun handleAuthSuccess(email: String, username: String, displayName: String) {
    val session = SupabaseClient.currentSession.value
    if (session != null) {
      repository.onUserSessionEstablished(session)
    }
    _isAuthDialogOpen.value = false
  }

  fun signOut(context: Context) {
    viewModelScope.launch {
      repository.signOut(context)
      _isSettingsOpen.value = false
      _currentTab.value = BottomNavTab.HOME
    }
  }
}
