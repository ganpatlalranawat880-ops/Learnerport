package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.AppLockManager
import com.example.ui.theme.LearnerBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityPinModal(
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val isConfigured = remember { AppLockManager.isPinConfigured(context) }

  var currentPin by remember { mutableStateOf("") }
  var newPin by remember { mutableStateOf("") }
  var confirmPin by remember { mutableStateOf("") }

  var errorMessage by remember { mutableStateOf<String?>(null) }
  var successMessage by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Lock, contentDescription = null, tint = LearnerBlue)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isConfigured) "Manage Security PIN" else "Set Security PIN",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = if (isConfigured) {
            "Protect app access with a 4-digit PIN. You can change your PIN or disable App Lock below."
          } else {
            "Set a 4-digit PIN to secure your educational resources, notes, and profile."
          },
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

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

        if (isConfigured) {
          OutlinedTextField(
            value = currentPin,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) currentPin = it },
            label = { Text("Enter Current 4-Digit PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("current_pin_input")
          )
        }

        OutlinedTextField(
          value = newPin,
          onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
          label = { Text(if (isConfigured) "New 4-Digit PIN" else "Create 4-Digit PIN") },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("new_pin_input")
        )

        OutlinedTextField(
          value = confirmPin,
          onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
          label = { Text("Confirm 4-Digit PIN") },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("confirm_pin_input")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          errorMessage = null
          successMessage = null

          if (isConfigured) {
            if (currentPin.length != 4) {
              errorMessage = "Enter your current 4-digit PIN."
              return@Button
            }
            if (!AppLockManager.verifyPin(context, currentPin)) {
              errorMessage = "Current PIN is incorrect."
              return@Button
            }
          }

          if (newPin.length != 4) {
            errorMessage = "New PIN must be exactly 4 digits."
            return@Button
          }

          if (newPin != confirmPin) {
            errorMessage = "PINs do not match."
            return@Button
          }

          if (AppLockManager.setPin(context, newPin)) {
            successMessage = "Security PIN saved successfully."
            onDismiss()
          } else {
            errorMessage = "Failed to save PIN."
          }
        },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue),
        modifier = Modifier.testTag("save_pin_button")
      ) {
        Text("Save PIN", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      if (isConfigured) {
        TextButton(
          onClick = {
            if (currentPin.length != 4) {
              errorMessage = "Enter your current PIN to disable App Lock."
              return@TextButton
            }
            if (AppLockManager.disableLock(context, currentPin)) {
              onDismiss()
            } else {
              errorMessage = "Incorrect current PIN."
            }
          },
          colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("disable_pin_button")
        ) {
          Text("Disable PIN")
        }
      } else {
        TextButton(onClick = onDismiss) {
          Text("Cancel")
        }
      }
    }
  )
}
