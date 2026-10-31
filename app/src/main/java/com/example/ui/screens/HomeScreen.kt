package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ResourceItem
import com.example.data.model.ResourceType
import com.example.data.model.ThoughtItem
import com.example.data.model.UserProfile
import com.example.ui.components.ResourceCard
import com.example.ui.components.ThoughtCard
import com.example.ui.theme.LearnerBlue

private sealed class FeedItem(val timestamp: Long) {
  data class Resource(val item: ResourceItem) : FeedItem(item.timestamp)
  data class Thought(val item: ThoughtItem) : FeedItem(item.timestamp)
}

@Composable
fun HomeScreen(
  resources: List<ResourceItem>,
  thoughts: List<ThoughtItem>,
  currentUser: UserProfile,
  following: List<UserProfile> = emptyList(),
  followers: List<UserProfile> = emptyList(),
  activeCategory: String, // Kept for interface compatibility
  onCategorySelected: (String) -> Unit,
  onOpenSearch: () -> Unit,
  onCardClick: (ResourceItem) -> Unit,
  onReadClick: (ResourceItem) -> Unit,
  onSaveResourceClick: (String) -> Unit,
  onSaveThoughtClick: (String) -> Unit,
  onLikeThoughtClick: (String) -> Unit,
  onCommentThoughtClick: (ThoughtItem) -> Unit,
  onCreatorClick: (String) -> Unit,
  onUploadClick: (ResourceType?) -> Unit,
  modifier: Modifier = Modifier
) {
  // Triple Horizontal Feed Tabs: "Following", "Follower", "Other All"
  var activeFeedTab by remember { mutableStateOf("Other All") }

  // 4 Sub-Filters: "All", "Notes", "Book PDF", "Thought", "Question Paper"
  var activeSubFilter by remember { mutableStateOf("All") }
  val subFilterOptions = listOf("All", "Notes", "Book PDF", "Thought", "Question Paper")

  val followingIds = remember(following) { following.map { it.id }.toSet() }
  val followerIds = remember(followers) { followers.map { it.id }.toSet() }

  // 1. Filter by Feed Tab
  val (tabResources, tabThoughts) = remember(activeFeedTab, resources, thoughts, followingIds, followerIds, currentUser.id) {
    when (activeFeedTab) {
      "Following" -> {
        val r = resources.filter { it.creatorId in followingIds }
        val t = thoughts.filter { it.authorId in followingIds }
        r to t
      }
      "Follower" -> {
        val r = resources.filter { it.creatorId in followerIds }
        val t = thoughts.filter { it.authorId in followerIds }
        r to t
      }
      else -> {
        // "Other All": All public content by anyone except current user's followers or following
        val r = resources.filter {
          it.creatorId != currentUser.id && it.creatorId !in followingIds && it.creatorId !in followerIds
        }.sortedByDescending { it.timestamp }

        val t = thoughts.filter {
          it.authorId != currentUser.id && it.authorId !in followingIds && it.authorId !in followerIds
        }.sortedByDescending { it.timestamp }
        r to t
      }
    }
  }

  // 2. Filter by Sub-Category Option
  val displayFeedItems = remember(activeSubFilter, tabResources, tabThoughts) {
    val items = mutableListOf<FeedItem>()
    when (activeSubFilter) {
      "Notes" -> {
        tabResources.filter { it.resourceType == ResourceType.NOTES || it.resourceType == ResourceType.CODING_RESOURCE }
          .forEach { items.add(FeedItem.Resource(it)) }
      }
      "Book PDF" -> {
        tabResources.filter { it.resourceType == ResourceType.BOOK }
          .forEach { items.add(FeedItem.Resource(it)) }
      }
      "Question Paper" -> {
        tabResources.filter { it.resourceType == ResourceType.QUESTION_PAPER }
          .forEach { items.add(FeedItem.Resource(it)) }
      }
      "Thought" -> {
        tabThoughts.forEach { items.add(FeedItem.Thought(it)) }
      }
      else -> {
        // "All" - Interleaved sorted newest first
        tabResources.forEach { items.add(FeedItem.Resource(it)) }
        tabThoughts.forEach { items.add(FeedItem.Thought(it)) }
      }
    }
    items.sortedByDescending { it.timestamp }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(4.dp))

      // 1. UNIFIED SEARCH BAR (Matches Search Page search bar, smaller height, placeholder only "serch users ,pdf,notes.....")
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = CardDefaults.outlinedCardBorder(),
        shadowElevation = 0.5.dp,
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onOpenSearch() }
          .testTag("home_search_bar")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = LearnerBlue,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "serch users ,pdf,notes.....",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            modifier = Modifier.weight(1f)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 2. TRIPLE HORIZONTAL FEED TABS: [Following, Follower, Other All]
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
          .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        listOf("Following", "Follower", "Other All").forEach { feedTab ->
          val isSelected = activeFeedTab == feedTab
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isSelected) LearnerBlue else Color.Transparent,
            modifier = Modifier
              .weight(1f)
              .clickable { activeFeedTab = feedTab }
              .testTag("feed_tab_${feedTab.lowercase().replace(" ", "_")}")
          ) {
            Box(
              modifier = Modifier.padding(vertical = 8.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = feedTab,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 3. SUB-FILTERS FOR CURRENT FEED: [All, Notes, Book PDF, Thought, Question Paper]
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        subFilterOptions.forEach { opt ->
          val isSelected = activeSubFilter == opt
          FilterChip(
            selected = isSelected,
            onClick = { activeSubFilter = opt },
            label = {
              Text(
                text = opt,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp
              )
            },
            leadingIcon = {
              val icon = when (opt) {
                "Notes" -> Icons.Default.Description
                "Book PDF" -> Icons.Default.MenuBook
                "Thought" -> Icons.Default.EditNote
                "Question Paper" -> Icons.Default.Assignment
                else -> Icons.Default.GridView
              }
              Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = LearnerBlue,
              selectedLabelColor = Color.White,
              selectedLeadingIconColor = Color.White
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.testTag("home_subfilter_${opt.lowercase().replace(" ", "_")}")
          )
        }
      }
    }

    // 4. FEED CONTENT OR REAL EMPTY STATES
    if (displayFeedItems.isEmpty()) {
      item {
        val (emptyTitle, emptySubtitle, emptyIcon) = when (activeFeedTab) {
          "Following" -> Triple(
            "No content from followed users yet",
            "Explore users in the 'Other All' feed or search for educators and learners to follow.",
            Icons.Default.People
          )
          "Follower" -> Triple(
            "No content from your followers yet",
            "When people follow you and publish educational materials, their updates will appear right here.",
            Icons.Default.GroupAdd
          )
          else -> Triple(
            "No $activeSubFilter content available",
            "Be the first to upload and share educational materials with the Learnerport community!",
            Icons.Default.CloudUpload
          )
        }

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          border = CardDefaults.outlinedCardBorder(),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(LearnerBlue.copy(alpha = 0.12f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = emptyIcon,
                contentDescription = null,
                tint = LearnerBlue,
                modifier = Modifier.size(28.dp)
              )
            }
            Text(
              text = emptyTitle,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              textAlign = TextAlign.Center
            )
            Text(
              text = emptySubtitle,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))
            Button(
              onClick = {
                val rType = when (activeSubFilter) {
                  "Book PDF" -> ResourceType.BOOK
                  "Question Paper" -> ResourceType.QUESTION_PAPER
                  "Thought" -> null
                  else -> ResourceType.NOTES
                }
                onUploadClick(rType)
              },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Upload Content", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    } else {
      items(displayFeedItems, key = { item ->
        when (item) {
          is FeedItem.Resource -> "res_${item.item.id}"
          is FeedItem.Thought -> "thought_${item.item.id}"
        }
      }) { feedItem ->
        when (feedItem) {
          is FeedItem.Resource -> {
            ResourceCard(
              resource = feedItem.item,
              onCardClick = { onCardClick(feedItem.item) },
              onReadClick = { onReadClick(feedItem.item) },
              onSaveClick = { onSaveResourceClick(feedItem.item.id) },
              onCreatorClick = { onCreatorClick(feedItem.item.creatorId) }
            )
          }
          is FeedItem.Thought -> {
            ThoughtCard(
              thought = feedItem.item,
              onLikeClick = { onLikeThoughtClick(feedItem.item.id) },
              onCommentClick = { onCommentThoughtClick(feedItem.item) },
              onSaveClick = { onSaveThoughtClick(feedItem.item.id) },
              onAuthorClick = { onCreatorClick(feedItem.item.authorId) }
            )
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
