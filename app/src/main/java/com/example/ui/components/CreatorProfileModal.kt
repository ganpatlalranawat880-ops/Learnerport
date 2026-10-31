package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ResourceItem
import com.example.data.model.ThoughtItem
import com.example.data.model.UserProfile
import com.example.ui.theme.LearnerBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorProfileModal(
  creator: UserProfile,
  creatorResources: List<ResourceItem>,
  creatorThoughts: List<ThoughtItem>,
  onClose: () -> Unit,
  onToggleFollow: () -> Unit,
  onCardClick: (ResourceItem) -> Unit,
  onReadClick: (ResourceItem) -> Unit,
  onSaveResourceClick: (String) -> Unit,
  onSaveThoughtClick: (String) -> Unit,
  onLikeThoughtClick: (String) -> Unit,
  onCommentThoughtClick: (ThoughtItem) -> Unit,
  modifier: Modifier = Modifier
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text(creator.displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onClose) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
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
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = CardDefaults.outlinedCardBorder(),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(LearnerBlue.copy(alpha = 0.2f)),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = creator.displayName.take(1).uppercase(),
                    color = LearnerBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                  )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                  Text(
                    text = creator.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "@${creator.username}",
                    fontSize = 13.sp,
                    color = LearnerBlue,
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }

              Button(
                onClick = onToggleFollow,
                shape = RoundedCornerShape(10.dp),
                colors = if (creator.isFollowedByCurrentUser) {
                  ButtonDefaults.outlinedButtonColors()
                } else {
                  ButtonDefaults.buttonColors(containerColor = LearnerBlue)
                }
              ) {
                Text(
                  text = if (creator.isFollowedByCurrentUser) "Following" else "Follow",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                )
              }
            }

            if (creator.bio.isNotBlank()) {
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = creator.bio,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${creatorResources.size}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(text = "Publications", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${creator.followersCount}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(text = "Followers", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${creator.followingCount}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(text = "Following", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }

      item {
        Text("Published by @${creator.username} (${creatorResources.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
      }

      items(creatorResources) { resource ->
        ResourceCard(
          resource = resource,
          onCardClick = { onCardClick(resource) },
          onReadClick = { onReadClick(resource) },
          onSaveClick = { onSaveResourceClick(resource.id) },
          onCreatorClick = { }
        )
      }

      if (creatorThoughts.isNotEmpty()) {
        item {
          Text("Thoughts by @${creator.username} (${creatorThoughts.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        items(creatorThoughts) { thought ->
          ThoughtCard(
            thought = thought,
            onLikeClick = { onLikeThoughtClick(thought.id) },
            onCommentClick = { onCommentThoughtClick(thought) },
            onSaveClick = { onSaveThoughtClick(thought.id) },
            onShareClick = { },
            onAuthorClick = { }
          )
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}
