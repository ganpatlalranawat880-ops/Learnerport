package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LearnerBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsModal(onClose: () -> Unit) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Terms & Conditions", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
        }
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Text("1. Acceptance of Terms", fontWeight = FontWeight.Bold, fontSize = 14.sp)
      Text(
        "By accessing and using Learnerport, you agree to comply with all applicable terms, laws, and educational distribution guidelines.",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Text("2. Educational Use & Content Ownership", fontWeight = FontWeight.Bold, fontSize = 14.sp)
      Text(
        "All study materials, notes, textbooks, and question papers uploaded to Learnerport must be authorized for educational distribution. You retain intellectual property rights to your original works while granting Learnerport a non-exclusive license to display them to registered learners.",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Text("3. Uploader Responsibility & Copyright", fontWeight = FontWeight.Bold, fontSize = 14.sp)
      Text(
        "Uploaders are solely responsible for ensuring they possess the legal right or written consent to share any copyrighted book, note, or examination paper. Unauthorized copyrighted uploads will be removed immediately upon notice.",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Text("4. Community Conduct", fontWeight = FontWeight.Bold, fontSize = 14.sp)
      Text(
        "Harassment, spam, fraudulent educational claims, and inappropriate content are strictly prohibited and will result in account suspension.",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyModal(onClose: () -> Unit) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Privacy Policy", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
        }
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Text("1. Information We Collect", fontWeight = FontWeight.Bold, fontSize = 14.sp)
      Text(
        "We collect information you provide directly during registration and profile setup: full name, unique username, email address, date of birth, educational qualifications, course/degree, profile photo, and uploaded educational documents.",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Text("2. How We Use Information", fontWeight = FontWeight.Bold, fontSize = 14.sp)
      Text(
        "Information is used solely to provide educational services, personalize course recommendations, attribute published notes to their respective authors, and enable secure authentication.",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Text("3. Data Security & Local Protection", fontWeight = FontWeight.Bold, fontSize = 14.sp)
      Text(
        "Authentication tokens and profile credentials are encrypted. You can also configure an on-device Security PIN (App Lock) in Settings to protect your application on your physical device.",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportModal(onClose: () -> Unit) {
  val context = androidx.compose.ui.platform.LocalContext.current
  var expandedFaqIndex by remember { mutableStateOf<Int?>(0) }
  var showFeedbackDialog by remember { mutableStateOf(false) }
  var feedbackSubject by remember { mutableStateOf("") }
  var feedbackBody by remember { mutableStateOf("") }
  var feedbackSuccessMessage by remember { mutableStateOf<String?>(null) }

  val faqs = remember {
    listOf(
      FaqItem(
        question = "How do I sign in or register with Google?",
        answer = "Tap 'Continue with Google' on the welcome screen. If you have an active Google account on your device, you'll see a seamless prompt 'Sign in to Learnerport as [Name]'. After signing in, complete your username and educational background to access all study materials."
      ),
      FaqItem(
        question = "Can I register with my standard Email address?",
        answer = "Yes! Tap 'Sign Up with Email', enter your full name, email, and choose a password with at least 6 characters. If you ever forget your password, use the 'Forgot Password?' option to receive a secure reset link."
      ),
      FaqItem(
        question = "How do I upload study notes, books, or question papers?",
        answer = "Navigate to the Upload tab at the bottom. Choose your category (Notes, Book PDF, or Question Paper), pick the PDF document or file from your device, and provide the subject, topic, and educational class. Once submitted, your upload becomes available to students worldwide."
      ),
      FaqItem(
        question = "How can I read PDFs and notes offline?",
        answer = "Open any study resource card and tap 'Download PDF' or 'Read Online'. Downloaded documents are securely saved to your 'Saved & Downloads' tab, allowing full offline reading anytime without internet connection."
      ),
      FaqItem(
        question = "How do I enable or change the 4-Digit App Lock PIN?",
        answer = "Go to Settings -> Security PIN / App Lock. Enter a 4-digit code and confirm it. When enabled, Learnerport will require this PIN each time the app is launched to protect your account and downloads."
      ),
      FaqItem(
        question = "How do I report copyrighted or infringing content?",
        answer = "Learnerport strictly respects educational copyright. Tap the menu on any resource to report it, or visit Settings -> Main Information -> Report Content to file a takedown request. Our administration reviews submissions promptly."
      )
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Help & Support", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
        },
        actions = {
          TextButton(onClick = { showFeedbackDialog = true }) {
            Text("Feedback", fontWeight = FontWeight.Bold, color = LearnerBlue)
          }
        }
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Header Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LearnerBlue.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = LearnerBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Learnerport Help Center", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LearnerBlue)
          }
          Text(
            "Find answers to frequently asked questions about authentication, uploading study resources, downloading notes, and app security.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
          )
        }
      }

      if (feedbackSuccessMessage != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFFDCFCE7),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = feedbackSuccessMessage!!,
            color = Color(0xFF166534),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(12.dp)
          )
        }
      }

      Text("Frequently Asked Questions", fontWeight = FontWeight.Bold, fontSize = 15.sp)

      faqs.forEachIndexed { index, faq ->
        val isExpanded = expandedFaqIndex == index
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = CardDefaults.outlinedCardBorder(),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              expandedFaqIndex = if (isExpanded) null else index
            }
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = faq.question,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (isExpanded) LearnerBlue else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
              )
              Text(
                text = if (isExpanded) "−" else "+",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = LearnerBlue,
                modifier = Modifier.padding(start = 8.dp)
              )
            }

            if (isExpanded) {
              Spacer(modifier = Modifier.height(8.dp))
              HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = faq.answer,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Direct Contact Section
      Text("Need More Help?", fontWeight = FontWeight.Bold, fontSize = 15.sp)

      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Official Support Team", fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Text(
            "Contact our support desk directly for assistance with your account, missing uploads, or bug reports.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = {
                val emailIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                  data = android.net.Uri.parse("mailto:support@learnerport.org")
                  putExtra(android.content.Intent.EXTRA_SUBJECT, "[Learnerport Support Request]")
                  putExtra(android.content.Intent.EXTRA_TEXT, "Hello Learnerport Support,\n\nI need assistance with:\n")
                }
                try {
                  context.startActivity(emailIntent)
                } catch (e: Exception) {
                  android.widget.Toast.makeText(context, "Email client not found. Contact: support@learnerport.org", android.widget.Toast.LENGTH_LONG).show()
                }
              },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue),
              modifier = Modifier.weight(1f)
            ) {
              Text("Email Support", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
              onClick = { showFeedbackDialog = true },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("Send Feedback", fontSize = 13.sp)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Learnerport Platform • Version 1.0.0 • support@learnerport.org",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.align(Alignment.CenterHorizontally)
      )
    }
  }

  // Interactive Feedback Dialog
  if (showFeedbackDialog) {
    AlertDialog(
      onDismissRequest = { showFeedbackDialog = false },
      title = { Text("Send App Feedback", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Your thoughts and reports help us improve Learnerport for all students.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

          OutlinedTextField(
            value = feedbackSubject,
            onValueChange = { feedbackSubject = it },
            label = { Text("Subject / Topic") },
            placeholder = { Text("e.g. Question Paper feature request") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = feedbackBody,
            onValueChange = { feedbackBody = it },
            label = { Text("Your Message") },
            placeholder = { Text("Describe the issue or suggestion in detail...") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (feedbackBody.isNotBlank()) {
              showFeedbackDialog = false
              feedbackSuccessMessage = "Thank you! Your feedback has been received. Our team will review it."
              feedbackSubject = ""
              feedbackBody = ""
            } else {
              android.widget.Toast.makeText(context, "Please write a message before submitting.", android.widget.Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue)
        ) {
          Text("Submit Feedback")
        }
      },
      dismissButton = {
        TextButton(onClick = { showFeedbackDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

data class FaqItem(val question: String, val answer: String)

@Composable
fun ReportContentModal(
  targetTitle: String = "Educational Content",
  onDismiss: () -> Unit,
  onSubmitReport: (reason: String, details: String) -> Unit
) {
  var selectedReason by remember { mutableStateOf("Copyright Infringement") }
  var details by remember { mutableStateOf("") }
  var isSubmitted by remember { mutableStateOf(false) }

  val reasons = listOf(
    "Copyright Infringement",
    "Inappropriate or Harmful Content",
    "Incorrect Academic Information",
    "Spam or Promotional Upload",
    "Other"
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(if (isSubmitted) "Report Received" else "Report Content", fontWeight = FontWeight.Bold, fontSize = 17.sp)
    },
    text = {
      if (isSubmitted) {
        Text(
          "Thank you for reporting. Our moderation team will review this upload according to our copyright and content policies.",
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Reporting: $targetTitle",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = LearnerBlue
          )
          Text("Please select a reason:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

          reasons.forEach { r ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              RadioButton(
                selected = selectedReason == r,
                onClick = { selectedReason = r }
              )
              Text(r, fontSize = 12.sp)
            }
          }

          OutlinedTextField(
            value = details,
            onValueChange = { details = it },
            label = { Text("Additional details (optional)") },
            minLines = 2,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    },
    confirmButton = {
      if (isSubmitted) {
        Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue)) {
          Text("Done")
        }
      } else {
        Button(
          onClick = {
            onSubmitReport(selectedReason, details)
            isSubmitted = true
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Submit Report")
        }
      }
    },
    dismissButton = {
      if (!isSubmitted) {
        TextButton(onClick = onDismiss) { Text("Cancel") }
      }
    }
  )
}
