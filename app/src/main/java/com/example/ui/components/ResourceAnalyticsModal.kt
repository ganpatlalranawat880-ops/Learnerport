package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommentItem
import com.example.data.model.ResourceItem
import com.example.ui.screens.MetricSummaryCard
import com.example.ui.theme.LearnerAccentGold
import com.example.ui.theme.LearnerAccentGreen
import com.example.ui.theme.LearnerBlue
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceAnalyticsModal(
  resource: ResourceItem,
  commentsFlow: Flow<List<CommentItem>>,
  onClose: () -> Unit,
  onDeleteResource: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val comments by commentsFlow.collectAsState(initial = emptyList())

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Resource Performance", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onClose) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
          }
        },
        actions = {
          IconButton(onClick = { onDeleteResource(resource.id) }) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Resource", tint = MaterialTheme.colorScheme.error)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
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
        Text(
          text = resource.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "${resource.resourceType.displayName} • Published on ${resource.publicationDate}",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Metrics grid
      item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          MetricSummaryCard(
            title = "Views",
            value = "${resource.viewsCount}",
            icon = Icons.Outlined.Visibility,
            iconTint = LearnerBlue,
            modifier = Modifier.weight(1f)
          )
          MetricSummaryCard(
            title = "Downloads",
            value = "${resource.downloadsCount}",
            icon = Icons.Default.FileDownload,
            iconTint = LearnerAccentGreen,
            modifier = Modifier.weight(1f)
          )
        }
      }

      item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          MetricSummaryCard(
            title = "Average Rating",
            value = "${resource.averageRating} ★",
            icon = Icons.Default.Star,
            iconTint = LearnerAccentGold,
            modifier = Modifier.weight(1f)
          )
          MetricSummaryCard(
            title = "Total Reviews",
            value = "${resource.ratingsCount}",
            icon = Icons.Default.RateReview,
            iconTint = Color(0xFF6366F1),
            modifier = Modifier.weight(1f)
          )
        }
      }

      // Performance Graph / Timeline indicator
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = CardDefaults.outlinedCardBorder(),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("Engagement Velocity", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              "Your resource is in the top 15% of newly discovered educational materials this week with healthy download-to-view conversion.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Conversion progress
            val downloadRatio = if (resource.viewsCount > 0) resource.downloadsCount.toFloat() / resource.viewsCount.toFloat() else 0f
            Text("Download Conversion Rate: ${(downloadRatio * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
              progress = { downloadRatio.coerceIn(0f, 1f) },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
              color = LearnerAccentGreen,
              trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
          }
        }
      }

      // User comments on this resource
      item {
        Text("Learner Feedback (${comments.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
      }

      if (comments.isEmpty()) {
        item {
          Text("No reader comments received yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(comment.userDisplayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(comment.formattedDate, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(comment.commentText, fontSize = 12.sp)
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}
