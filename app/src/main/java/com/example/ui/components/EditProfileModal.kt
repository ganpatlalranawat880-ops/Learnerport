package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.data.remote.FirebaseManager
import com.example.data.remote.SupabaseClient
import com.example.data.util.ImageCompressor
import com.example.ui.theme.LearnerBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileModal(
  currentUser: UserProfile,
  onClose: () -> Unit,
  onSave: (
    displayName: String,
    username: String,
    bio: String,
    educationalLevel: String,
    courseOrDegree: String,
    institution: String,
    avatarUrl: String
  ) -> Boolean,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var displayName by remember { mutableStateOf(currentUser.displayName) }
  var username by remember { mutableStateOf(currentUser.username) }
  var bio by remember { mutableStateOf(currentUser.bio) }
  var educationalLevel by remember { mutableStateOf(currentUser.educationalLevel.ifBlank { "Undergraduate Degree" }) }
  var courseOrDegree by remember { mutableStateOf(currentUser.courseOrDegree.ifBlank { "B.Tech. Computer Science" }) }
  var institution by remember { mutableStateOf(currentUser.institution) }
  var avatarUrl by remember { mutableStateOf(currentUser.avatarUrl) }

  var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
  var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
  var isUploadingPhoto by remember { mutableStateOf(false) }

  var errorMessage by remember { mutableStateOf<String?>(null) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      selectedImageUri = uri
      val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
      imageBytes = bytes
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Edit Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onClose) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
          }
        },
        actions = {
          TextButton(
            onClick = {
              if (displayName.isBlank()) {
                errorMessage = "Display name cannot be empty."
                return@TextButton
              }
              if (username.isBlank()) {
                errorMessage = "Username cannot be empty."
                return@TextButton
              }

              coroutineScope.launch {
                var finalAvatar = avatarUrl
                if (imageBytes != null) {
                  isUploadingPhoto = true
                  val compressed = ImageCompressor.compressAvatar(imageBytes!!)
                  val fbRes = FirebaseManager.uploadAvatarImage(currentUser.id, compressed)
                  isUploadingPhoto = false
                  if (fbRes.isSuccess) {
                    finalAvatar = fbRes.getOrNull()?.first ?: finalAvatar
                  } else {
                    val res = SupabaseClient.uploadAvatar(currentUser.id, compressed)
                    if (res.isSuccess) {
                      finalAvatar = res.getOrNull() ?: finalAvatar
                    }
                  }
                }

                val success = onSave(
                  displayName.trim(),
                  username.trim().lowercase().removePrefix("@"),
                  bio.trim(),
                  educationalLevel.trim(),
                  courseOrDegree.trim(),
                  institution.trim(),
                  finalAvatar
                )
                if (!success) {
                  errorMessage = "Username @$username is already taken. Please choose another."
                }
              }
            },
            enabled = !isUploadingPhoto,
            modifier = Modifier.testTag("save_profile_button")
          ) {
            if (isUploadingPhoto) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
              Text("Save", fontWeight = FontWeight.Bold, color = LearnerBlue, fontSize = 15.sp)
            }
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
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Avatar
      Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(LearnerBlue.copy(alpha = 0.15f))
            .clickable {
              photoPickerLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                  ActivityResultContracts.PickVisualMedia.ImageOnly
                )
              )
            },
          contentAlignment = Alignment.Center
        ) {
          when {
            selectedImageUri != null -> {
              AsyncImage(
                model = selectedImageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
            }
            avatarUrl.isNotBlank() -> {
              AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
            }
            else -> {
              Text(
                text = displayName.take(1).uppercase().ifBlank { "U" },
                color = LearnerBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
              )
            }
          }
        }
      }

      Text(
        text = "Tap photo to change from gallery",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.align(Alignment.CenterHorizontally)
      )

      if (errorMessage != null) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = errorMessage!!,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 12.sp,
            modifier = Modifier.padding(12.dp)
          )
        }
      }

      OutlinedTextField(
        value = displayName,
        onValueChange = { displayName = it },
        label = { Text("Full Name *") },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("edit_display_name_input")
      )

      // Username is permanently locked
      OutlinedTextField(
        value = username,
        onValueChange = {},
        readOnly = true,
        label = { Text("Username (Locked)") },
        prefix = { Text("@") },
        trailingIcon = {
          Icon(Icons.Default.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        supportingText = {
          Text("Username is permanently locked and cannot be edited.")
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("edit_username_input")
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Dynamic Connection Selector
      Text(
        text = "Your Connection (School / College / Other)",
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf("School", "College", "Other").forEach { conn ->
          val isSel = educationalLevel.equals(conn, ignoreCase = true)
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isSel) LearnerBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
              .weight(1f)
              .clickable {
                educationalLevel = conn
                if (conn == "School" && !courseOrDegree.startsWith("Class")) {
                  courseOrDegree = "Class 10"
                }
              }
          ) {
            Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
              Text(
                text = conn,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      when {
        educationalLevel.equals("School", ignoreCase = true) -> {
          OutlinedTextField(
            value = courseOrDegree,
            onValueChange = { courseOrDegree = it },
            label = { Text("Class (e.g. Class 10, Class 12)") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )
        }
        educationalLevel.equals("College", ignoreCase = true) -> {
          OutlinedTextField(
            value = courseOrDegree,
            onValueChange = { courseOrDegree = it },
            label = { Text("Degree / Program (e.g. B.Tech, BCA, MBBS)") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )
        }
        else -> {
          OutlinedTextField(
            value = courseOrDegree,
            onValueChange = { courseOrDegree = it },
            label = { Text("Occupation / Field") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )
        }
      }

      OutlinedTextField(
        value = bio,
        onValueChange = { if (it.length <= 250) bio = it },
        label = { Text("Bio (max 250 chars)") },
        minLines = 3,
        maxLines = 5,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("edit_bio_input")
      )

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}
