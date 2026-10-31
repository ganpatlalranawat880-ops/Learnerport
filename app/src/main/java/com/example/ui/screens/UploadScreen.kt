package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ResourceType
import com.example.ui.theme.LearnerBlue
import kotlinx.coroutines.launch

private enum class UploadTypeChoice(
  val title: String,
  val description: String,
  val icon: ImageVector,
  val badgeColor: Color
) {
  NOTES("Upload Notes", "Share lecture notes, study summaries & revision guides", Icons.Default.Description, Color(0xFF007AFF)),
  BOOK("Upload Book PDF", "Publish educational textbooks, handbooks & readers", Icons.Default.MenuBook, Color(0xFF10B981)),
  QUESTION_PAPER("Upload Question Paper", "Share previous year papers, sample tests & mock exams", Icons.Default.Assignment, Color(0xFFF59E0B)),
  THOUGHT("Publish Thought", "Share educational insights, study tips & reflections", Icons.Default.EditNote, Color(0xFF8B5CF6))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
  onPublishResource: (
    title: String,
    description: String,
    resourceType: ResourceType,
    subject: String,
    topic: String,
    educationalLevel: String,
    programmingLanguage: String?,
    language: String,
    tags: List<String>,
    examinationYear: String?,
    boardOrUniversity: String?,
    paperType: String?,
    coverUrl: String,
    pdfBytes: ByteArray?,
    pdfFileName: String?,
    coverBytes: ByteArray?,
    fileSize: String
  ) -> Unit,
  onPublishThought: (title: String?, content: String, tags: List<String>) -> Unit,
  initialChoice: ResourceType? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  var selectedChoice by remember {
    mutableStateOf(
      when (initialChoice) {
        ResourceType.NOTES, ResourceType.CODING_RESOURCE -> UploadTypeChoice.NOTES
        ResourceType.BOOK -> UploadTypeChoice.BOOK
        ResourceType.QUESTION_PAPER -> UploadTypeChoice.QUESTION_PAPER
        null -> null
      }
    )
  }

  // Common Document Fields
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var classOrCourse by remember { mutableStateOf("B.Tech. / College") }
  var subject by remember { mutableStateOf("") }
  var academicYear by remember { mutableStateOf("2025-2026") }
  var language by remember { mutableStateOf("English") }
  var tagsInput by remember { mutableStateOf("") }
  var hasConfirmedCopyright by remember { mutableStateOf(false) }

  // Book Specific
  var authorName by remember { mutableStateOf("") }
  var publicationInfo by remember { mutableStateOf("") }

  // Question Paper Specific
  var examinationType by remember { mutableStateOf("Previous Year Paper") }
  var examinationYear by remember { mutableStateOf("2025") }
  var boardOrUniversity by remember { mutableStateOf("CBSE / State University") }

  // Native PDF File Picker
  var pdfUri by remember { mutableStateOf<Uri?>(null) }
  var pdfBytes by remember { mutableStateOf<ByteArray?>(null) }
  var pdfFileName by remember { mutableStateOf<String?>(null) }
  var pdfFileSize by remember { mutableStateOf("2.8 MB") }

  val pdfFilePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      pdfUri = uri
      var name = "Document.pdf"
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
          if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
          if (sizeIndex != -1) {
            val sizeBytes = cursor.getLong(sizeIndex)
            pdfFileSize = "${String.format("%.1f", sizeBytes / (1024.0 * 1024.0))} MB"
          }
        }
      }
      pdfFileName = name
      pdfBytes = try { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } } catch (e: Exception) { null }
    }
  }

  // Cover Selection (Custom Gallery Image)
  var coverType by remember { mutableStateOf("") }
  var customCoverUri by remember { mutableStateOf<Uri?>(null) }
  var coverBytes by remember { mutableStateOf<ByteArray?>(null) }

  val coverGalleryPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      customCoverUri = uri
      coverType = "custom"
      coverBytes = try { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } } catch (e: Exception) { null }
    }
  }

  // Thought Specific
  var thoughtTitle by remember { mutableStateOf("") }
  var thoughtContent by remember { mutableStateOf("") }
  var thoughtTagsInput by remember { mutableStateOf("") }

  val coroutineScope = rememberCoroutineScope()
  var isUploading by remember { mutableStateOf(false) }
  var uploadStageText by remember { mutableStateOf("") }
  var uploadProgressPercent by remember { mutableIntStateOf(0) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    if (selectedChoice == null) {
      Text(
        text = "Publish Educational Content",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Select what you would like to share with the Learnerport community:",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(18.dp))

      UploadTypeChoice.values().forEach { choice ->
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
              selectedChoice = choice
              errorMessage = null
            }
            .testTag("upload_choice_${choice.name.lowercase()}"),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = CardDefaults.outlinedCardBorder()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = choice.badgeColor.copy(alpha = 0.12f),
              modifier = Modifier.size(52.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = choice.icon,
                  contentDescription = null,
                  tint = choice.badgeColor,
                  modifier = Modifier.size(28.dp)
                )
              }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = choice.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = choice.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
              )
            }

            Icon(
              imageVector = Icons.Default.ChevronRight,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    } else {
      // FORM HEADER
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        IconButton(onClick = { selectedChoice = null }) {
          Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = selectedChoice!!.title,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (errorMessage != null) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = errorMessage!!,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 13.sp,
            modifier = Modifier.padding(12.dp)
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      if (selectedChoice == UploadTypeChoice.THOUGHT) {
        // ===================== THOUGHT FORM =====================
        OutlinedTextField(
          value = thoughtTitle,
          onValueChange = { thoughtTitle = it },
          label = { Text("Title (Optional)") },
          placeholder = { Text("e.g. Master algorithms through pattern recognition") },
          modifier = Modifier.fillMaxWidth().testTag("thought_title_input"),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Word counter for 9000 words max
        val thoughtWordCount = remember(thoughtContent) {
          if (thoughtContent.isBlank()) 0 else thoughtContent.trim().split("\\s+".toRegex()).count { it.isNotBlank() }
        }
        val isWordCountExceeded = thoughtWordCount > 9000

        OutlinedTextField(
          value = thoughtContent,
          onValueChange = { thoughtContent = it },
          label = { Text("Thought Content *") },
          placeholder = { Text("Write your educational insight, study advice, notes or discussion topic (max 9000 words)...") },
          minLines = 6,
          maxLines = 14,
          isError = isWordCountExceeded,
          modifier = Modifier.fillMaxWidth().testTag("thought_content_input"),
          shape = RoundedCornerShape(12.dp)
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, end = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (isWordCountExceeded) {
            Text(
              text = "Exceeded limit! Maximum is 9000 words.",
              color = MaterialTheme.colorScheme.error,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          } else {
            Text(
              text = "Pure text post • No files required",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 11.sp
            )
          }

          Text(
            text = "$thoughtWordCount / 9000 words",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isWordCountExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = thoughtTagsInput,
          onValueChange = { thoughtTagsInput = it },
          label = { Text("Tags (comma-separated)") },
          placeholder = { Text("StudyTips, CodingHabits, Learning") },
          modifier = Modifier.fillMaxWidth().testTag("thought_tags_input"),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = {
            if (thoughtContent.isBlank()) {
              errorMessage = "Thought content cannot be empty."
              return@Button
            }
            if (thoughtWordCount > 9000) {
              errorMessage = "Content exceeds 9000 words limit. Please shorten."
              return@Button
            }
            val parsedTags = thoughtTagsInput.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }
            onPublishThought(thoughtTitle.ifBlank { null }, thoughtContent.trim(), parsedTags)
            selectedChoice = null
          },
          enabled = thoughtContent.isNotBlank() && !isWordCountExceeded,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("publish_thought_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue)
        ) {
          Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Publish Thought", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

      } else {
        // ===================== PDF DOCUMENT FORMS =====================
        // Category specific titles
        val titleLabel = when (selectedChoice) {
          UploadTypeChoice.BOOK -> "Book Title *"
          UploadTypeChoice.QUESTION_PAPER -> "Question Paper Title *"
          else -> "Notes Title *"
        }

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text(titleLabel) },
          placeholder = { Text("e.g. Data Structures & Algorithms Complete Notes") },
          modifier = Modifier.fillMaxWidth().testTag("doc_title_input"),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Book-specific author
        if (selectedChoice == UploadTypeChoice.BOOK) {
          OutlinedTextField(
            value = authorName,
            onValueChange = { authorName = it },
            label = { Text("Author / Publisher *") },
            placeholder = { Text("e.g. Dr. Thomas Cormen") },
            modifier = Modifier.fillMaxWidth().testTag("book_author_input"),
            shape = RoundedCornerShape(12.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
        }

        // Question Paper specific fields
        if (selectedChoice == UploadTypeChoice.QUESTION_PAPER) {
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
              value = examinationType,
              onValueChange = { examinationType = it },
              label = { Text("Exam Type") },
              placeholder = { Text("Previous Year / Sample") },
              modifier = Modifier.weight(1f).testTag("exam_type_input"),
              shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
              value = examinationYear,
              onValueChange = { examinationYear = it },
              label = { Text("Exam Year *") },
              placeholder = { Text("2025") },
              modifier = Modifier.weight(1f).testTag("exam_year_input"),
              shape = RoundedCornerShape(12.dp)
            )
          }
          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = boardOrUniversity,
            onValueChange = { boardOrUniversity = it },
            label = { Text("Board / University *") },
            placeholder = { Text("e.g. CBSE / Delhi University / State Board") },
            modifier = Modifier.fillMaxWidth().testTag("board_input"),
            shape = RoundedCornerShape(12.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = classOrCourse,
            onValueChange = { classOrCourse = it },
            label = { Text("Class / Course *") },
            placeholder = { Text("Class 12 / B.Tech.") },
            modifier = Modifier.weight(1f).testTag("doc_class_input"),
            shape = RoundedCornerShape(12.dp)
          )
          OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject *") },
            placeholder = { Text("Physics / CS") },
            modifier = Modifier.weight(1f).testTag("doc_subject_input"),
            shape = RoundedCornerShape(12.dp)
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description & Learning Outcomes *") },
          placeholder = { Text("Describe the key topics, syllabus coverage, and summary...") },
          minLines = 3,
          maxLines = 5,
          modifier = Modifier.fillMaxWidth().testTag("doc_desc_input"),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // NATIVE PDF FILE PICKER
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (pdfUri != null) LearnerBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
          ),
          border = CardDefaults.outlinedCardBorder(),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              pdfFilePickerLauncher.launch(arrayOf("application/pdf"))
            }
            .testTag("pdf_file_picker_card")
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = if (pdfUri != null) Color(0xFF10B981) else LearnerBlue.copy(alpha = 0.15f),
              modifier = Modifier.size(42.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = if (pdfUri != null) Icons.Default.Check else Icons.Default.PictureAsPdf,
                  contentDescription = null,
                  tint = if (pdfUri != null) Color.White else LearnerBlue
                )
              }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (pdfFileName != null) "Selected: $pdfFileName" else "Select PDF File from Device *",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (pdfUri != null) "Size: $pdfFileSize • Ready to upload" else "Tap to choose a PDF from phone files",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            TextButton(
              onClick = { pdfFilePickerLauncher.launch(arrayOf("application/pdf")) }
            ) {
              Text(if (pdfUri != null) "Change" else "Browse", color = LearnerBlue, fontWeight = FontWeight.SemiBold)
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CUSTOM PDF COVER SELECTION (From Device Gallery)
        Text(
          text = "Custom Book / PDF Cover Image",
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Select a cover photo from your gallery (strict 3:4 portrait book aspect ratio applied):",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Pick from gallery
        OutlinedButton(
          onClick = {
            coverGalleryPickerLauncher.launch(
              androidx.activity.result.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
              )
            )
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("pick_custom_cover_button")
        ) {
          Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp), tint = LearnerBlue)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            if (customCoverUri != null) "Change Cover Image" else "Choose Cover from Device Gallery",
            fontWeight = FontWeight.SemiBold
          )
        }

        if (customCoverUri != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                  .width(150.dp)
                  .aspectRatio(3f / 4f) // Strict 3:4 Standard Portrait Book Ratio
                  .clip(RoundedCornerShape(12.dp))
              ) {
                AsyncImage(
                  model = customCoverUri,
                  contentDescription = "Selected Cover Preview (3:4 Portrait Ratio)",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              TextButton(
                onClick = {
                  customCoverUri = null
                  coverBytes = null
                  coverType = ""
                }
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Remove Cover", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
              }
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "3:4 Standard Portrait Book Cover Ratio Enforced (No File Size Limit)",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = LearnerBlue,
            modifier = Modifier.align(Alignment.CenterHorizontally)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tags
        OutlinedTextField(
          value = tagsInput,
          onValueChange = { tagsInput = it },
          label = { Text("Tags (comma-separated)") },
          placeholder = { Text("Physics, Formulas, Class12, PYQ") },
          modifier = Modifier.fillMaxWidth().testTag("doc_tags_input"),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Copyright Confirmation Checkbox
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .clickable { hasConfirmedCopyright = !hasConfirmedCopyright }
        ) {
          Checkbox(
            checked = hasConfirmedCopyright,
            onCheckedChange = { hasConfirmedCopyright = it },
            modifier = Modifier.testTag("doc_copyright_checkbox")
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "I confirm that I have the right to publish this educational material in accordance with copyright policies (By Satish Ranawat).",
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Upload In-Flight Progress Card
        if (isUploading) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = LearnerBlue.copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(1.dp, LearnerBlue.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = uploadStageText,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = LearnerBlue
                )
                Text(
                  text = "$uploadProgressPercent%",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = LearnerBlue
                )
              }
              LinearProgressIndicator(
                progress = { uploadProgressPercent / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = LearnerBlue,
                trackColor = LearnerBlue.copy(alpha = 0.2f)
              )
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
        }

        // Submit Button
        Button(
          onClick = {
            if (isUploading) return@Button

            if (title.isBlank()) {
              errorMessage = "Please enter a title."
              return@Button
            }
            if (description.isBlank()) {
              errorMessage = "Please enter a description."
              return@Button
            }
            if (subject.isBlank()) {
              errorMessage = "Please enter a subject."
              return@Button
            }
            if (pdfUri == null || pdfBytes == null) {
              errorMessage = "Please select a PDF document from your device."
              return@Button
            }
            if (pdfBytes!!.size > 50 * 1024 * 1024) {
              errorMessage = "PDF file exceeds the 50MB size limit."
              return@Button
            }
            if (!com.example.data.util.ImageCompressor.isValidPdfBytes(pdfBytes)) {
              errorMessage = "The selected file is not a valid PDF document (missing %PDF header)."
              return@Button
            }
            if (!hasConfirmedCopyright) {
              errorMessage = "Please accept the copyright confirmation."
              return@Button
            }

            val rType = when (selectedChoice) {
              UploadTypeChoice.NOTES -> ResourceType.NOTES
              UploadTypeChoice.BOOK -> ResourceType.BOOK
              UploadTypeChoice.QUESTION_PAPER -> ResourceType.QUESTION_PAPER
              else -> ResourceType.NOTES
            }

            val parsedTags = tagsInput.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }
            val chosenCover = customCoverUri?.toString() ?: coverType

            isUploading = true
            errorMessage = null
            uploadStageText = "Starting cloud upload..."
            uploadProgressPercent = 5

            coroutineScope.launch {
              val uploadResult = com.example.data.repository.LearnerportRepository.uploadAndPublishResource(
                title = title.trim(),
                description = description.trim(),
                resourceType = rType,
                subject = subject.trim(),
                topic = classOrCourse.trim(),
                educationalLevel = classOrCourse.trim(),
                programmingLanguage = if (selectedChoice == UploadTypeChoice.BOOK) authorName.trim() else null,
                language = language.trim(),
                tags = parsedTags,
                examinationYear = if (selectedChoice == UploadTypeChoice.QUESTION_PAPER) examinationYear else null,
                boardOrUniversity = if (selectedChoice == UploadTypeChoice.QUESTION_PAPER) boardOrUniversity else null,
                paperType = if (selectedChoice == UploadTypeChoice.QUESTION_PAPER) examinationType else null,
                coverUrl = chosenCover,
                pdfBytes = pdfBytes!!,
                pdfFileName = pdfFileName ?: "$title.pdf",
                coverBytes = coverBytes,
                fileSize = pdfFileSize,
                onProgress = { stage, percent ->
                  uploadStageText = stage
                  uploadProgressPercent = percent
                }
              )

              isUploading = false
              if (uploadResult.isSuccess) {
                // Clear fields and return
                title = ""
                description = ""
                subject = ""
                pdfUri = null
                pdfBytes = null
                pdfFileName = null
                customCoverUri = null
                coverBytes = null
                selectedChoice = null
              } else {
                errorMessage = uploadResult.exceptionOrNull()?.localizedMessage ?: "Failed to upload document to cloud storage."
              }
            }
          },
          enabled = !isUploading,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("publish_document_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue)
        ) {
          if (isUploading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text("Uploading to Cloud...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
          } else {
            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Publish & Share", fontWeight = FontWeight.Bold, fontSize = 16.sp)
          }
        }
      }

      Spacer(modifier = Modifier.height(30.dp))
    }
  }
}
