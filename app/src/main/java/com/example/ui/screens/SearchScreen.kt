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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.components.ResourceCard
import com.example.ui.components.ThoughtCard
import com.example.ui.theme.LearnerBlue

private sealed class SearchFeedItem(val timestamp: Long) {
  data class Resource(val item: ResourceItem) : SearchFeedItem(item.timestamp)
  data class Thought(val item: ThoughtItem) : SearchFeedItem(item.timestamp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
  searchQuery: String,
  onQueryChange: (String) -> Unit,
  selectedTab: SearchTab,
  onTabSelected: (SearchTab) -> Unit,
  resources: List<ResourceItem>,
  thoughts: List<ThoughtItem>,
  creators: List<UserProfile>,
  onClose: () -> Unit,
  onCardClick: (ResourceItem) -> Unit,
  onReadClick: (ResourceItem) -> Unit,
  onSaveResourceClick: (String) -> Unit,
  onSaveThoughtClick: (String) -> Unit,
  onLikeThoughtClick: (String) -> Unit,
  onCommentThoughtClick: (ThoughtItem) -> Unit,
  onCreatorClick: (UserProfile) -> Unit,
  onToggleFollow: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  // Main tabs: "PDFs & Notes" and "Profiles"
  var currentMainTab by remember {
    mutableStateOf(if (selectedTab == SearchTab.USERS) "Profiles" else "PDFs & Notes")
  }

  // 4 Sub-Categories inside "PDFs & Notes": "All", "Notes", "Book PDF", "Thought", "Question Paper"
  var selectedSubCategory by remember {
    mutableStateOf(
      when (selectedTab) {
        SearchTab.BOOKS -> "Book PDF"
        SearchTab.QUESTION_PAPERS -> "Question Paper"
        SearchTab.THOUGHTS -> "Thought"
        SearchTab.NOTES -> "Notes"
        else -> "All"
      }
    )
  }
  val subCategories = listOf("All", "Notes", "Book PDF", "Thought", "Question Paper")

  val q = searchQuery.trim().lowercase()

  // Base filtered collections
  val matchingNotes = remember(resources, q) {
    resources.filter {
      (it.resourceType == ResourceType.NOTES || it.resourceType == ResourceType.CODING_RESOURCE) &&
        (q.isEmpty() || it.title.lowercase().contains(q) || it.subject.lowercase().contains(q) ||
          it.topic.lowercase().contains(q) || it.educationalLevel.lowercase().contains(q) ||
          it.tags.any { tag -> tag.lowercase().contains(q) })
    }
  }

  val matchingBooks = remember(resources, q) {
    resources.filter {
      it.resourceType == ResourceType.BOOK &&
        (q.isEmpty() || it.title.lowercase().contains(q) || it.subject.lowercase().contains(q) ||
          it.topic.lowercase().contains(q) || it.creatorDisplayName.lowercase().contains(q) ||
          it.tags.any { tag -> tag.lowercase().contains(q) })
    }
  }

  val matchingQuestionPapers = remember(resources, q) {
    resources.filter {
      it.resourceType == ResourceType.QUESTION_PAPER &&
        (q.isEmpty() || it.title.lowercase().contains(q) || it.subject.lowercase().contains(q) ||
          it.examinationYear?.contains(q) == true || it.boardOrUniversity?.lowercase()?.contains(q) == true ||
          it.paperType?.lowercase()?.contains(q) == true || it.tags.any { tag -> tag.lowercase().contains(q) })
    }
  }

  val matchingThoughts = remember(thoughts, q) {
    thoughts.filter {
      q.isEmpty() || it.content.lowercase().contains(q) || it.authorDisplayName.lowercase().contains(q) ||
        it.authorUsername.lowercase().contains(q) || it.title?.lowercase()?.contains(q) == true ||
        it.tags.any { tag -> tag.lowercase().contains(q) }
    }
  }

  val matchingUsers = remember(creators, q) {
    creators.filter {
      q.isEmpty() || it.username.lowercase().contains(q) || it.displayName.lowercase().contains(q) ||
        it.bio.lowercase().contains(q) || it.educationalLevel.lowercase().contains(q)
    }
  }

  // Active items for "PDFs & Notes" based on selectedSubCategory
  val pdfsAndNotesItems = remember(selectedSubCategory, matchingNotes, matchingBooks, matchingQuestionPapers, matchingThoughts) {
    val list = mutableListOf<SearchFeedItem>()
    when (selectedSubCategory) {
      "Notes" -> matchingNotes.forEach { list.add(SearchFeedItem.Resource(it)) }
      "Book PDF" -> matchingBooks.forEach { list.add(SearchFeedItem.Resource(it)) }
      "Question Paper" -> matchingQuestionPapers.forEach { list.add(SearchFeedItem.Resource(it)) }
      "Thought" -> matchingThoughts.forEach { list.add(SearchFeedItem.Thought(it)) }
      else -> {
        // "All"
        matchingNotes.forEach { list.add(SearchFeedItem.Resource(it)) }
        matchingBooks.forEach { list.add(SearchFeedItem.Resource(it)) }
        matchingQuestionPapers.forEach { list.add(SearchFeedItem.Resource(it)) }
        matchingThoughts.forEach { list.add(SearchFeedItem.Thought(it)) }
      }
    }
    list.sortedByDescending { it.timestamp }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 6.dp)
        ) {
          // Search Input Bar
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(onClick = onClose) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            OutlinedTextField(
              value = searchQuery,
              onValueChange = onQueryChange,
              placeholder = { Text("serch users ,pdf,notes.....", fontSize = 13.sp) },
              leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = LearnerBlue, modifier = Modifier.size(18.dp))
              },
              trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                  IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                  }
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("search_text_field")
            )
          }

          // Main Tabs: "PDFs & Notes" and "Profiles"
          TabRow(
            selectedTabIndex = if (currentMainTab == "PDFs & Notes") 0 else 1,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = LearnerBlue,
            divider = {}
          ) {
            Tab(
              selected = currentMainTab == "PDFs & Notes",
              onClick = {
                currentMainTab = "PDFs & Notes"
                onTabSelected(SearchTab.PDFS_AND_NOTES)
              },
              text = {
                Text(
                  text = "PDFs & Notes (${matchingNotes.size + matchingBooks.size + matchingQuestionPapers.size + matchingThoughts.size})",
                  fontWeight = if (currentMainTab == "PDFs & Notes") FontWeight.Bold else FontWeight.Medium,
                  fontSize = 13.sp
                )
              }
            )
            Tab(
              selected = currentMainTab == "Profiles",
              onClick = {
                currentMainTab = "Profiles"
                onTabSelected(SearchTab.USERS)
              },
              text = {
                Text(
                  text = "Profiles (${matchingUsers.size})",
                  fontWeight = if (currentMainTab == "Profiles") FontWeight.Bold else FontWeight.Medium,
                  fontSize = 13.sp
                )
              }
            )
          }

          // 4 Sub-Category options/buttons below "PDFs & Notes" tab
          if (currentMainTab == "PDFs & Notes") {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              subCategories.forEach { subCat ->
                val isSelected = selectedSubCategory == subCat
                FilterChip(
                  selected = isSelected,
                  onClick = { selectedSubCategory = subCat },
                  label = {
                    Text(
                      text = subCat,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                      fontSize = 12.sp
                    )
                  },
                  leadingIcon = {
                    val icon = when (subCat) {
                      "Notes" -> Icons.Default.Description
                      "Book PDF" -> Icons.Default.MenuBook
                      "Thought" -> Icons.Default.EditNote
                      "Question Paper" -> Icons.Default.Assignment
                      else -> Icons.Default.GridView
                    }
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp))
                  },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = LearnerBlue,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                  ),
                  shape = RoundedCornerShape(16.dp),
                  modifier = Modifier.testTag("search_subcat_${subCat.lowercase().replace(" ", "_")}")
                )
              }
            }
          }
        }
      }
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      if (currentMainTab == "PDFs & Notes") {
        if (pdfsAndNotesItems.isEmpty()) {
          SearchEmptyState("No $selectedSubCategory matching \"$searchQuery\"")
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            items(pdfsAndNotesItems, key = { item ->
              when (item) {
                is SearchFeedItem.Resource -> "res_${item.item.id}"
                is SearchFeedItem.Thought -> "thought_${item.item.id}"
              }
            }) { feedItem ->
              when (feedItem) {
                is SearchFeedItem.Resource -> {
                  ResourceCard(
                    resource = feedItem.item,
                    onCardClick = { onCardClick(feedItem.item) },
                    onReadClick = { onReadClick(feedItem.item) },
                    onSaveClick = { onSaveResourceClick(feedItem.item.id) },
                    onCreatorClick = {
                      val cr = creators.firstOrNull { it.id == feedItem.item.creatorId }
                      if (cr != null) onCreatorClick(cr)
                    }
                  )
                }
                is SearchFeedItem.Thought -> {
                  ThoughtCard(
                    thought = feedItem.item,
                    onLikeClick = { onLikeThoughtClick(feedItem.item.id) },
                    onCommentClick = { onCommentThoughtClick(feedItem.item) },
                    onSaveClick = { onSaveThoughtClick(feedItem.item.id) },
                    onAuthorClick = {
                      val cr = creators.firstOrNull { it.id == feedItem.item.authorId }
                      if (cr != null) onCreatorClick(cr)
                    }
                  )
                }
              }
            }
          }
        }
      } else {
        // "Profiles"
        if (matchingUsers.isEmpty()) {
          SearchEmptyState("No users matching \"$searchQuery\"")
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(matchingUsers, key = { it.id }) { user ->
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onCreatorClick(user) }
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  if (user.avatarUrl.isNotBlank()) {
                    AsyncImage(
                      model = user.avatarUrl,
                      contentDescription = user.displayName,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                    )
                  } else {
                    Surface(
                      shape = CircleShape,
                      color = LearnerBlue.copy(alpha = 0.15f),
                      modifier = Modifier.size(46.dp)
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Text(
                          text = user.displayName.take(1).uppercase(),
                          fontWeight = FontWeight.Bold,
                          color = LearnerBlue,
                          fontSize = 20.sp
                        )
                      }
                    }
                  }

                  Spacer(modifier = Modifier.width(12.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Text(text = user.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "@${user.username}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (user.educationalLevel.isNotBlank()) {
                      Text(text = "${user.educationalLevel} • ${user.courseOrDegree}", fontSize = 11.sp, color = LearnerBlue)
                    }
                  }

                  if (!user.isCurrentUser) {
                    Button(
                      onClick = { onToggleFollow(user.id) },
                      shape = RoundedCornerShape(16.dp),
                      colors = if (user.isFollowedByCurrentUser) ButtonDefaults.filledTonalButtonColors() else ButtonDefaults.buttonColors(containerColor = LearnerBlue),
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                      modifier = Modifier.height(34.dp)
                    ) {
                      Text(if (user.isFollowedByCurrentUser) "Following" else "Follow", fontSize = 12.sp)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SearchEmptyState(message: String) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(LearnerBlue.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = null,
          tint = LearnerBlue,
          modifier = Modifier.size(26.dp)
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "No Results Found",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = message,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
    }
  }
}
