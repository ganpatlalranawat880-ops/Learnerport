package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.AppLockManager
import com.example.ui.theme.LearnerBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppLockScreen(
  onUnlocked: () -> Unit,
  onResetRequested: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var pinInput by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var isShaking by remember { mutableStateOf(false) }

  val shakeOffset = remember { Animatable(0f) }

  fun triggerShake() {
    coroutineScope.launch {
      repeat(3) {
        shakeOffset.animateTo(20f, tween(50, easing = LinearEasing))
        shakeOffset.animateTo(-20f, tween(50, easing = LinearEasing))
      }
      shakeOffset.animateTo(0f, tween(50, easing = LinearEasing))
    }
  }

  fun onKeyClick(digit: String) {
    if (pinInput.length < 4) {
      val next = pinInput + digit
      pinInput = next
      errorMessage = null
      if (next.length == 4) {
        val lockoutSec = AppLockManager.getLockoutSecondsRemaining(context)
        if (lockoutSec > 0) {
          errorMessage = "Too many attempts. Try again in ${lockoutSec}s."
          pinInput = ""
          triggerShake()
          return
        }

        if (AppLockManager.verifyPin(context, next)) {
          onUnlocked()
        } else {
          errorMessage = "Incorrect PIN"
          pinInput = ""
          triggerShake()
        }
      }
    }
  }

  fun onDeleteClick() {
    if (pinInput.isNotEmpty()) {
      pinInput = pinInput.dropLast(1)
      errorMessage = null
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(CircleShape)
          .background(LearnerBlue.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Lock,
          contentDescription = "Lock",
          tint = LearnerBlue,
          modifier = Modifier.size(36.dp)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "Enter Security PIN",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )

      Text(
        text = "Your Learnerport account is protected",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
      )

      Spacer(modifier = Modifier.height(28.dp))

      // 4 PIN Dots with shake
      Row(
        modifier = Modifier
          .offset(x = shakeOffset.value.dp)
          .testTag("pin_dots_row"),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
      ) {
        for (i in 0 until 4) {
          val isFilled = i < pinInput.length
          Box(
            modifier = Modifier
              .size(18.dp)
              .clip(CircleShape)
              .background(
                if (isFilled) LearnerBlue else MaterialTheme.colorScheme.surfaceVariant
              )
          )
        }
      }

      if (errorMessage != null) {
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = errorMessage!!,
          color = MaterialTheme.colorScheme.error,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )
      } else {
        Spacer(modifier = Modifier.height(30.dp))
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Keypad numbers 1 to 9
      val keypad = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
      )

      Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        for (row in keypad) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            for (key in row) {
              if (key.isEmpty()) {
                Spacer(modifier = Modifier.size(72.dp))
              } else if (key == "DEL") {
                Box(
                  modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .clickable { onDeleteClick() },
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              } else {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                  modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .clickable { onKeyClick(key) }
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = key,
                      fontSize = 24.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      TextButton(
        onClick = onResetRequested,
        modifier = Modifier.testTag("forgot_pin_button")
      ) {
        Text("Forgot PIN? Reset via Account", fontSize = 13.sp, color = LearnerBlue)
      }
    }
  }
}
