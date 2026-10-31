package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LearnerBlue
import com.example.ui.theme.PortBlack
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
  onSplashFinished: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isDark = MaterialTheme.colorScheme.background.red < 0.2f
  val portTextColor = if (isDark) Color.White else PortBlack

  // 2.5 second automatic splash transition without any skip button
  LaunchedEffect(Unit) {
    delay(2500L)
    onSplashFinished()
  }

  // Smooth floating and pulsing animations
  val infiniteTransition = rememberInfiniteTransition(label = "splash_mascot_anim")

  val floatOffset by infiniteTransition.animateFloat(
    initialValue = -8f,
    targetValue = 8f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "floating_offset"
  )

  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.98f,
    targetValue = 1.03f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulsing_scale"
  )

  val haloAlpha by infiniteTransition.animateFloat(
    initialValue = 0.2f,
    targetValue = 0.5f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "halo_glow"
  )

  // Smooth progress bar over exactly 2.5s (2500ms)
  val progressAnim = remember { Animatable(0f) }
  LaunchedEffect(Unit) {
    progressAnim.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 2500, easing = LinearEasing)
    )
  }

  val contentAlpha = remember { Animatable(0f) }
  LaunchedEffect(Unit) {
    contentAlpha.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
    )
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = if (isDark) {
            listOf(Color(0xFF0B111E), Color(0xFF141E33), Color(0xFF0F172A))
          } else {
            listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC), Color(0xFFEFF6FF))
          }
        )
      )
      .systemBarsPadding()
      .testTag("splash_screen_container")
  ) {
    // Center Hero Animation Content with transparent logo
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp)
        .alpha(contentAlpha.value),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Glow Halo Ring + Logo (NO white background box or extra border)
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(200.dp)
      ) {
        // Subtle ambient radial glow behind the logo
        Box(
          modifier = Modifier
            .size(190.dp)
            .scale(pulseScale)
            .alpha(haloAlpha)
            .clip(CircleShape)
            .background(
              Brush.radialGradient(
                colors = listOf(
                  LearnerBlue.copy(alpha = 0.35f),
                  LearnerBlue.copy(alpha = 0.1f),
                  Color.Transparent
                )
              )
            )
        )

        // Transparent original application logo with smooth floating motion
        Image(
          painter = painterResource(id = R.drawable.learnerport_logo),
          contentDescription = "Learnerport Logo",
          modifier = Modifier
            .size(140.dp)
            .offset(y = floatOffset.dp)
            .scale(pulseScale)
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Wordmark: "Learner" in Blue + "port" in Black/White
      Text(
        text = buildAnnotatedString {
          withStyle(
            style = SpanStyle(
              color = LearnerBlue,
              fontWeight = FontWeight.Black,
              fontSize = 36.sp
            )
          ) {
            append("Learner")
          }
          withStyle(
            style = SpanStyle(
              color = portTextColor,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 36.sp
            )
          ) {
            append("port")
          }
        },
        letterSpacing = (-1).sp
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Tagline
      Text(
        text = "Discover • Learn • Share • Excel",
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp
      )

      Spacer(modifier = Modifier.height(14.dp))

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = LearnerBlue.copy(alpha = 0.1f)
      ) {
        Text(
          text = "Educational Content-Sharing Platform",
          color = LearnerBlue,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
      }
    }

    // Bottom Animated Progress Indicator (NO skip button)
    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .padding(horizontal = 36.dp, vertical = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      LinearProgressIndicator(
        progress = { progressAnim.value },
        modifier = Modifier
          .fillMaxWidth()
          .height(4.dp)
          .clip(RoundedCornerShape(2.dp)),
        color = LearnerBlue,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Loading your educational resources...",
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
      )
    }
  }
}
