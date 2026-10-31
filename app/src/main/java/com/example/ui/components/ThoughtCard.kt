package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ThoughtItem
import com.example.ui.theme.LearnerAccentRed
import com.example.ui.theme.LearnerBlue

@Composable
fun ThoughtCard(
  thought: ThoughtItem,
  onLikeClick: () -> Unit,
  onCommentClick: () -> Unit,
  onSaveClick: () -> Unit,
  onShareClick: () -> Unit = {},
  onAuthorClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(false) }
  val isLongText = thought.content.length > 280

  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .animateContentSize()
      .testTag("thought_card_${thought.id}"),
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
          modifier = Modifier.clickable { onAuthorClick() }
        ) {
          if (thought.authorAvatarUrl.isNotBlank()) {
            AsyncImage(
              model = thought.authorAvatarUrl,
              contentDescription = "Author Avatar",
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
                .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = thought.authorDisplayName.take(1).uppercase(),
                color = Color(0xFF8B5CF6),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              )
            }
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column {
            Text(
              text = thought.authorDisplayName,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "@${thought.authorUsername} • ${thought.publicationDate}",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        IconButton(
          onClick = onSaveClick,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = if (thought.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = "Save thought",
            tint = if (thought.isSaved) LearnerBlue else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // ================= 2. MIDDLE AREA =================
      // Title and Description / Text Post / Quote card
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          if (!thought.title.isNullOrBlank()) {
            Text(
              text = thought.title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
          }

          Row(verticalAlignment = Alignment.Top) {
            Icon(
              imageVector = Icons.Default.FormatQuote,
              contentDescription = null,
              tint = Color(0xFF8B5CF6),
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text(
                text = if (isLongText && !isExpanded) {
                  thought.content.take(280).trim() + "..."
                } else {
                  thought.content
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
              )

              if (isLongText) {
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                  onClick = { isExpanded = !isExpanded },
                  contentPadding = PaddingValues(0.dp)
                ) {
                  Text(
                    text = if (isExpanded) "Show Less" else "Read More (Full Post)",
                    color = Color(0xFF8B5CF6),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                  )
                }
              }
            }
          }
        }
      }

      if (thought.tags.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          thought.tags.forEach { tag ->
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFF8B5CF6).copy(alpha = 0.1f)
            ) {
              Text(
                text = "#$tag",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF8B5CF6),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      Spacer(modifier = Modifier.height(8.dp))

      // ================= 3. BOTTOM AREA =================
      // Views counter, Likes, Comments, Share
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Views counter
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.Visibility,
            contentDescription = "Views",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${thought.viewsCount} views",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
          )
        }

        // Actions: Likes, Comments, Share
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onLikeClick,
              modifier = Modifier.size(34.dp)
            ) {
              Icon(
                imageVector = if (thought.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Like",
                tint = if (thought.isLiked) LearnerAccentRed else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Text(
              text = "${thought.likesCount}",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (thought.isLiked) LearnerAccentRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onCommentClick() }
          ) {
            IconButton(
              onClick = onCommentClick,
              modifier = Modifier.size(34.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = "Comment",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Text(
              text = "${thought.commentsCount}",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          IconButton(
            onClick = onShareClick,
            modifier = Modifier.size(34.dp)
          ) {
            Icon(
              imageVector = Icons.Filled.Share,
              contentDescription = "Share",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}
