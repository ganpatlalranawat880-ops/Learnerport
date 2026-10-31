package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ResourceItem
import com.example.data.model.ResourceType
import com.example.ui.theme.LearnerAccentGold
import com.example.ui.theme.LearnerBlue

@Composable
fun ResourceCard(
  resource: ResourceItem,
  onCardClick: () -> Unit,
  onReadClick: () -> Unit,
  onSaveClick: () -> Unit,
  onCreatorClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val (badgeBg, badgeFg) = when (resource.resourceType) {
    ResourceType.NOTES -> Color(0xFFEFF6FF) to Color(0xFF1D4ED8)
    ResourceType.BOOK -> Color(0xFFF0FDF4) to Color(0xFF15803D)
    ResourceType.QUESTION_PAPER -> Color(0xFFFEF3C7) to Color(0xFFB45309)
    ResourceType.CODING_RESOURCE -> Color(0xFFFAF5FF) to Color(0xFF7E22CE)
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable { onCardClick() }
      .testTag("resource_card_${resource.id}"),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {

      // ================= 1. TOP AREA =================
      // Publisher's Profile Image, Username, and published time
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clickable { onCreatorClick() }
            .testTag("creator_info_${resource.creatorUsername}")
        ) {
          if (resource.creatorAvatarUrl.isNotBlank()) {
            AsyncImage(
              model = resource.creatorAvatarUrl,
              contentDescription = "Publisher Avatar",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
            )
          } else {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(LearnerBlue.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = resource.creatorDisplayName.take(1).uppercase(),
                color = LearnerBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              )
            }
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column {
            Text(
              text = resource.creatorDisplayName,
              fontWeight = FontWeight.SemiBold,
              fontSize = 14.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "@${resource.creatorUsername} • ${resource.publicationDate}",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        IconButton(
          onClick = onSaveClick,
          modifier = Modifier
            .size(36.dp)
            .testTag("save_button_${resource.id}")
        ) {
          Icon(
            imageVector = if (resource.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = if (resource.isSaved) "Remove from saved" else "Save resource",
            tint = if (resource.isSaved) LearnerBlue else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // ================= 2. MIDDLE AREA =================
      // Title and Short Description
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = badgeBg
        ) {
          Text(
            text = resource.resourceType.displayName,
            color = badgeFg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = MaterialTheme.colorScheme.surfaceVariant
        ) {
          Text(
            text = resource.subject,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }

        if (resource.isDownloaded) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFDCFCE7)
          ) {
            Text(
              text = "Offline",
              color = Color(0xFF166534),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = resource.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = resource.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(12.dp))

      // ================= 3. BOTTOM AREA =================
      // PDF Card (with Strict 3:4 Aspect Ratio Cover) + Views Counter & Read Action
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
          .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Strict 3:4 Standard Portrait Book Ratio Cover
        Surface(
          shape = RoundedCornerShape(8.dp),
          shadowElevation = 2.dp,
          modifier = Modifier
            .width(54.dp)
            .aspectRatio(3f / 4f) // Strict 3:4 Portrait Book Ratio
            .clip(RoundedCornerShape(8.dp))
        ) {
          when {
            resource.coverUrl.startsWith("http") -> {
              AsyncImage(
                model = resource.coverUrl,
                contentDescription = "PDF Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
            }
            resource.coverUrl.contains("red") -> {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(Brush.verticalGradient(listOf(Color(0xFFEF4444), Color(0xFF991B1B)))),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
              }
            }
            resource.coverUrl.contains("green") -> {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF065F46)))),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
              }
            }
            else -> {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(Brush.verticalGradient(listOf(Color(0xFF007AFF), Color(0xFF004385)))),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
              }
            }
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${resource.fileFormat} • ${resource.fileSize}",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(2.dp))

          // Views Counter
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.Visibility,
              contentDescription = "Views",
              tint = LearnerBlue,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${resource.viewsCount} views",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = LearnerBlue
            )

            Spacer(modifier = Modifier.width(8.dp))
            Icon(
              imageVector = Icons.Filled.Star,
              contentDescription = "Rating",
              tint = LearnerAccentGold,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
              text = String.format("%.1f", resource.averageRating),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        // Read Button
        FilledTonalButton(
          onClick = onReadClick,
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = LearnerBlue,
            contentColor = Color.White
          ),
          modifier = Modifier.testTag("read_button_${resource.id}")
        ) {
          Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "Read", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }
  }
}
