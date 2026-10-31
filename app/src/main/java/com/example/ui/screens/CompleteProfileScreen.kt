package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CountryStateData
import com.example.data.model.UserProfile
import com.example.data.remote.FirebaseManager
import com.example.data.remote.SupabaseClient
import com.example.data.repository.LearnerportRepository
import com.example.ui.theme.LearnerAccentGreen
import com.example.ui.theme.LearnerAccentRed
import com.example.ui.theme.LearnerBlue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
  initialDisplayName: String = "",
  initialEmail: String = "",
  onProfileCompleted: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  val currentSession = SupabaseClient.currentSession.collectAsState().value
  val firebaseUser = FirebaseManager.currentUser.collectAsState().value
  val firebaseProfile = FirebaseManager.userProfile.collectAsState().value

  val authenticatedAccount = initialEmail.ifBlank {
    firebaseUser?.email ?: currentSession?.email ?: (firebaseUser?.phoneNumber ?: "")
  }

  val defaultName = initialDisplayName.ifBlank {
    firebaseProfile?.displayName?.ifBlank { firebaseUser?.displayName }
      ?: currentSession?.displayName
      ?: if (authenticatedAccount.contains("@")) {
        authenticatedAccount.substringBefore("@").replace(".", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
      } else {
        "Learner"
      }
  }

  val initialUname = firebaseProfile?.username?.ifBlank {
    if (authenticatedAccount.contains("@")) {
      authenticatedAccount.substringBefore("@").lowercase().replace(".", "_")
    } else {
      "learner_" + (firebaseUser?.phoneNumber?.takeLast(4) ?: "user")
    }
  } ?: currentSession?.username ?: ""

  // Form Fields
  var displayName by remember { mutableStateOf(defaultName) }
  var username by remember { mutableStateOf(initialUname) }
  var dateOfBirth by remember { mutableStateOf(firebaseProfile?.dateOfBirth ?: currentSession?.dateOfBirth ?: "") }

  // Connection selection: "School", "College", "Other"
  var selectedConnection by remember {
    mutableStateOf(
      when {
        firebaseProfile?.connectionType?.isNotBlank() == true -> firebaseProfile.connectionType
        currentSession?.educationalLevel?.contains("School", ignoreCase = true) == true -> "School"
        currentSession?.educationalLevel?.contains("College", ignoreCase = true) == true -> "College"
        else -> "College"
      }
    )
  }
  var selectedSchoolClass by remember { mutableStateOf(firebaseProfile?.connectionDetail?.ifBlank { "Class 10" } ?: "Class 10") }
  var collegeDegree by remember { mutableStateOf(firebaseProfile?.connectionDetail ?: currentSession?.courseOrDegree ?: "B.Tech Computer Science") }
  var otherOccupation by remember { mutableStateOf(firebaseProfile?.connectionDetail ?: "") }

  // Location: Cascading Country & State
  var selectedCountry by remember { mutableStateOf(firebaseProfile?.country?.ifBlank { "India" } ?: "India") }
  var selectedState by remember { mutableStateOf(firebaseProfile?.state?.ifBlank { "Delhi" } ?: "Delhi") }
  var isCountryDropdownOpen by remember { mutableStateOf(false) }
  var isStateDropdownOpen by remember { mutableStateOf(false) }
  var countrySearchQuery by remember { mutableStateOf("") }
  var stateSearchQuery by remember { mutableStateOf("") }

  // School Class dropdown
  var isClassDropdownOpen by remember { mutableStateOf(false) }
  val schoolClasses = (1..12).map { "Class $it" }

  // Bio (Optional)
  var bio by remember { mutableStateOf(firebaseProfile?.bio ?: currentSession?.bio ?: "") }

  // Avatar Image selection
  var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
  var imageBytes by remember { mutableStateOf<ByteArray?>(null) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      selectedImageUri = uri
      try {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        imageBytes = bytes
      } catch (e: Exception) {
        // Fallback
      }
    }
  }

  // Real-time Username Validation
  var isCheckingUsername by remember { mutableStateOf(false) }
  var isUsernameAvailable by remember { mutableStateOf<Boolean?>(null) }
  var usernameCheckJob by remember { mutableStateOf<Job?>(null) }

  fun checkUsername(uname: String) {
    val clean = uname.trim().lowercase().removePrefix("@")
    if (clean.length < 3) {
      isUsernameAvailable = null
      isCheckingUsername = false
      return
    }
    usernameCheckJob?.cancel()
    usernameCheckJob = coroutineScope.launch {
      isCheckingUsername = true
      delay(400) // debounce
      val available = FirebaseManager.checkUsernameAvailable(clean)
      isUsernameAvailable = available
      isCheckingUsername = false
    }
  }

  // Initial check if username already preset
  LaunchedEffect(username) {
    if (isUsernameAvailable == null && username.length >= 3) {
      checkUsername(username)
    }
  }

  // DatePicker State
  var showDatePicker by remember { mutableStateOf(false) }
  val datePickerState = rememberDatePickerState(
    initialSelectedDateMillis = System.currentTimeMillis() - 568025136000L // ~18 years ago default
  )

  var isSubmitting by remember { mutableStateOf(false) }
  var submitErrorMessage by remember { mutableStateOf<String?>(null) }

  // Validation rules
  val isConnectionValid = when (selectedConnection) {
    "School" -> selectedSchoolClass.isNotBlank()
    "College" -> collegeDegree.isNotBlank()
    "Other" -> otherOccupation.isNotBlank()
    else -> false
  }

  val isFormValid = displayName.trim().isNotBlank() &&
    username.trim().length >= 3 &&
    isUsernameAvailable == true &&
    dateOfBirth.isNotBlank() &&
    selectedCountry.isNotBlank() &&
    selectedState.isNotBlank() &&
    isConnectionValid &&
    !isCheckingUsername &&
    !isSubmitting

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Image(
            painter = painterResource(id = R.drawable.learnerport_logo),
            contentDescription = "Learnerport Logo",
            modifier = Modifier.size(36.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Profile Setup",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Welcome to Learnerport Community",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {

      // 1. AVATAR IMAGE PICKER (Uploaded to Supabase bucket 'avatars')
      Box(
        modifier = Modifier
          .size(108.dp)
          .clip(CircleShape)
          .background(LearnerBlue.copy(alpha = 0.12f))
          .clickable {
            photoPickerLauncher.launch(
              androidx.activity.result.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
              )
            )
          }
          .testTag("profile_avatar_picker"),
        contentAlignment = Alignment.Center
      ) {
        when {
          selectedImageUri != null -> {
            AsyncImage(
              model = selectedImageUri,
              contentDescription = "Profile Photo",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
          }
          firebaseUser?.photoUrl != null -> {
            AsyncImage(
              model = firebaseUser.photoUrl,
              contentDescription = "Profile Photo",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
          }
          else -> {
            Icon(
              imageVector = Icons.Default.AccountCircle,
              contentDescription = null,
              tint = LearnerBlue,
              modifier = Modifier.size(72.dp)
            )
          }
        }

        // Camera badge
        Surface(
          shape = CircleShape,
          color = LearnerBlue,
          modifier = Modifier
            .size(32.dp)
            .align(Alignment.BottomEnd)
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.CameraAlt,
              contentDescription = "Pick Image",
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Tap to choose profile photo",
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = LearnerBlue
      )

      if (authenticatedAccount.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
          Text(
            text = "Linked to: $authenticatedAccount",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // 2. PROFILE NAME
      OutlinedTextField(
        value = displayName,
        onValueChange = { displayName = it },
        label = { Text("Profile Name *") },
        placeholder = { Text("e.g. Alex Johnson") },
        leadingIcon = {
          Icon(Icons.Default.Person, contentDescription = null, tint = LearnerBlue)
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("profile_name_input")
      )

      Spacer(modifier = Modifier.height(14.dp))

      // 3. USERNAME (Real-time live unique validation + permanent lock notice)
      OutlinedTextField(
        value = username,
        onValueChange = {
          val clean = it.lowercase().replace(" ", "_").filter { c -> c.isLetterOrDigit() || c == '_' }
          username = clean
          checkUsername(clean)
        },
        label = { Text("Username *") },
        placeholder = { Text("e.g. learner_alex") },
        leadingIcon = {
          Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = LearnerBlue)
        },
        trailingIcon = {
          when {
            isCheckingUsername -> {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = LearnerBlue)
            }
            isUsernameAvailable == true -> {
              Icon(Icons.Default.CheckCircle, contentDescription = "Available", tint = LearnerAccentGreen)
            }
            isUsernameAvailable == false -> {
              Icon(Icons.Default.Cancel, contentDescription = "Taken", tint = LearnerAccentRed)
            }
          }
        },
        isError = isUsernameAvailable == false,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("profile_username_input")
      )

      // Live validation message
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        when {
          isUsernameAvailable == false -> {
            Text(
              text = "Username already taken! Please choose another.",
              color = LearnerAccentRed,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
          isUsernameAvailable == true -> {
            Text(
              text = "✓ Username is available and unique.",
              color = LearnerAccentGreen,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
          else -> {
            Text(
              text = "Locked permanently once submitted. Letters, numbers, and underscores only.",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 11.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 4. DATE OF BIRTH (DOB) - Mandatory with DatePicker
      OutlinedTextField(
        value = dateOfBirth,
        onValueChange = {},
        readOnly = true,
        label = { Text("Date of Birth (DOB) *") },
        placeholder = { Text("Select your birthdate") },
        leadingIcon = {
          Icon(Icons.Default.Cake, contentDescription = null, tint = LearnerBlue)
        },
        trailingIcon = {
          IconButton(onClick = { showDatePicker = true }) {
            Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date", tint = LearnerBlue)
          }
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { showDatePicker = true }
          .testTag("profile_dob_input")
      )

      Spacer(modifier = Modifier.height(18.dp))

      // 5. DYNAMIC "YOUR CONNECTION" SELECTION [School, College, Other]
      Text(
        text = "Your Connection *",
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
      )
      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        listOf("School", "College", "Other").forEach { connection ->
          val isSelected = selectedConnection == connection
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) LearnerBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
              .weight(1f)
              .clickable { selectedConnection = connection }
              .testTag("connection_chip_${connection.lowercase()}")
          ) {
            Box(
              modifier = Modifier.padding(vertical = 12.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = connection,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Dynamic sub-fields for Connection
      when (selectedConnection) {
        "School" -> {
          // Dropdown to select class (1 to 12)
          Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
              value = selectedSchoolClass,
              onValueChange = {},
              readOnly = true,
              label = { Text("Select Class (1 to 12) *") },
              leadingIcon = {
                Icon(Icons.Default.School, contentDescription = null, tint = LearnerBlue)
              },
              trailingIcon = {
                IconButton(onClick = { isClassDropdownOpen = true }) {
                  Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
              },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { isClassDropdownOpen = true }
                .testTag("school_class_dropdown")
            )

            DropdownMenu(
              expanded = isClassDropdownOpen,
              onDismissRequest = { isClassDropdownOpen = false },
              modifier = Modifier.fillMaxWidth(0.9f)
            ) {
              schoolClasses.forEach { cls ->
                DropdownMenuItem(
                  text = { Text(cls, fontWeight = if (selectedSchoolClass == cls) FontWeight.Bold else FontWeight.Normal) },
                  onClick = {
                    selectedSchoolClass = cls
                    isClassDropdownOpen = false
                  }
                )
              }
            }
          }
        }
        "College" -> {
          // Text input field for Degree Name
          OutlinedTextField(
            value = collegeDegree,
            onValueChange = { collegeDegree = it },
            label = { Text("Degree / Course Name *") },
            placeholder = { Text("e.g. B.Tech Computer Science / BCA / MBBS / B.Com") },
            leadingIcon = {
              Icon(Icons.Default.AccountBalance, contentDescription = null, tint = LearnerBlue)
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("college_degree_input")
          )
        }
        "Other" -> {
          // Text input for custom connection / occupation
          OutlinedTextField(
            value = otherOccupation,
            onValueChange = { otherOccupation = it },
            label = { Text("Your Occupation / Field *") },
            placeholder = { Text("e.g. Self-Taught Developer / Educator / Aspirant") },
            leadingIcon = {
              Icon(Icons.Default.Work, contentDescription = null, tint = LearnerBlue)
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("other_connection_input")
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // 6. CASCADING COUNTRY & STATE LOCATION SELECTOR (Searchable dropdowns)
      Text(
        text = "Location (Country & State) *",
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
      )
      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Country Selector
        Box(modifier = Modifier.weight(1f)) {
          OutlinedTextField(
            value = selectedCountry,
            onValueChange = {},
            readOnly = true,
            label = { Text("Country *") },
            trailingIcon = {
              IconButton(onClick = { isCountryDropdownOpen = true }) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
              }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { isCountryDropdownOpen = true }
              .testTag("country_selector")
          )

          DropdownMenu(
            expanded = isCountryDropdownOpen,
            onDismissRequest = { isCountryDropdownOpen = false },
            modifier = Modifier.fillMaxWidth(0.85f)
          ) {
            OutlinedTextField(
              value = countrySearchQuery,
              onValueChange = { countrySearchQuery = it },
              placeholder = { Text("Search country...") },
              modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
            )
            val filteredCountries = CountryStateData.countries.filter {
              countrySearchQuery.isBlank() || it.contains(countrySearchQuery, ignoreCase = true)
            }
            filteredCountries.forEach { c ->
              DropdownMenuItem(
                text = { Text(c, fontWeight = if (selectedCountry == c) FontWeight.Bold else FontWeight.Normal) },
                onClick = {
                  selectedCountry = c
                  // Reset state to first state of newly selected country
                  val states = CountryStateData.getStates(c)
                  selectedState = states.firstOrNull() ?: ""
                  isCountryDropdownOpen = false
                  countrySearchQuery = ""
                }
              )
            }
          }
        }

        // State Selector (dynamically filtered by selected Country)
        Box(modifier = Modifier.weight(1f)) {
          OutlinedTextField(
            value = selectedState,
            onValueChange = {},
            readOnly = true,
            label = { Text("State *") },
            trailingIcon = {
              IconButton(onClick = { isStateDropdownOpen = true }) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
              }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { isStateDropdownOpen = true }
              .testTag("state_selector")
          )

          DropdownMenu(
            expanded = isStateDropdownOpen,
            onDismissRequest = { isStateDropdownOpen = false },
            modifier = Modifier.fillMaxWidth(0.85f)
          ) {
            OutlinedTextField(
              value = stateSearchQuery,
              onValueChange = { stateSearchQuery = it },
              placeholder = { Text("Search state...") },
              modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
            )
            val statesForCountry = CountryStateData.getStates(selectedCountry).filter {
              stateSearchQuery.isBlank() || it.contains(stateSearchQuery, ignoreCase = true)
            }
            statesForCountry.forEach { s ->
              DropdownMenuItem(
                text = { Text(s, fontWeight = if (selectedState == s) FontWeight.Bold else FontWeight.Normal) },
                onClick = {
                  selectedState = s
                  isStateDropdownOpen = false
                  stateSearchQuery = ""
                }
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // 7. BIO (ONLY OPTIONAL FIELD)
      OutlinedTextField(
        value = bio,
        onValueChange = { bio = it },
        label = { Text("Bio (Optional)") },
        placeholder = { Text("Write a few words about your educational goals, subjects of interest...") },
        minLines = 3,
        maxLines = 5,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("profile_bio_input")
      )

      if (submitErrorMessage != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = submitErrorMessage!!,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 12.sp,
            modifier = Modifier.padding(12.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // 8. "OK / SUBMIT" BUTTON
      Button(
        onClick = {
          if (!isFormValid) return@Button
          isSubmitting = true
          submitErrorMessage = null

          coroutineScope.launch {
            val user = FirebaseManager.currentUser.value
            val userId = user?.uid ?: currentSession?.userId ?: "user_${System.currentTimeMillis()}"

            // 1. Upload Avatar if chosen to Firebase Storage (with Supabase fallback)
            var uploadedAvatarUrl = firebaseProfile?.avatarUrl ?: currentSession?.avatarUrl ?: ""
            if (imageBytes != null) {
              val compressed = com.example.data.util.ImageCompressor.compressAvatar(imageBytes!!)
              val fbRes = FirebaseManager.uploadAvatarImage(userId, compressed)
              if (fbRes.isSuccess) {
                uploadedAvatarUrl = fbRes.getOrNull()?.first ?: uploadedAvatarUrl
              } else {
                val uploadRes = SupabaseClient.uploadAvatar(userId, compressed)
                if (uploadRes.isSuccess) {
                  uploadedAvatarUrl = uploadRes.getOrNull() ?: uploadedAvatarUrl
                }
              }
            }

            val connectionDetail = when (selectedConnection) {
              "School" -> selectedSchoolClass
              "College" -> collegeDegree
              else -> otherOccupation
            }

            // 2. Save in Firestore
            val saveResult = FirebaseManager.saveUserProfile(
              displayName = displayName.trim(),
              username = username.trim().lowercase(),
              dateOfBirth = dateOfBirth,
              educationalLevel = selectedConnection,
              courseOrDegree = connectionDetail,
              institution = "",
              fieldOfStudy = "",
              photoUrl = uploadedAvatarUrl,
              bio = bio.trim(),
              country = selectedCountry,
              state = selectedState,
              connectionType = selectedConnection,
              connectionDetail = connectionDetail,
              isCompleted = true
            )

            // 3. Sync to Supabase DB if configured
            SupabaseClient.updateProfileInDb(
              context = context,
              userId = userId,
              username = username.trim().lowercase(),
              displayName = displayName.trim(),
              avatarUrl = uploadedAvatarUrl,
              dateOfBirth = dateOfBirth,
              educationalLevel = selectedConnection,
              courseOrDegree = connectionDetail,
              fieldOfStudy = "",
              bio = bio.trim(),
              isCompleted = true
            )

            isSubmitting = false
            if (saveResult.isSuccess) {
              onProfileCompleted()
            } else {
              submitErrorMessage = saveResult.exceptionOrNull()?.localizedMessage ?: "Failed to save profile. Please try again."
            }
          }
        },
        enabled = isFormValid,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("submit_profile_button")
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
          Spacer(modifier = Modifier.width(10.dp))
          Text("Saving Profile...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        } else {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("OK / Submit", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      }

      Spacer(modifier = Modifier.height(28.dp))
    }
  }

  // Material 3 DatePickerDialog
  if (showDatePicker) {
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            val millis = datePickerState.selectedDateMillis
            if (millis != null) {
              val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
              dateOfBirth = sdf.format(Date(millis))
            }
            showDatePicker = false
          }
        ) {
          Text("OK", fontWeight = FontWeight.Bold, color = LearnerBlue)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text("Cancel")
        }
      }
    ) {
      DatePicker(state = datePickerState)
    }
  }
}
