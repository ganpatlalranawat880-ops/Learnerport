package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ResourceItem
import com.example.data.model.ThoughtItem
import com.example.ui.components.ResourceCard
import com.example.ui.components.ThoughtCard
import com.example.ui.theme.LearnerBlue

@Composable
fun SavedDownloadsScreen(
  activeTab: Int, // 0: Saved, 1: Downloads
  onTabChange: (Int) -> Unit,
  savedResources: List<ResourceItem>,
  savedThoughts: List<ThoughtItem>,
  downloadedResources: List<ResourceItem>,
  onCardClick: (ResourceItem) -> Unit,
  onReadClick: (ResourceItem) -> Unit,
  onSaveResourceClick: (String) -> Unit,
  onSaveThoughtClick: (String) -> Unit,
  onLikeThoughtClick: (String) -> Unit,
  onCommentThoughtClick: (ThoughtItem) -> Unit,
  onCreatorClick: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val tabs = listOf("Saved (${savedResources.size + savedThoughts.size})", "Downloads (${downloadedResources.size})")

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(10.dp))

    // Two clearly distinguishable tabs
    TabRow(
      selectedTabIndex = activeTab,
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = LearnerBlue,
      modifier = Modifier.testTag("saved_downloads_tab_row")
    ) {
      tabs.forEachIndexed { index, title ->
        Tab(
          selected = activeTab == index,
          onClick = { onTabChange(index) },
          text = {
            Text(
              text = title,
              fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Medium,
              fontSize = 13.sp
            )
          },
          modifier = Modifier.testTag(if (index == 0) "tab_saved" else "tab_downloads")
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      if (activeTab == 0) {
        // SAVED TAB
        if (savedResources.isEmpty() && savedThoughts.isEmpty()) {
          item {
            EmptyListPlaceholder(
              icon = Icons.Default.BookmarkRemove,
              title = "No Saved Resources",
              subtitle = "Tap the bookmark icon on any note, book, question paper, or thought to save it here for quick access."
            )
          }
        } else {
          items(savedResources) { resource ->
            ResourceCard(
              resource = resource,
              onCardClick = { onCardClick(resource) },
              onReadClick = { onReadClick(resource) },
              onSaveClick = { onSaveResourceClick(resource.id) },
              onCreatorClick = { onCreatorClick(resource.creatorId) }
            )
          }

          items(savedThoughts) { thought ->
            ThoughtCard(
              thought = thought,
              onLikeClick = { onLikeThoughtClick(thought.id) },
              onCommentClick = { onCommentThoughtClick(thought) },
              onSaveClick = { onSaveThoughtClick(thought.id) },
              onShareClick = { },
              onAuthorClick = { onCreatorClick(thought.authorId) }
            )
          }
        }
      } else {
        // DOWNLOADS TAB
        if (downloadedResources.isEmpty()) {
          item {
            EmptyListPlaceholder(
              icon = Icons.Default.FileDownload,
              title = "No Download History",
              subtitle = "Downloaded materials will be recorded here so you can review and open them anytime."
            )
          }
        } else {
          items(downloadedResources) { resource ->
            ResourceCard(
              resource = resource,
              onCardClick = { onCardClick(resource) },
              onReadClick = { onReadClick(resource) },
              onSaveClick = { onSaveResourceClick(resource.id) },
              onCreatorClick = { onCreatorClick(resource.creatorId) }
            )
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
fun EmptyListPlaceholder(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 60.dp, bottom = 40.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(horizontal = 24.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(64.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LearnerBlue,
            modifier = Modifier.size(32.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 18.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )
    }
  }
}
