package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
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
import com.example.data.model.ResourceItem
import com.example.data.model.ResourceType
import com.example.data.model.ThoughtItem
import com.example.data.model.UserProfile
import com.example.ui.components.ResourceCard
import com.example.ui.components.ThoughtCard
import com.example.ui.theme.LearnerBlue

@Composable
fun ProfileScreen(
  user: UserProfile,
  userResources: List<ResourceItem>,
  userThoughts: List<ThoughtItem>,
  onEditProfileClick: () -> Unit,
  onFollowersClick: () -> Unit,
  onFollowingClick: () -> Unit,
  onCardClick: (ResourceItem) -> Unit,
  onReadClick: (ResourceItem) -> Unit,
  onSaveResourceClick: (String) -> Unit,
  onSaveThoughtClick: (String) -> Unit,
  onLikeThoughtClick: (String) -> Unit,
  onCommentThoughtClick: (ThoughtItem) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf(
    "All (${userResources.size + userThoughts.size})",
    "Notes",
    "Books",
    "Papers",
    "Thoughts"
  )

  val filteredResources = when (selectedTab) {
    1 -> userResources.filter { it.resourceType == ResourceType.NOTES || it.resourceType == ResourceType.CODING_RESOURCE }
    2 -> userResources.filter { it.resourceType == ResourceType.BOOK }
    3 -> userResources.filter { it.resourceType == ResourceType.QUESTION_PAPER }
    4 -> emptyList()
    else -> userResources
  }

  val filteredThoughts = when (selectedTab) {
    4 -> userThoughts
    0 -> userThoughts
    else -> emptyList()
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(10.dp))
      // Profile Header Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              if (user.avatarUrl.isNotBlank()) {
                AsyncImage(
                  model = user.avatarUrl,
                  contentDescription = user.displayName,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                )
              } else {
                Box(
                  modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(LearnerBlue.copy(alpha = 0.15f)),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = user.displayName.take(1).uppercase().ifBlank { "U" },
                    color = LearnerBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                  )
                }
              }

              Spacer(modifier = Modifier.width(14.dp))

              Column {
                Text(
                  text = user.displayName.ifBlank { "Learner User" },
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = if (user.username.isNotBlank()) "@${user.username}" else "@user",
                  fontSize = 13.sp,
                  color = LearnerBlue,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            OutlinedButton(
              onClick = onEditProfileClick,
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("edit_profile_button")
            ) {
              Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Edit", fontSize = 12.sp)
            }
          }

          // Educational Qualification & Course
          if (user.educationalLevel.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = LearnerBlue,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "${user.educationalLevel} • ${user.courseOrDegree}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }

          // Optional Educational Institution
          if (user.institution.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = user.institution,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // Location (Country & State)
          if (user.country.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = LearnerBlue,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (user.state.isNotBlank()) "${user.state}, ${user.country}" else user.country,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // Bio
          if (user.bio.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = user.bio,
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurface,
              lineHeight = 18.sp
            )
          }

          Spacer(modifier = Modifier.height(16.dp))
          HorizontalDivider()
          Spacer(modifier = Modifier.height(12.dp))

          // Statistics: Followers, Following, Published (Clickable!)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.clickable { selectedTab = 0 }
            ) {
              Text(text = "${userResources.size + userThoughts.size}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
              Text(text = "Published", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .clickable { onFollowersClick() }
                .testTag("followers_stat_button")
            ) {
              Text(text = "${user.followersCount}", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = LearnerBlue)
              Text(text = "Followers", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .clickable { onFollowingClick() }
                .testTag("following_stat_button")
            ) {
              Text(text = "${user.followingCount}", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = LearnerBlue)
              Text(text = "Following", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
    }

    // Published Tabs
    item {
      ScrollableTabRow(
        selectedTabIndex = selectedTab,
        edgePadding = 0.dp,
        containerColor = Color.Transparent,
        contentColor = LearnerBlue
      ) {
        tabs.forEachIndexed { index, tabTitle ->
          Tab(
            selected = selectedTab == index,
            onClick = { selectedTab = index },
            text = {
              Text(
                text = tabTitle,
                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
              )
            }
          )
        }
      }
    }

    // Empty state or list of user items
    if (filteredResources.isEmpty() && filteredThoughts.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = CardDefaults.outlinedCardBorder(),
          modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "No content published in this category yet.",
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    } else {
      items(filteredResources, key = { it.id }) { resource ->
        ResourceCard(
          resource = resource,
          onCardClick = { onCardClick(resource) },
          onReadClick = { onReadClick(resource) },
          onSaveClick = { onSaveResourceClick(resource.id) },
          onCreatorClick = {}
        )
      }

      items(filteredThoughts, key = { it.id }) { thought ->
        ThoughtCard(
          thought = thought,
          onLikeClick = { onLikeThoughtClick(thought.id) },
          onCommentClick = { onCommentThoughtClick(thought) },
          onSaveClick = { onSaveThoughtClick(thought.id) },
          onAuthorClick = {}
        )
      }
    }

    item {
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}
