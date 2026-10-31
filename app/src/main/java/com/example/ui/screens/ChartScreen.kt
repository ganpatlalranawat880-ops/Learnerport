package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ResourceItem
import com.example.data.model.UserAnalytics
import com.example.ui.theme.LearnerAccentGold
import com.example.ui.theme.LearnerAccentGreen
import com.example.ui.theme.LearnerBlue

@Composable
fun ChartScreen(
  analytics: UserAnalytics,
  userResources: List<ResourceItem>,
  onResourceSelect: (ResourceItem) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Personal Content Analytics",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = "Performance and audience engagement for your published educational content.",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    // Top Summary Metric Cards Grid (2x3)
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          MetricSummaryCard(
            title = "Published",
            value = "${analytics.totalPublished}",
            icon = Icons.Default.Description,
            iconTint = LearnerBlue,
            modifier = Modifier.weight(1f)
          )
          MetricSummaryCard(
            title = "Total Views",
            value = "${analytics.totalViews}",
            icon = Icons.Outlined.Visibility,
            iconTint = Color(0xFF6366F1),
            modifier = Modifier.weight(1f)
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          MetricSummaryCard(
            title = "Downloads",
            value = "${analytics.totalDownloads}",
            icon = Icons.Default.FileDownload,
            iconTint = LearnerAccentGreen,
            modifier = Modifier.weight(1f)
          )
          MetricSummaryCard(
            title = "Avg Rating",
            value = if (analytics.averageRating > 0) "${analytics.averageRating} ★" else "N/A",
            icon = Icons.Default.Star,
            iconTint = LearnerAccentGold,
            modifier = Modifier.weight(1f)
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          MetricSummaryCard(
            title = "Comments",
            value = "${analytics.totalComments}",
            icon = Icons.Outlined.ChatBubbleOutline,
            iconTint = Color(0xFFEC4899),
            modifier = Modifier.weight(1f)
          )
          MetricSummaryCard(
            title = "Likes",
            value = "${analytics.totalLikes}",
            icon = Icons.Default.Favorite,
            iconTint = Color(0xFFEF4444),
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    // Rating Distribution Bar Chart
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Rating Breakdown",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(12.dp))

          val totalRatings = analytics.totalRatings.coerceAtLeast(1)

          (5 downTo 1).forEach { star ->
            val count = analytics.ratingDistribution[star] ?: 0
            val fraction = count.toFloat() / totalRatings.toFloat()

            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
            ) {
              Text(
                text = "$star ★",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(32.dp)
              )

              LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                  .weight(1f)
                  .height(8.dp)
                  .clip(RoundedCornerShape(4.dp)),
                color = LearnerAccentGold,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
              )

              Spacer(modifier = Modifier.width(10.dp))

              Text(
                text = "$count",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp)
              )
            }
          }
        }
      }
    }

    // Published Resources Performance Breakdown
    item {
      Text(
        text = "Your Published Resources (${userResources.size})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    if (userResources.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "You haven't published any resources yet. Tap 'Upload' to publish notes or papers and view real analytics here.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp)
          )
        }
      }
    } else {
      items(userResources) { res ->
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onResourceSelect(res) }
            .testTag("chart_resource_${res.id}"),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = CardDefaults.outlinedCardBorder()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = LearnerBlue.copy(alpha = 0.1f)
              ) {
                Text(
                  text = res.resourceType.displayName,
                  color = LearnerBlue,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
              Text(
                text = res.publicationDate,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = res.title,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Resource Metrics Bar
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(3.dp))
                Text("${res.viewsCount} views", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(3.dp))
                Text("${res.downloadsCount} dls", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = LearnerAccentGold)
                Spacer(modifier = Modifier.width(3.dp))
                Text("${res.averageRating} (${res.ratingsCount})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(3.dp))
                Text("${res.commentsCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }

              Icon(Icons.Default.ChevronRight, contentDescription = "View Details", tint = LearnerBlue, modifier = Modifier.size(18.dp))
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
fun MetricSummaryCard(
  title: String,
  value: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconTint: Color,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = iconTint.copy(alpha = 0.12f),
        modifier = Modifier.size(40.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column {
        Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
      }
    }
  }
}
