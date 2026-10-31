package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationItem
import com.example.data.model.NotificationType
import com.example.ui.theme.LearnerAccentGold
import com.example.ui.theme.LearnerAccentGreen
import com.example.ui.theme.LearnerBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
  notifications: List<NotificationItem>,
  onClose: () -> Unit,
  onNotificationClick: (NotificationItem) -> Unit,
  onMarkAllRead: () -> Unit,
  modifier: Modifier = Modifier
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Notifications", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onClose) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
          }
        },
        actions = {
          if (notifications.any { !it.isRead }) {
            TextButton(onClick = onMarkAllRead) {
              Text("Mark all read", fontSize = 12.sp, color = LearnerBlue, fontWeight = FontWeight.SemiBold)
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    }
  ) { padding ->
    if (notifications.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Outlined.NotificationsNone,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text("No notifications yet", fontWeight = FontWeight.Bold, fontSize = 15.sp)
          Text("You'll see activity updates from followed creators here.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(notifications) { notif ->
          val icon = when (notif.type) {
            NotificationType.PUBLICATION -> Icons.Default.MenuBook
            NotificationType.COMMENT -> Icons.Outlined.ChatBubbleOutline
            NotificationType.RATING -> Icons.Default.Star
            NotificationType.FOLLOW -> Icons.Default.PersonAdd
            NotificationType.SYSTEM -> Icons.Default.Info
          }
          val iconColor = when (notif.type) {
            NotificationType.PUBLICATION -> LearnerBlue
            NotificationType.COMMENT -> Color(0xFF6366F1)
            NotificationType.RATING -> LearnerAccentGold
            NotificationType.FOLLOW -> LearnerAccentGreen
            NotificationType.SYSTEM -> MaterialTheme.colorScheme.primary
          }

          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable { onNotificationClick(notif) },
            colors = CardDefaults.cardColors(
              containerColor = if (!notif.isRead) LearnerBlue.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface
            ),
            border = CardDefaults.outlinedCardBorder()
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.Top
            ) {
              Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                }
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = notif.title,
                    fontWeight = if (!notif.isRead) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                  )
                  Text(
                    text = notif.formattedTime,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = notif.message,
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  lineHeight = 16.sp
                )
              }
            }
          }
        }
      }
    }
  }
}
