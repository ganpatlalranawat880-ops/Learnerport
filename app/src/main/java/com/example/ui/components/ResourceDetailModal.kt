package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommentItem
import com.example.data.model.ResourceItem
import com.example.data.model.ResourceType
import com.example.ui.theme.LearnerAccentGold
import com.example.ui.theme.LearnerBlue
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceDetailModal(
  resource: ResourceItem,
  commentsFlow: Flow<List<CommentItem>>,
  onClose: () -> Unit,
  onReadClick: () -> Unit,
  onSaveClick: () -> Unit,
  onDownloadClick: () -> Unit,
  onRateSubmit: (Int) -> Unit,
  onAddComment: (String) -> Unit,
  onCreatorClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val comments by commentsFlow.collectAsState(initial = emptyList())
  var newCommentText by remember { mutableStateOf("") }
  var selectedRating by remember(resource.userRating) { mutableIntStateOf(resource.userRating) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Resource Details", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onClose) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
          }
        },
        actions = {
          IconButton(onClick = onSaveClick) {
            Icon(
              imageVector = if (resource.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
              contentDescription = "Save",
              tint = if (resource.isSaved) LearnerBlue else MaterialTheme.colorScheme.onSurface
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    bottomBar = {
      // Bottom sticky action bar with Read and Download
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedButton(
            onClick = onDownloadClick,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = if (resource.isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (resource.isDownloaded) "Downloaded" else "Download", fontSize = 13.sp)
          }

          Button(
            onClick = onReadClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue),
            modifier = Modifier.weight(1.5f)
          ) {
            Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Read Document", fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        Spacer(modifier = Modifier.height(4.dp))
        // Badges Row
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = LearnerBlue.copy(alpha = 0.12f)
          ) {
            Text(
              text = resource.resourceType.displayName,
              color = LearnerBlue,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }

          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
          ) {
            Text(
              text = resource.subject,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }

          if (resource.programmingLanguage != null) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFF3E8FF)
            ) {
              Text(
                text = resource.programmingLanguage,
                color = Color(0xFF7E22CE),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }

      // Title
      item {
        Text(
          text = resource.title,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      // Creator Row
      item {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onCreatorClick() }
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(LearnerBlue.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = resource.creatorDisplayName.take(1).uppercase(),
                color = LearnerBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = resource.creatorDisplayName,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "@${resource.creatorUsername} • ${resource.publicationDate}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Icon(
              imageVector = Icons.Default.ChevronRight,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // Description
      item {
        Text(
          text = "Description",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = resource.description,
          style = MaterialTheme.typography.bodyMedium,
          lineHeight = 22.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      // Metadata Grid Card
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Resource Specifications", fontWeight = FontWeight.Bold, fontSize = 13.sp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Educational Level:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(resource.educationalLevel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Topic:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(resource.topic, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Format & Size:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("${resource.fileFormat} (${resource.fileSize})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            if (!resource.boardOrUniversity.isNullOrBlank()) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Board / University:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(resource.boardOrUniversity, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              }
            }
            if (!resource.examinationYear.isNullOrBlank()) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Exam Year:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(resource.examinationYear, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              }
            }
            if (!resource.paperType.isNullOrBlank()) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Paper Type:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(resource.paperType, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
      }

      // Ratings & Reviews Section
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = CardDefaults.outlinedCardBorder(),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("Ratings & Reviews", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = String.format("%.1f", resource.averageRating),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Row {
                  repeat(5) { index ->
                    Icon(
                      imageVector = if (index < resource.averageRating.toInt()) Icons.Filled.Star else Icons.Outlined.StarBorder,
                      contentDescription = null,
                      tint = LearnerAccentGold,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
                Text(
                  text = "Based on ${resource.ratingsCount} reviews",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Text("Rate this resource:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              (1..5).forEach { star ->
                IconButton(
                  onClick = {
                    selectedRating = star
                    onRateSubmit(star)
                  },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(
                    imageVector = if (star <= selectedRating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "$star stars",
                    tint = if (star <= selectedRating) LearnerAccentGold else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
            if (selectedRating > 0) {
              Text("Your rating: $selectedRating ★ (Tap another star to update)", fontSize = 11.sp, color = LearnerBlue)
            }
          }
        }
      }

      // Comments Section
      item {
        Text("Discussion & Comments (${comments.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))

        // New Comment Input
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = newCommentText,
            onValueChange = { newCommentText = it },
            placeholder = { Text("Add a constructive comment...", fontSize = 13.sp) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          )

          Button(
            onClick = {
              if (newCommentText.isNotBlank()) {
                onAddComment(newCommentText)
                newCommentText = ""
              }
            },
            enabled = newCommentText.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue)
          ) {
            Text("Post", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      if (comments.isEmpty()) {
        item {
          Text(
            text = "No comments yet. Start the educational discussion!",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp)
          )
        }
      } else {
        items(comments) { comment ->
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = comment.userDisplayName,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )
                Text(
                  text = comment.formattedDate,
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = comment.commentText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }
}
