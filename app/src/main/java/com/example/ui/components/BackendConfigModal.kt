package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.SupabaseClient
import com.example.ui.theme.LearnerBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackendConfigModal(
  onClose: () -> Unit,
  onConfigSaved: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var supabaseUrl by remember { mutableStateOf(SupabaseClient.getSupabaseUrl(context)) }
  var anonKey by remember { mutableStateOf(SupabaseClient.getSupabaseAnonKey(context)) }
  var googleClientId by remember { mutableStateOf(SupabaseClient.getGoogleServerClientId(context)) }

  var isTesting by remember { mutableStateOf(false) }
  var testResult by remember { mutableStateOf<String?>(null) }
  var testSuccess by remember { mutableStateOf<Boolean?>(null) }

  val packageName = "com.aistudio.learnerport.etczci"
  val sha1Fingerprint = "FB:47:54:66:B9:E1:23:02:37:97:48:D0:79:73:8A:85:F2:B9:FB:6F"
  val sha256Fingerprint = "3C:E9:FC:20:F7:AA:15:52:07:9E:DE:BE:4B:5A:03:81:92:01:E0:23:35:8A:80:4B:32:BF:2D:8D:A2:73:06:DA"
  val redirectUri = "learnerport://login-callback"

  fun copyToClipboard(label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
  }

  AlertDialog(
    onDismissRequest = onClose,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Backend & Auth Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold)
          Text("Supabase & Google OAuth", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onClose) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text(
          text = "Enter your live Supabase credentials to enable real email signup, login, cloud data sync, and avatar uploads.",
          fontSize = 12.sp,
          lineHeight = 16.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 1. Supabase Project URL
        OutlinedTextField(
          value = supabaseUrl,
          onValueChange = { supabaseUrl = it.trim() },
          label = { Text("Supabase Project URL") },
          placeholder = { Text("https://xyzcompany.supabase.co") },
          leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null, tint = LearnerBlue) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("config_supabase_url_input")
        )

        // 2. Supabase Anon Key
        OutlinedTextField(
          value = anonKey,
          onValueChange = { anonKey = it.trim() },
          label = { Text("Supabase Anon / Public Key") },
          placeholder = { Text("eyJhbGciOiJIUzI1NiIsInR5cCI6...") },
          leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = LearnerBlue) },
          singleLine = false,
          maxLines = 3,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("config_supabase_anon_key_input")
        )

        // 3. Google Server / Web Client ID
        OutlinedTextField(
          value = googleClientId,
          onValueChange = { googleClientId = it.trim() },
          label = { Text("Google Web Client ID (Optional)") },
          placeholder = { Text("123456789-xyz.apps.googleusercontent.com") },
          leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = LearnerBlue) },
          singleLine = false,
          maxLines = 2,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("config_google_client_id_input")
        )

        // Test Connection Button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              if (supabaseUrl.isBlank() || anonKey.isBlank()) {
                testResult = "Please enter both Supabase URL and Anon Key first."
                testSuccess = false
                return@OutlinedButton
              }

              // Temporarily save to test
              SupabaseClient.setRuntimeCredentials(context, supabaseUrl, anonKey, googleClientId)

              coroutineScope.launch {
                isTesting = true
                testResult = null
                val res = SupabaseClient.testConnection()
                isTesting = false
                res.onSuccess {
                  testSuccess = true
                  testResult = "✅ Connection successful! Backend is reachable."
                }.onFailure { err ->
                  testSuccess = false
                  testResult = "❌ Connection failed: ${err.message}"
                }
              }
            },
            enabled = !isTesting,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            if (isTesting) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Testing...", fontSize = 12.sp)
            } else {
              Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Test Connection", fontSize = 12.sp)
            }
          }
        }

        // Test Result Feedback
        testResult?.let { msg ->
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (testSuccess == true) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = msg,
              color = if (testSuccess == true) Color(0xFF15803D) else Color(0xFFB91C1C),
              fontSize = 12.sp,
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        HorizontalDivider()

        // Setup Guide & Copy Helpers
        Text("Google OAuth & Supabase Setup Details", fontWeight = FontWeight.Bold, fontSize = 13.sp)

        // Package Name
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Android Package Name", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(packageName, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
          }
          IconButton(onClick = { copyToClipboard("Package Name", packageName) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Package Name", modifier = Modifier.size(16.dp))
          }
        }

        // SHA-1
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Debug SHA-1 Certificate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(sha1Fingerprint, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
          }
          IconButton(onClick = { copyToClipboard("SHA-1", sha1Fingerprint) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy SHA-1", modifier = Modifier.size(16.dp))
          }
        }

        // Redirect URI
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Redirect URL (Supabase Auth)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(redirectUri, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
          }
          IconButton(onClick = { copyToClipboard("Redirect URI", redirectUri) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Redirect URI", modifier = Modifier.size(16.dp))
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (supabaseUrl.isNotBlank() && anonKey.isNotBlank()) {
            SupabaseClient.setRuntimeCredentials(context, supabaseUrl, anonKey, googleClientId)
            Toast.makeText(context, "Backend credentials saved successfully!", Toast.LENGTH_SHORT).show()
            onConfigSaved()
            onClose()
          } else {
            Toast.makeText(context, "Please enter both Supabase URL and Anon Key", Toast.LENGTH_SHORT).show()
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Save & Apply")
      }
    },
    dismissButton = {
      TextButton(onClick = onClose) {
        Text("Cancel")
      }
    }
  )
}
