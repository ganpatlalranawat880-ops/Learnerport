package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BottomNavTab
import com.example.ui.theme.LearnerBlue

private data class NavItemData(
  val tab: BottomNavTab,
  val title: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String
)

@Composable
fun LearnerportBottomBar(
  currentTab: BottomNavTab,
  onTabSelected: (BottomNavTab) -> Unit,
  modifier: Modifier = Modifier
) {
  val items = listOf(
    NavItemData(
      tab = BottomNavTab.HOME,
      title = "Home",
      selectedIcon = Icons.Filled.Home,
      unselectedIcon = Icons.Outlined.Home,
      testTag = "bottom_nav_home"
    ),
    NavItemData(
      tab = BottomNavTab.SAVED_DOWNLOADS,
      title = "Saved",
      selectedIcon = Icons.Filled.Bookmark,
      unselectedIcon = Icons.Outlined.BookmarkBorder,
      testTag = "bottom_nav_saved"
    ),
    NavItemData(
      tab = BottomNavTab.UPLOAD,
      title = "Upload",
      selectedIcon = Icons.Filled.AddCircle,
      unselectedIcon = Icons.Outlined.AddCircleOutline,
      testTag = "bottom_nav_upload"
    ),
    NavItemData(
      tab = BottomNavTab.CHART,
      title = "Chart",
      selectedIcon = Icons.Filled.BarChart,
      unselectedIcon = Icons.Outlined.BarChart,
      testTag = "bottom_nav_chart"
    ),
    NavItemData(
      tab = BottomNavTab.PROFILE,
      title = "Profile",
      selectedIcon = Icons.Filled.Person,
      unselectedIcon = Icons.Outlined.Person,
      testTag = "bottom_nav_profile"
    )
  )

  NavigationBar(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding(),
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp
  ) {
    items.forEach { item ->
      val selected = currentTab == item.tab
      NavigationBarItem(
        selected = selected,
        onClick = { onTabSelected(item.tab) },
        icon = {
          Icon(
            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.title
          )
        },
        label = {
          Text(
            text = item.title,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
          )
        },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = LearnerBlue,
          selectedTextColor = LearnerBlue,
          indicatorColor = LearnerBlue.copy(alpha = 0.15f),
          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.testTag(item.testTag)
      )
    }
  }
}
