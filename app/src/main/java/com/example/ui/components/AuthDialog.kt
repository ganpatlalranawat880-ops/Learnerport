package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.SupabaseClient
import com.example.ui.theme.LearnerBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthDialog(
  onDismiss: () -> Unit,
  onAuthSuccess: (email: String, username: String, displayName: String) -> Unit,
  modifier: Modifier = Modifier
) {
  val coroutineScope = rememberCoroutineScope()
  var isSignUp by remember { mutableStateOf(false) }

  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var username by remember { mutableStateOf("") }
  var displayName by remember { mutableStateOf("") }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var successMessage by remember { mutableStateOf<String?>(null) }

  val isConfigured = SupabaseClient.isConfigured()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (isSignUp) "Create Learnerport Account" else "Sign In to Learnerport",
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Tab switcher (Sign In vs Sign Up)
        TabRow(
          selectedTabIndex = if (isSignUp) 1 else 0,
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          contentColor = LearnerBlue
        ) {
          Tab(
            selected = !isSignUp,
            onClick = {
              isSignUp = false
              errorMessage = null
            },
            text = { Text("Sign In", fontWeight = if (!isSignUp) FontWeight.Bold else FontWeight.Normal) }
          )
          Tab(
            selected = isSignUp,
            onClick = {
              isSignUp = true
              errorMessage = null
            },
            text = { Text("Sign Up", fontWeight = if (isSignUp) FontWeight.Bold else FontWeight.Normal) }
          )
        }

        if (!isConfigured) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFEF3C7),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Notice: Supabase credentials are in placeholder mode in .env. Enter valid SUPABASE_URL and SUPABASE_ANON_KEY to sync with your real cloud database.",
              fontSize = 11.sp,
              color = Color(0xFF92400E),
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        if (errorMessage != null) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = errorMessage!!,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        if (successMessage != null) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFDCFCE7),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = successMessage!!,
              fontSize = 12.sp,
              color = Color(0xFF166534),
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        if (isSignUp) {
          OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = LearnerBlue) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = username,
            onValueChange = { username = it.replace(" ", "_").lowercase() },
            label = { Text("Unique Username") },
            prefix = { Text("@") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }

        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email Address") },
          leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = LearnerBlue) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text("Password") },
          leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = LearnerBlue) },
          visualTransformation = PasswordVisualTransformation(),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        if (!isSignUp) {
          TextButton(
            onClick = {
              if (email.isBlank()) {
                errorMessage = "Enter your email to receive a password reset link."
                return@TextButton
              }
              coroutineScope.launch {
                isLoading = true
                val res = SupabaseClient.resetPassword(email)
                isLoading = false
                if (res.isSuccess) {
                  successMessage = "Password reset instructions sent to $email."
                  errorMessage = null
                } else {
                  errorMessage = res.exceptionOrNull()?.message ?: "Failed to reset password."
                }
              }
            },
            modifier = Modifier.align(Alignment.End)
          ) {
            Text("Forgot Password?", fontSize = 11.sp, color = LearnerBlue)
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // OAuth Options
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              errorMessage = "Google OAuth: Ensure Google Provider is enabled in Supabase Dashboard (Authentication -> Providers -> Google) with Client ID & Secret."
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Google", fontSize = 12.sp)
          }

          OutlinedButton(
            onClick = {
              errorMessage = "Apple OAuth: Ensure Apple Provider is enabled in your Supabase project under Authentication -> Providers."
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Apple", fontSize = 12.sp)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (email.isBlank() || password.isBlank()) {
            errorMessage = "Please enter both email and password."
            return@Button
          }

          if (isSignUp && (username.isBlank() || displayName.isBlank())) {
            errorMessage = "Please enter your name and choose a unique username."
            return@Button
          }

          if (isConfigured) {
            coroutineScope.launch {
              isLoading = true
              errorMessage = null
              val result = if (isSignUp) {
                SupabaseClient.signUpWithEmail(null, email, password, displayName)
              } else {
                SupabaseClient.signInWithEmail(null, email, password)
              }
              isLoading = false

              result.onSuccess { session ->
                onAuthSuccess(session.email, session.username, session.displayName)
                onDismiss()
              }.onFailure { err ->
                errorMessage = err.message ?: "Authentication failed."
              }
            }
          } else {
            // Local fallback simulation when credentials not yet entered
            val finalUser = if (isSignUp) username else email.substringBefore("@")
            val finalName = if (isSignUp) displayName else finalUser.replaceFirstChar { it.uppercase() }
            onAuthSuccess(email, finalUser, finalName)
            onDismiss()
          }
        },
        enabled = !isLoading,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue),
        modifier = Modifier.testTag("auth_submit_button")
      ) {
        if (isLoading) {
          CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
        } else {
          Text(if (isSignUp) "Sign Up" else "Sign In", fontWeight = FontWeight.Bold)
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
