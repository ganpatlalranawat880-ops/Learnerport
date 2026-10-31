package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LearnerBlue
import com.example.ui.theme.PortBlack

@Composable
fun LearnerportHeader(
  onSearchClick: () -> Unit,
  onNotificationsClick: () -> Unit,
  onSettingsClick: () -> Unit,
  unreadNotificationsCount: Int = 0,
  modifier: Modifier = Modifier
) {
  val isDark = MaterialTheme.colorScheme.background.red < 0.2f
  val portTextColor = if (isDark) Color.White else PortBlack

  Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    shadowElevation = 1.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Mascot Logo + Learnerport Wordmark
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Image(
          painter = painterResource(id = R.drawable.learnerport_logo),
          contentDescription = "Learnerport Logo",
          modifier = Modifier.size(36.dp)
        )

        // Learner in Blue, port in Black/Light
        Text(
          text = buildAnnotatedString {
            withStyle(
              style = SpanStyle(
                color = LearnerBlue,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp
              )
            ) {
              append("Learner")
            }
            withStyle(
              style = SpanStyle(
                color = portTextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
              )
            ) {
              append("port")
            }
          },
          letterSpacing = (-0.5).sp
        )
      }

      // Actions: Search, Notifications, Settings in top corner
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        IconButton(
          onClick = onSearchClick,
          modifier = Modifier
            .size(40.dp)
            .testTag("header_search_button")
        ) {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = MaterialTheme.colorScheme.onSurface
          )
        }

        Box {
          IconButton(
            onClick = onNotificationsClick,
            modifier = Modifier
              .size(40.dp)
              .testTag("header_notifications_button")
          ) {
            Icon(
              imageVector = Icons.Default.Notifications,
              contentDescription = "Notifications",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }

          if (unreadNotificationsCount > 0) {
            Box(
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 4.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(LearnerBlue),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = unreadNotificationsCount.toString(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        IconButton(
          onClick = onSettingsClick,
          modifier = Modifier
            .size(40.dp)
            .testTag("header_settings_button")
        ) {
          Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings",
            tint = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}
