package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ThemeMode
import com.example.data.model.UserProfile
import com.example.data.security.AppLockManager
import com.example.ui.components.*
import com.example.ui.theme.LearnerBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  currentUser: UserProfile,
  themeMode: ThemeMode,
  onThemeModeChange: (ThemeMode) -> Unit,
  onEditProfileClick: () -> Unit,
  onLogoutClick: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var showSecurityPinModal by remember { mutableStateOf(false) }
  var showMainInfoModal by remember { mutableStateOf(false) }
  var showAboutAppModal by remember { mutableStateOf(false) }
  var showTermsModal by remember { mutableStateOf(false) }
  var showPrivacyModal by remember { mutableStateOf(false) }
  var showHelpModal by remember { mutableStateOf(false) }
  var showReportModal by remember { mutableStateOf(false) }
  var showLogoutConfirmDialog by remember { mutableStateOf(false) }
  var showBackendConfigModal by remember { mutableStateOf(false) }

  val isAppLockActive = remember { AppLockManager.isAppLockEnabled(context) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onClose) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close Settings")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      // User Profile Header Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onEditProfileClick() }
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (currentUser.avatarUrl.isNotBlank()) {
            AsyncImage(
              model = currentUser.avatarUrl,
              contentDescription = currentUser.displayName,
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
            )
          } else {
            Surface(
              shape = CircleShape,
              color = LearnerBlue.copy(alpha = 0.15f),
              modifier = Modifier.size(52.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = currentUser.displayName.take(1).uppercase().ifBlank { "U" },
                  fontWeight = FontWeight.Bold,
                  fontSize = 22.sp,
                  color = LearnerBlue
                )
              }
            }
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = currentUser.displayName.ifBlank { "Learner User" },
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = if (currentUser.username.isNotBlank()) "@${currentUser.username}" else "Complete your profile",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (currentUser.educationalLevel.isNotBlank()) {
              Text(
                text = "${currentUser.educationalLevel} • ${currentUser.courseOrDegree}",
                fontSize = 11.sp,
                color = LearnerBlue
              )
            }
          }

          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit Profile",
            tint = LearnerBlue,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      // 1. ACCOUNT & PROFILE
      SettingsSection(title = "ACCOUNT") {
        SettingsRow(
          icon = Icons.Default.Person,
          title = "Edit Profile",
          subtitle = "Change name, photo, bio, educational level & qualification",
          onClick = onEditProfileClick
        )
      }

      // 2. APPEARANCE (ONLY Light Mode and Dark Mode)
      SettingsSection(title = "APPEARANCE") {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "Theme Selection",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
          )
          Text(
            text = "Choose your preferred appearance mode. Saved locally on this device.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Light Mode Option
            FilterChip(
              selected = themeMode == ThemeMode.LIGHT,
              onClick = { onThemeModeChange(ThemeMode.LIGHT) },
              label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Light Mode", fontWeight = FontWeight.SemiBold)
                }
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = LearnerBlue,
                selectedLabelColor = Color.White
              ),
              modifier = Modifier.weight(1f).testTag("theme_light_chip")
            )

            // Dark Mode Option
            FilterChip(
              selected = themeMode == ThemeMode.DARK,
              onClick = { onThemeModeChange(ThemeMode.DARK) },
              label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Dark Mode", fontWeight = FontWeight.SemiBold)
                }
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = LearnerBlue,
                selectedLabelColor = Color.White
              ),
              modifier = Modifier.weight(1f).testTag("theme_dark_chip")
            )
          }
        }
      }

      // 3. SECURITY & PROTECTION
      SettingsSection(title = "SECURITY") {
        SettingsRow(
          icon = Icons.Default.Lock,
          title = "Security PIN / App Lock",
          subtitle = if (isAppLockActive) "Protected with 4-digit PIN (Tap to change/disable)" else "Set 4-digit PIN to secure application access",
          onClick = { showSecurityPinModal = true }
        )
      }

      // 4. INFORMATION & POLICIES
      SettingsSection(title = "INFORMATION & POLICIES") {
        SettingsRow(
          icon = Icons.Default.Gavel,
          title = "Main Information",
          subtitle = "महत्वपूर्ण सूचना एवं अपलोड संबंधी जिम्मेदारी (By Satish Ranawat)",
          onClick = { showMainInfoModal = true }
        )
        HorizontalDivider()
        SettingsRow(
          icon = Icons.Default.Info,
          title = "About App",
          subtitle = "Application details, features & signature",
          onClick = { showAboutAppModal = true }
        )
        HorizontalDivider()
        SettingsRow(
          icon = Icons.Default.Article,
          title = "Terms and Conditions",
          subtitle = "Usage rules, distribution license & author terms",
          onClick = { showTermsModal = true }
        )
        HorizontalDivider()
        SettingsRow(
          icon = Icons.Default.Security,
          title = "Privacy Policy",
          subtitle = "Personal data protection & encryption practices",
          onClick = { showPrivacyModal = true }
        )
        HorizontalDivider()
        SettingsRow(
          icon = Icons.Default.Help,
          title = "Help and Support",
          subtitle = "Frequently asked questions & assistance",
          onClick = { showHelpModal = true }
        )
      }

      // 5. SHARE & ACTIONS
      SettingsSection(title = "ACTIONS") {
        SettingsRow(
          icon = Icons.Default.Share,
          title = "Share App",
          subtitle = "Share Learnerport with classmates & colleagues",
          onClick = {
            val sendIntent = Intent().apply {
              action = Intent.ACTION_SEND
              putExtra(
                Intent.EXTRA_TEXT,
                "Explore and share educational notes, textbooks, and previous-year question papers on Learnerport! Join the learning platform: https://learnerport.org"
              )
              type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Learnerport App"))
          }
        )
        HorizontalDivider()
        SettingsRow(
          icon = Icons.Default.Logout,
          title = "Log Out",
          subtitle = "Sign out of your Learnerport account",
          iconTint = MaterialTheme.colorScheme.error,
          onClick = { showLogoutConfirmDialog = true }
        )
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  // Modals
  if (showSecurityPinModal) {
    SecurityPinModal(onDismiss = { showSecurityPinModal = false })
  }

  if (showMainInfoModal) {
    MainInformationModal(
      onClose = { showMainInfoModal = false },
      onOpenReportContent = {
        showMainInfoModal = false
        showReportModal = true
      }
    )
  }

  if (showAboutAppModal) {
    AboutAppModal(onClose = { showAboutAppModal = false })
  }

  if (showTermsModal) {
    TermsModal(onClose = { showTermsModal = false })
  }

  if (showPrivacyModal) {
    PrivacyPolicyModal(onClose = { showPrivacyModal = false })
  }

  if (showHelpModal) {
    HelpSupportModal(onClose = { showHelpModal = false })
  }

  if (showReportModal) {
    ReportContentModal(
      targetTitle = "General Platform Content",
      onDismiss = { showReportModal = false },
      onSubmitReport = { _, _ -> }
    )
  }

  if (showLogoutConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showLogoutConfirmDialog = false },
      title = { Text("Log Out from Learnerport?", fontWeight = FontWeight.Bold) },
      text = { Text("Are you sure you want to log out? You will need your email and password or Google login to access your account again.") },
      confirmButton = {
        Button(
          onClick = {
            showLogoutConfirmDialog = false
            onLogoutClick()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_logout_button")
        ) {
          Text("Log Out")
        }
      },
      dismissButton = {
        TextButton(onClick = { showLogoutConfirmDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun SettingsSection(
  title: String,
  content: @Composable () -> Unit
) {
  Column {
    Text(
      text = title,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = LearnerBlue,
      letterSpacing = 1.sp,
      modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = CardDefaults.outlinedCardBorder(),
      modifier = Modifier.fillMaxWidth()
    ) {
      content()
    }
  }
}

@Composable
fun SettingsRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  iconTint: Color = LearnerBlue,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = iconTint.copy(alpha = 0.1f),
      modifier = Modifier.size(38.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
      }
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
      Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Icon(
      imageVector = Icons.Default.ChevronRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )
  }
}
