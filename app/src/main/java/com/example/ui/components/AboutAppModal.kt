package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LearnerBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppModal(
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val featuresList = listOf(
    "Educational Notes (Subject, Topic & Academic Year)",
    "Book PDFs with Author & Publication Details",
    "Previous-Year Question Papers & Sample Tests",
    "Educational Thoughts & Knowledge Reflections",
    "PDF Uploading with Category-Specific Fields",
    "Interactive In-App PDF Reading & Zooming",
    "Offline PDF Downloading & Device Storage",
    "Native PDF & Content Device Sharing",
    "Custom PDF Cover Selection (Templates & Gallery)",
    "Global Search across Resources, Topics & Users",
    "Class, Course & Subject Organization",
    "User Registration & Secure Authentication",
    "Personalized User Profiles & Qualifications",
    "Profile Photographs (Camera & Gallery)",
    "Publishing, Managing & Deleting Own Uploads",
    "Real Followers and Following Relationships",
    "Light Mode & Dark Mode with Local Persistence",
    "Security PIN / App Lock Protection",
    "Content Reporting & Copyright Infringement Notice",
    "Responsive Premium Apple iOS-Inspired Interface"
  )

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("About Learnerport", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close")
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
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      // App Branding Header (Transparent logo, no white box)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Image(
          painter = painterResource(id = R.drawable.learnerport_logo),
          contentDescription = "Learnerport Logo",
          modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
          Text(
            text = "Learnerport",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Version 1.0 • Educational Content-Sharing Platform",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // 1. About the Application
      AboutSection(title = "ABOUT THE APPLICATION") {
        Text(
          text = "Learnerport is an educational content-sharing platform designed to empower students, educators, and lifelong learners to discover, read, organize, and share high-quality educational resources in one convenient place.\n\nWhether preparing for school board examinations, undergraduate university degrees, competitive entrance tests, or pursuing self-directed programming and science skills, Learnerport offers structured, verified, and community-driven learning materials.",
          fontSize = 13.sp,
          lineHeight = 20.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      // 2. Main Features
      AboutSection(title = "MAIN FEATURES") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          featuresList.forEach { feat ->
            Row(verticalAlignment = Alignment.Top) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = LearnerBlue,
                modifier = Modifier.size(16.dp).padding(top = 2.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = feat,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }

      // 3. User Community
      AboutSection(title = "USER COMMUNITY") {
        Text(
          text = "The Learnerport platform allows genuine registered users to publish original educational notes, textbooks, and thoughts. Every piece of user content is associated with a real authenticated author. Learnerport does not generate fake followers, artificial user accounts, or fabricated engagement statistics.",
          fontSize = 13.sp,
          lineHeight = 20.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      // 4. Responsible Publishing
      AboutSection(title = "RESPONSIBLE PUBLISHING") {
        Text(
          text = "Users must respect intellectual property rights and obtain all necessary permissions before publishing material belonging to others. Uploaders bear sole responsibility for ensuring they possess authorized rights or valid open educational distribution licenses.",
          fontSize = 13.sp,
          lineHeight = 20.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      // 5. Privacy and Security
      AboutSection(title = "PRIVACY & SECURITY") {
        Text(
          text = "Your account data, profile details, and educational contributions are secured through standardized authentication and database row-level security. We also offer an optional local 4-Digit Security PIN (App Lock) to safeguard your app on-device.",
          fontSize = 13.sp,
          lineHeight = 20.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Final Signature (EXACT REQUIREMENT: "By Satish Ranawat" at the very bottom)
      Column(
        modifier = Modifier.fillMaxWidth().testTag("about_app_signature"),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        HorizontalDivider(modifier = Modifier.width(120.dp), color = LearnerBlue.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "By Satish Ranawat",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = LearnerBlue,
          letterSpacing = 0.5.sp
        )
        Text(
          text = "Creator & Lead Developer",
          fontSize = 11.sp,
          fontStyle = FontStyle.Italic,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun AboutSection(
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
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = CardDefaults.outlinedCardBorder(),
      modifier = Modifier.fillMaxWidth()
    ) {
      Box(modifier = Modifier.padding(16.dp)) {
        content()
      }
    }
  }
}
