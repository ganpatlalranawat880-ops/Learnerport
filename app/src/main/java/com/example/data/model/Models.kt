package com.example.data.model

enum class ResourceType(val displayName: String) {
  NOTES("Notes"),
  BOOK("Book PDF"),
  QUESTION_PAPER("Question Paper"),
  CODING_RESOURCE("Coding Resource")
}

enum class PaperType(val label: String) {
  PREVIOUS_YEAR("Previous Year Paper"),
  SAMPLE_PAPER("Official Sample Paper"),
  MODEL_PAPER("Model Paper"),
  MOCK_TEST("Mock Test")
}

data class UserProfile(
  val id: String,
  val username: String,
  val displayName: String,
  val bio: String = "",
  val avatarUrl: String = "",
  val dateOfBirth: String = "",
  val educationalLevel: String = "",
  val courseOrDegree: String = "",
  val fieldOfStudy: String = "",
  val institution: String = "",
  val country: String = "",
  val state: String = "",
  val connectionType: String = "", // "School", "College", "Other"
  val connectionDetail: String = "", // Class 1-12, Degree Name, or Custom text
  val isProfileCompleted: Boolean = false,
  val isUsernameLocked: Boolean = false,
  val educationalInterests: List<String> = emptyList(),
  val preferredTopics: List<String> = emptyList(),
  val followersCount: Int = 0,
  val followingCount: Int = 0,
  val publishedCount: Int = 0,
  val isCurrentUser: Boolean = false,
  val isFollowedByCurrentUser: Boolean = false
)

data class FollowUser(
  val id: String,
  val username: String,
  val displayName: String,
  val avatarUrl: String = "",
  val isFollowedByCurrentUser: Boolean = false
)

data class ResourceItem(
  val id: String,
  val title: String,
  val description: String,
  val creatorId: String,
  val creatorUsername: String,
  val creatorDisplayName: String,
  val creatorAvatarUrl: String = "",
  val resourceType: ResourceType,
  val subject: String,
  val topic: String,
  val educationalLevel: String,
  val programmingLanguage: String? = null,
  val language: String = "English",
  val fileFormat: String = "PDF",
  val fileSize: String = "2.4 MB",
  val publicationDate: String,
  val timestamp: Long = System.currentTimeMillis(),
  val viewsCount: Int = 0,
  val downloadsCount: Int = 0,
  val averageRating: Float = 0.0f,
  val ratingsCount: Int = 0,
  val commentsCount: Int = 0,
  val tags: List<String> = emptyList(),
  val isSaved: Boolean = false,
  val isDownloaded: Boolean = false,
  val userRating: Int = 0,
  val pagesContent: List<String> = emptyList(),
  val examinationYear: String? = null,
  val boardOrUniversity: String? = null,
  val paperType: String? = null,
  val coverUrl: String = "",
  val pdfUrl: String = "",
  val localFilePath: String? = null
)

data class ThoughtItem(
  val id: String,
  val authorId: String,
  val authorUsername: String,
  val authorDisplayName: String,
  val authorAvatarUrl: String = "",
  val title: String? = null,
  val content: String,
  val tags: List<String> = emptyList(),
  val publicationDate: String,
  val timestamp: Long = System.currentTimeMillis(),
  val viewsCount: Int = 0,
  val likesCount: Int = 0,
  val commentsCount: Int = 0,
  val isLiked: Boolean = false,
  val isSaved: Boolean = false
)

data class CommentItem(
  val id: String,
  val targetId: String,
  val userId: String,
  val username: String,
  val userDisplayName: String,
  val userAvatarUrl: String = "",
  val commentText: String,
  val timestamp: Long = System.currentTimeMillis(),
  val formattedDate: String
)

enum class NotificationType {
  FOLLOW,
  COMMENT,
  RATING,
  PUBLICATION,
  SYSTEM
}

data class NotificationItem(
  val id: String,
  val title: String,
  val message: String,
  val type: NotificationType,
  val targetResourceId: String? = null,
  val timestamp: Long = System.currentTimeMillis(),
  val isRead: Boolean = false,
  val formattedTime: String = "Just now"
)

data class UserAnalytics(
  val totalPublished: Int = 0,
  val totalViews: Int = 0,
  val totalDownloads: Int = 0,
  val totalRatings: Int = 0,
  val averageRating: Float = 0f,
  val totalComments: Int = 0,
  val totalLikes: Int = 0,
  val ratingDistribution: Map<Int, Int> = mapOf(5 to 0, 4 to 0, 3 to 0, 2 to 0, 1 to 0)
)

enum class ThemeMode {
  LIGHT,
  DARK
}

enum class BottomNavTab(val label: String) {
  HOME("Home"),
  SAVED_DOWNLOADS("Saved & Downloads"),
  UPLOAD("Upload"),
  CHART("Chart"),
  PROFILE("Profile")
}

enum class SearchTab(val label: String) {
  PDFS_AND_NOTES("PDFs & Notes"),
  USERS("Profiles"),
  NOTES("Notes"),
  BOOKS("Book PDFs"),
  QUESTION_PAPERS("Question Papers"),
  THOUGHTS("Thoughts")
}
