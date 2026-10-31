package com.example.ui.screens

import android.app.Activity
import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.remote.FirebaseManager
import com.example.data.remote.SupabaseClient
import com.example.data.repository.LearnerportRepository
import com.example.ui.components.HelpSupportModal
import com.example.ui.theme.LearnerBlue
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AuthScreenMode {
  WELCOME,
  EMAIL_FLOW,
  PHONE_FLOW,
  EMAIL_VERIFICATION,
  FORGOT_PASSWORD
}

enum class EmailSubTab {
  SIX_DIGIT_OTP,
  PASSWORD_LOGIN,
  PASSWORD_SIGNUP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthWelcomeScreen(
  onAuthSuccess: (isNewUserOrIncompleteProfile: Boolean) -> Unit,
  onContinueWithGoogle: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val activity = context as? Activity
  val coroutineScope = rememberCoroutineScope()

  var mode by remember { mutableStateOf(AuthScreenMode.WELCOME) }
  var showHelpModal by remember { mutableStateOf(false) }

  // General Status
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var successMessage by remember { mutableStateOf<String?>(null) }

  // --- Email Flow States ---
  var emailSubTab by remember { mutableStateOf(EmailSubTab.SIX_DIGIT_OTP) }
  var emailAddress by remember { mutableStateOf("") }
  var emailOtpSent by remember { mutableStateOf(false) }
  var emailOtpCode by remember { mutableStateOf("") }
  var emailOtpCooldownSeconds by remember { mutableIntStateOf(0) }
  var emailOtpAttempts by remember { mutableIntStateOf(0) }

  // Email/Password fields
  var fullName by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var termsAccepted by remember { mutableStateOf(false) }
  var isPasswordVisible by remember { mutableStateOf(false) }
  var isConfirmPasswordVisible by remember { mutableStateOf(false) }

  // Email Verification Link state
  var unverifiedEmail by remember { mutableStateOf("") }
  var resendCooldownSeconds by remember { mutableIntStateOf(0) }
  var verificationChecking by remember { mutableStateOf(false) }

  // --- Phone Flow States ---
  var countryCode by remember { mutableStateOf("+91") }
  var rawPhoneNumber by remember { mutableStateOf("") }
  var phoneOtpSent by remember { mutableStateOf(false) }
  var phoneOtpCode by remember { mutableStateOf("") }
  var phoneVerificationId by remember { mutableStateOf("") }
  var phoneResendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
  var phoneOtpCooldownSeconds by remember { mutableIntStateOf(0) }
  var phoneOtpAttempts by remember { mutableIntStateOf(0) }

  // Cooldown countdowns
  LaunchedEffect(phoneOtpCooldownSeconds) {
    if (phoneOtpCooldownSeconds > 0) {
      delay(1000L)
      phoneOtpCooldownSeconds -= 1
    }
  }

  LaunchedEffect(emailOtpCooldownSeconds) {
    if (emailOtpCooldownSeconds > 0) {
      delay(1000L)
      emailOtpCooldownSeconds -= 1
    }
  }

  LaunchedEffect(resendCooldownSeconds) {
    if (resendCooldownSeconds > 0) {
      delay(1000L)
      resendCooldownSeconds -= 1
    }
  }

  androidx.activity.compose.BackHandler(enabled = mode != AuthScreenMode.WELCOME) {
    mode = when (mode) {
      AuthScreenMode.EMAIL_VERIFICATION -> AuthScreenMode.EMAIL_FLOW
      else -> AuthScreenMode.WELCOME
    }
    errorMessage = null
    successMessage = null
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = when (mode) {
              AuthScreenMode.WELCOME -> ""
              AuthScreenMode.EMAIL_FLOW -> "Email Sign-In"
              AuthScreenMode.PHONE_FLOW -> "Phone Sign-In"
              AuthScreenMode.EMAIL_VERIFICATION -> "Email Verification"
              AuthScreenMode.FORGOT_PASSWORD -> "Reset Password"
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
        },
        navigationIcon = {
          if (mode != AuthScreenMode.WELCOME) {
            IconButton(onClick = {
              mode = when (mode) {
                AuthScreenMode.EMAIL_VERIFICATION -> AuthScreenMode.EMAIL_FLOW
                else -> AuthScreenMode.WELCOME
              }
              errorMessage = null
              successMessage = null
            }) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          }
        },
        actions = {
          IconButton(
            onClick = { showHelpModal = true },
            modifier = Modifier.testTag("auth_help_button")
          ) {
            Icon(
              imageVector = Icons.Default.HelpOutline,
              contentDescription = "Help & Support",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = if (mode == AuthScreenMode.WELCOME) Arrangement.SpaceBetween else Arrangement.Top
    ) {

      // ==========================================
      // 1. WELCOME SCREEN (Premium Options)
      // ==========================================
      if (mode == AuthScreenMode.WELCOME) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(top = 16.dp)
        ) {
          Image(
            painter = painterResource(id = R.drawable.learnerport_logo),
            contentDescription = "Learnerport Logo",
            modifier = Modifier.size(104.dp)
          )

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = buildAnnotatedString {
              withStyle(SpanStyle(color = LearnerBlue, fontWeight = FontWeight.Black, fontSize = 32.sp)) {
                append("Learner")
              }
              withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)) {
                append("port")
              }
            }
          )

          Text(
            text = "Open Educational Content Platform",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))

          Surface(
            shape = RoundedCornerShape(16.dp),
            color = LearnerBlue.copy(alpha = 0.08f)
          ) {
            Text(
              text = "Notes • Book PDFs • Question Papers • Thoughts",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = LearnerBlue,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Center Welcome Card / Options
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Text(
            text = "Welcome to Learnerport",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "Sign in to access your saved notes, upload textbooks, and connect with fellow learners.",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(4.dp))

          // OPTION 1: Continue with Google
          OutlinedButton(
            onClick = onContinueWithGoogle,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("auth_google_button"),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Image(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = "Official Google Logo",
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = "Continue with Google",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }

          // OPTION 2: Continue with Email
          Button(
            onClick = {
              mode = AuthScreenMode.EMAIL_FLOW
              errorMessage = null
              successMessage = null
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("auth_open_email_button"),
            colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(10.dp))
              Text("Continue with Email", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
          }

          // OPTION 3: Continue with Phone Number
          FilledTonalButton(
            onClick = {
              mode = AuthScreenMode.PHONE_FLOW
              errorMessage = null
              successMessage = null
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("auth_open_phone_button"),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                Icons.Default.Phone,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                "Continue with Phone Number",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
          text = "By continuing, you agree to Learnerport's Educational Content Policy & Terms of Service.",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
          lineHeight = 16.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(bottom = 12.dp)
        )

      }

      // ==========================================
      // 2. EMAIL AUTHENTICATION FLOW
      // ==========================================
      else if (mode == AuthScreenMode.EMAIL_FLOW) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Sub-tabs: 6-Digit OTP vs Password
          TabRow(
            selectedTabIndex = when (emailSubTab) {
              EmailSubTab.SIX_DIGIT_OTP -> 0
              EmailSubTab.PASSWORD_LOGIN -> 1
              EmailSubTab.PASSWORD_SIGNUP -> 2
            },
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            indicator = {},
            divider = {}
          ) {
            Tab(
              selected = emailSubTab == EmailSubTab.SIX_DIGIT_OTP,
              onClick = {
                emailSubTab = EmailSubTab.SIX_DIGIT_OTP
                errorMessage = null
                successMessage = null
              },
              text = { Text("6-Digit OTP", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
              selected = emailSubTab == EmailSubTab.PASSWORD_LOGIN,
              onClick = {
                emailSubTab = EmailSubTab.PASSWORD_LOGIN
                errorMessage = null
                successMessage = null
              },
              text = { Text("Log In", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
              selected = emailSubTab == EmailSubTab.PASSWORD_SIGNUP,
              onClick = {
                emailSubTab = EmailSubTab.PASSWORD_SIGNUP
                errorMessage = null
                successMessage = null
              },
              text = { Text("Sign Up", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
          }

          // Alerts
          if (errorMessage != null) {
            Surface(
              shape = RoundedCornerShape(10.dp),
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

          if (successMessage != null) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFDCFCE7),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = successMessage!!,
                color = Color(0xFF166534),
                fontSize = 12.sp,
                modifier = Modifier.padding(12.dp)
              )
            }
          }

          // TAB 1: EXACT 6-DIGIT EMAIL OTP
          if (emailSubTab == EmailSubTab.SIX_DIGIT_OTP) {
            if (!emailOtpSent) {
              Text(
                text = "Enter your email address to receive an instant, genuine 6-digit OTP.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
              )

              OutlinedTextField(
                value = emailAddress,
                onValueChange = { emailAddress = it.trim() },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = LearnerBlue) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("email_otp_input")
              )

              Button(
                onClick = {
                  errorMessage = null
                  if (emailAddress.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
                    errorMessage = "Please enter a valid email address."
                    return@Button
                  }

                  coroutineScope.launch {
                    isLoading = true
                    val res = SupabaseClient.sendEmailOtp(emailAddress)
                    isLoading = false

                    res.onSuccess {
                      emailOtpSent = true
                      emailOtpCooldownSeconds = 60
                      emailOtpAttempts = 0
                      successMessage = "A 6-digit verification code has been dispatched to $emailAddress."
                    }.onFailure { err ->
                      errorMessage = err.message ?: "Failed to dispatch email OTP. Verify network."
                    }
                  }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("send_email_otp_button"),
                colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue)
              ) {
                if (isLoading) {
                  CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                  Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Send 6-Digit OTP", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
              }
            } else {
              // 6-Digit Code Entry
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LearnerBlue.copy(alpha = 0.06f)),
                border = BorderStroke(1.dp, LearnerBlue.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier.padding(16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Text(
                    text = "Enter 6-Digit Code",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = LearnerBlue
                  )
                  Text(
                    text = "A verification code was sent to:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = emailAddress,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }

              OutlinedTextField(
                value = emailOtpCode,
                onValueChange = {
                  if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                    emailOtpCode = it
                  }
                },
                label = { Text("6-Digit OTP") },
                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = LearnerBlue) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(
                  fontSize = 24.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 8.sp,
                  textAlign = TextAlign.Center,
                  fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier.fillMaxWidth().testTag("email_otp_code_input")
              )

              Text(
                text = "⏱️ Code expires in 10 minutes. Attempt ${emailOtpAttempts + 1} of 5.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              Button(
                onClick = {
                  errorMessage = null
                  if (emailOtpCode.length != 6) {
                    errorMessage = "Please enter the complete 6-digit verification code."
                    return@Button
                  }
                  if (emailOtpAttempts >= 5) {
                    errorMessage = "Too many failed attempts. Please request a new code."
                    return@Button
                  }

                  coroutineScope.launch {
                    isLoading = true
                    emailOtpAttempts += 1
                    val res = SupabaseClient.verifyEmailOtp(context, emailAddress, emailOtpCode)
                    isLoading = false

                    res.onSuccess { session ->
                      LearnerportRepository.init(context)
                      onAuthSuccess(!session.isProfileCompleted)
                    }.onFailure { err ->
                      errorMessage = err.message ?: "Invalid code. Please try again."
                    }
                  }
                },
                enabled = !isLoading && emailOtpCode.length == 6,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("verify_email_otp_button"),
                colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue)
              ) {
                if (isLoading) {
                  CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Verify & Continue", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                TextButton(onClick = {
                  emailOtpSent = false
                  emailOtpCode = ""
                  errorMessage = null
                }) {
                  Text("Change Email", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                TextButton(
                  onClick = {
                    if (emailOtpCooldownSeconds == 0) {
                      coroutineScope.launch {
                        isLoading = true
                        val res = SupabaseClient.sendEmailOtp(emailAddress)
                        isLoading = false
                        res.onSuccess {
                          emailOtpCooldownSeconds = 60
                          emailOtpAttempts = 0
                          successMessage = "A fresh 6-digit code was sent to $emailAddress."
                        }.onFailure { e ->
                          errorMessage = "Resend failed: ${e.message}"
                        }
                      }
                    }
                  },
                  enabled = emailOtpCooldownSeconds == 0
                ) {
                  Text(
                    text = if (emailOtpCooldownSeconds > 0) "Resend in ${emailOtpCooldownSeconds}s" else "Resend Code",
                    fontSize = 12.sp,
                    color = if (emailOtpCooldownSeconds > 0) MaterialTheme.colorScheme.onSurfaceVariant else LearnerBlue
                  )
                }
              }
            }
          }

          // TAB 2: PASSWORD LOG IN
          else if (emailSubTab == EmailSubTab.PASSWORD_LOGIN) {
            OutlinedTextField(
              value = emailAddress,
              onValueChange = { emailAddress = it.trim() },
              label = { Text("Email Address") },
              leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = LearnerBlue) },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().testTag("login_email_input")
            )

            OutlinedTextField(
              value = password,
              onValueChange = { password = it },
              label = { Text("Password") },
              leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = LearnerBlue) },
              trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                  Icon(if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                }
              },
              visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().testTag("login_password_input")
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End
            ) {
              Text(
                text = "Forgot Password?",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = LearnerBlue,
                modifier = Modifier.clickable {
                  mode = AuthScreenMode.FORGOT_PASSWORD
                  errorMessage = null
                  successMessage = null
                }
              )
            }

            Button(
              onClick = {
                errorMessage = null
                if (emailAddress.isBlank() || password.isBlank()) {
                  errorMessage = "Please enter both email and password."
                  return@Button
                }

                coroutineScope.launch {
                  isLoading = true
                  val res = FirebaseManager.signInWithEmail(emailAddress, password)
                  isLoading = false

                  res.onSuccess { (user, isVerified) ->
                    if (!isVerified) {
                      unverifiedEmail = emailAddress.trim()
                      mode = AuthScreenMode.EMAIL_VERIFICATION
                      errorMessage = null
                      successMessage = "Please check your Gmail inbox to verify your account before logging in."
                    } else {
                      LearnerportRepository.notifyUserVerified()
                      val isCompleted = FirebaseManager.checkIsProfileCompleted(user.uid)
                      onAuthSuccess(!isCompleted)
                    }
                  }.onFailure { err ->
                    errorMessage = err.message ?: "Invalid email or password."
                  }
                }
              },
              enabled = !isLoading,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("login_submit_button"),
              colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue)
            ) {
              if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              } else {
                Text("Log In", fontWeight = FontWeight.Bold, fontSize = 15.sp)
              }
            }
          }

          // TAB 3: PASSWORD SIGN UP
          else if (emailSubTab == EmailSubTab.PASSWORD_SIGNUP) {
            OutlinedTextField(
              value = fullName,
              onValueChange = { fullName = it },
              label = { Text("Full Name") },
              leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = LearnerBlue) },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().testTag("signup_fullname_input")
            )

            OutlinedTextField(
              value = emailAddress,
              onValueChange = { emailAddress = it.trim() },
              label = { Text("Email Address") },
              leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = LearnerBlue) },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().testTag("signup_email_input")
            )

            OutlinedTextField(
              value = password,
              onValueChange = { password = it },
              label = { Text("Password (min 6 characters)") },
              leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = LearnerBlue) },
              trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                  Icon(if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                }
              },
              visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().testTag("signup_password_input")
            )

            OutlinedTextField(
              value = confirmPassword,
              onValueChange = { confirmPassword = it },
              label = { Text("Confirm Password") },
              leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = LearnerBlue) },
              trailingIcon = {
                IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                  Icon(if (isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                }
              },
              visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().testTag("signup_confirm_password_input")
            )

            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth().clickable { termsAccepted = !termsAccepted }
            ) {
              Checkbox(
                checked = termsAccepted,
                onCheckedChange = { termsAccepted = it },
                modifier = Modifier.testTag("signup_terms_checkbox")
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "I accept the Terms & Conditions and understand upload permissions.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Button(
              onClick = {
                errorMessage = null
                if (fullName.isBlank()) {
                  errorMessage = "Please enter your full name."
                  return@Button
                }
                if (emailAddress.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
                  errorMessage = "Please enter a valid email address."
                  return@Button
                }
                if (password.length < 6) {
                  errorMessage = "Password must be at least 6 characters."
                  return@Button
                }
                if (password != confirmPassword) {
                  errorMessage = "Passwords do not match."
                  return@Button
                }
                if (!termsAccepted) {
                  errorMessage = "You must accept the terms & conditions to proceed."
                  return@Button
                }

                coroutineScope.launch {
                  isLoading = true
                  val res = FirebaseManager.signUpWithEmail(emailAddress, password, fullName)
                  isLoading = false

                  res.onSuccess {
                    unverifiedEmail = emailAddress.trim()
                    resendCooldownSeconds = 45
                    mode = AuthScreenMode.EMAIL_VERIFICATION
                    errorMessage = null
                    successMessage = "Verification link sent to your Gmail inbox."
                  }.onFailure { err ->
                    errorMessage = err.message ?: "Sign up failed."
                  }
                }
              },
              enabled = !isLoading,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("signup_submit_button"),
              colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue)
            ) {
              if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              } else {
                Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
              }
            }
          }
        }
      }

      // ==========================================
      // 3. PHONE NUMBER AUTHENTICATION (Real 6-Digit SMS OTP)
      // ==========================================
      else if (mode == AuthScreenMode.PHONE_FLOW) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Text(
            text = "Authenticate with Phone Number",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "A genuine 6-digit SMS verification code will be delivered to your mobile phone.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
          )

          // Alerts
          if (errorMessage != null) {
            Surface(
              shape = RoundedCornerShape(10.dp),
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

          if (successMessage != null) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFDCFCE7),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = successMessage!!,
                color = Color(0xFF166534),
                fontSize = 12.sp,
                modifier = Modifier.padding(12.dp)
              )
            }
          }

          if (!phoneOtpSent) {
            // STEP 1: Phone Number Input
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                  .width(84.dp)
                  .height(56.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = "🇮🇳 $countryCode",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }

              OutlinedTextField(
                value = rawPhoneNumber,
                onValueChange = {
                  val digits = it.filter { ch -> ch.isDigit() }
                  if (digits.length <= 10) rawPhoneNumber = digits
                },
                label = { Text("10-Digit Mobile Number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = LearnerBlue) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).testTag("phone_number_input")
              )
            }

            Button(
              onClick = {
                errorMessage = null
                if (rawPhoneNumber.length < 10) {
                  errorMessage = "Please enter a valid 10-digit mobile number."
                  return@Button
                }
                if (activity == null) {
                  errorMessage = "Unable to access device activity for SMS verification."
                  return@Button
                }

                val fullPhone = "$countryCode$rawPhoneNumber"
                isLoading = true
                FirebaseManager.sendPhoneOtp(
                  activity = activity,
                  phoneNumber = fullPhone,
                  onCodeSent = { verificationId, token ->
                    isLoading = false
                    phoneVerificationId = verificationId
                    phoneResendToken = token
                    phoneOtpSent = true
                    phoneOtpCooldownSeconds = 60
                    phoneOtpAttempts = 0
                    successMessage = "A 6-digit SMS code was sent to $fullPhone."
                  },
                  onVerificationCompleted = { credential ->
                    coroutineScope.launch {
                      val res = FirebaseManager.signInWithPhoneCredential(credential)
                      isLoading = false
                      res.onSuccess { user ->
                        val isComplete = FirebaseManager.checkIsProfileCompleted(user.uid)
                        onAuthSuccess(!isComplete)
                      }.onFailure { e ->
                        errorMessage = e.message ?: "Instant verification failed."
                      }
                    }
                  },
                  onError = { err ->
                    isLoading = false
                    errorMessage = err
                  }
                )
              },
              enabled = !isLoading && rawPhoneNumber.length == 10,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("send_phone_otp_button"),
              colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue)
            ) {
              if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              } else {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send 6-Digit SMS OTP", fontWeight = FontWeight.Bold, fontSize = 15.sp)
              }
            }

          } else {
            // STEP 2: Phone 6-Digit OTP Entry
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = LearnerBlue.copy(alpha = 0.06f)),
              border = BorderStroke(1.dp, LearnerBlue.copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = "SMS Verification",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = LearnerBlue
                )
                Text(
                  text = "SMS verification code dispatched to:",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val masked = if (rawPhoneNumber.length == 10) {
                  "$countryCode ••••• ••${rawPhoneNumber.takeLast(3)}"
                } else {
                  "$countryCode $rawPhoneNumber"
                }
                Text(
                  text = masked,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }

            OutlinedTextField(
              value = phoneOtpCode,
              onValueChange = {
                if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                  phoneOtpCode = it
                }
              },
              label = { Text("Enter 6-Digit SMS Code") },
              leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = LearnerBlue) },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              shape = RoundedCornerShape(12.dp),
              textStyle = LocalTextStyle.current.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace
              ),
              modifier = Modifier.fillMaxWidth().testTag("phone_otp_code_input")
            )

            Text(
              text = "⏱️ Code expires in 60 seconds. Attempt ${phoneOtpAttempts + 1} of 5.",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
              onClick = {
                errorMessage = null
                if (phoneOtpCode.length != 6) {
                  errorMessage = "Please enter all 6 digits of the SMS code."
                  return@Button
                }
                if (phoneOtpAttempts >= 5) {
                  errorMessage = "Too many failed attempts. Please request a new SMS code."
                  return@Button
                }

                coroutineScope.launch {
                  isLoading = true
                  phoneOtpAttempts += 1
                  val res = FirebaseManager.signInWithPhoneOtp(phoneVerificationId, phoneOtpCode)
                  isLoading = false

                  res.onSuccess { user ->
                    val isComplete = FirebaseManager.checkIsProfileCompleted(user.uid)
                    onAuthSuccess(!isComplete)
                  }.onFailure { err ->
                    errorMessage = err.message ?: "Invalid SMS code. Please try again."
                  }
                }
              },
              enabled = !isLoading && phoneOtpCode.length == 6,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("verify_phone_otp_button"),
              colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue)
            ) {
              if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verify & Continue", fontWeight = FontWeight.Bold, fontSize = 15.sp)
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              TextButton(onClick = {
                phoneOtpSent = false
                phoneOtpCode = ""
                errorMessage = null
              }) {
                Text("Change Phone", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }

              TextButton(
                onClick = {
                  if (phoneOtpCooldownSeconds == 0 && activity != null) {
                    val fullPhone = "$countryCode$rawPhoneNumber"
                    isLoading = true
                    FirebaseManager.sendPhoneOtp(
                      activity = activity,
                      phoneNumber = fullPhone,
                      resendToken = phoneResendToken,
                      onCodeSent = { verificationId, token ->
                        isLoading = false
                        phoneVerificationId = verificationId
                        phoneResendToken = token
                        phoneOtpCooldownSeconds = 60
                        phoneOtpAttempts = 0
                        successMessage = "A fresh SMS code was dispatched."
                      },
                      onVerificationCompleted = { cred ->
                        coroutineScope.launch {
                          val res = FirebaseManager.signInWithPhoneCredential(cred)
                          isLoading = false
                          res.onSuccess { user ->
                            val isComplete = FirebaseManager.checkIsProfileCompleted(user.uid)
                            onAuthSuccess(!isComplete)
                          }
                        }
                      },
                      onError = { err ->
                        isLoading = false
                        errorMessage = err
                      }
                    )
                  }
                },
                enabled = phoneOtpCooldownSeconds == 0
              ) {
                Text(
                  text = if (phoneOtpCooldownSeconds > 0) "Resend in ${phoneOtpCooldownSeconds}s" else "Resend SMS Code",
                  fontSize = 12.sp,
                  color = if (phoneOtpCooldownSeconds > 0) MaterialTheme.colorScheme.onSurfaceVariant else LearnerBlue
                )
              }
            }
          }
        }
      }

      // ==========================================
      // 4. EMAIL LINK VERIFICATION
      // ==========================================
      else if (mode == AuthScreenMode.EMAIL_VERIFICATION) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Box(
            modifier = Modifier
              .size(88.dp)
              .clip(CircleShape)
              .background(LearnerBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.MarkEmailUnread,
              contentDescription = "Verify Email",
              tint = LearnerBlue,
              modifier = Modifier.size(44.dp)
            )
          }

          Text(
            text = "Verify Your Account",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )

          Surface(
            shape = RoundedCornerShape(16.dp),
            color = LearnerBlue.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, LearnerBlue.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "Please check your Gmail inbox to verify your account",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = LearnerBlue,
                textAlign = TextAlign.Center
              )
              Text(
                text = "We have sent an automatic verification email with a secure link from Learnerport to:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
              )
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(top = 2.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Email, contentDescription = null, tint = LearnerBlue, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = unverifiedEmail.ifBlank { emailAddress }.ifBlank { "your Gmail address" },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }
          }

          if (errorMessage != null) {
            Surface(
              shape = RoundedCornerShape(10.dp),
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

          if (successMessage != null) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFDCFCE7),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = successMessage!!,
                color = Color(0xFF166534),
                fontSize = 12.sp,
                modifier = Modifier.padding(12.dp)
              )
            }
          }

          // Open Gmail App Action Button
          OutlinedButton(
            onClick = {
              val gmailIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.gm")
              if (gmailIntent != null) {
                context.startActivity(gmailIntent)
              } else {
                val emailIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                  addCategory(android.content.Intent.CATEGORY_APP_EMAIL)
                  addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                  context.startActivity(emailIntent)
                } catch (e: Exception) {
                  android.widget.Toast.makeText(context, "Please open your Gmail application.", android.widget.Toast.LENGTH_SHORT).show()
                }
              }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_gmail_app_button")
          ) {
            Icon(Icons.Default.Email, contentDescription = null, tint = LearnerBlue, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Gmail App", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
          }

          // I've Verified My Email Button
          Button(
            onClick = {
              errorMessage = null
              successMessage = null
              coroutineScope.launch {
                verificationChecking = true
                val checkRes = FirebaseManager.reloadAndCheckEmailVerified()
                verificationChecking = false
                checkRes.onSuccess { isVerified ->
                  if (isVerified) {
                    android.widget.Toast.makeText(context, "Email verified successfully!", android.widget.Toast.LENGTH_SHORT).show()
                    LearnerportRepository.notifyUserVerified()
                    val uid = FirebaseManager.currentUser.value?.uid ?: ""
                    val isComplete = FirebaseManager.checkIsProfileCompleted(uid)
                    onAuthSuccess(!isComplete)
                  } else {
                    errorMessage = "Your email has not been verified yet. Please click the link sent to your Gmail inbox, then tap this button again."
                  }
                }.onFailure { e ->
                  errorMessage = "Could not check verification status: ${e.message}."
                }
              }
            },
            enabled = !verificationChecking,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("check_verified_button"),
            colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue)
          ) {
            if (verificationChecking) {
              CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Checking Status...")
            } else {
              Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("I've Verified My Email", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
          }

          // Resend Verification Email Button
          TextButton(
            onClick = {
              if (resendCooldownSeconds == 0) {
                coroutineScope.launch {
                  val sendRes = FirebaseManager.sendEmailVerification()
                  sendRes.onSuccess {
                    resendCooldownSeconds = 45
                    successMessage = "A fresh verification link was sent to your Gmail inbox."
                    errorMessage = null
                  }.onFailure { e ->
                    errorMessage = "Failed to resend: ${e.message}"
                  }
                }
              }
            },
            enabled = resendCooldownSeconds == 0,
            modifier = Modifier.testTag("resend_verification_button")
          ) {
            Text(
              text = if (resendCooldownSeconds > 0) "Resend link in ${resendCooldownSeconds}s" else "Didn't receive email? Resend link",
              fontSize = 13.sp,
              color = if (resendCooldownSeconds > 0) MaterialTheme.colorScheme.onSurfaceVariant else LearnerBlue
            )
          }

          TextButton(onClick = {
            mode = AuthScreenMode.EMAIL_FLOW
            errorMessage = null
            successMessage = null
          }) {
            Text("Back to Email Sign-In", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }

      // ==========================================
      // 5. FORGOT PASSWORD
      // ==========================================
      else if (mode == AuthScreenMode.FORGOT_PASSWORD) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Text(
            text = "Enter your registered email address to receive password reset instructions.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
          )

          if (errorMessage != null) {
            Surface(
              shape = RoundedCornerShape(10.dp),
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

          if (successMessage != null) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFDCFCE7),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = successMessage!!,
                color = Color(0xFF166534),
                fontSize = 12.sp,
                modifier = Modifier.padding(12.dp)
              )
            }
          }

          OutlinedTextField(
            value = emailAddress,
            onValueChange = { emailAddress = it.trim() },
            label = { Text("Registered Email Address") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = LearnerBlue) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("forgot_email_input")
          )

          Button(
            onClick = {
              if (emailAddress.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
                errorMessage = "Please enter a valid email address."
                return@Button
              }

              coroutineScope.launch {
                isLoading = true
                val res = FirebaseManager.sendPasswordReset(emailAddress)
                isLoading = false
                res.onSuccess {
                  successMessage = "Password reset instructions sent to $emailAddress."
                  errorMessage = null
                }.onFailure { err ->
                  errorMessage = err.message ?: "Failed to send reset link."
                }
              }
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("send_reset_button"),
            colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue)
          ) {
            if (isLoading) {
              CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
              Text("Send Reset Link", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
          }

          TextButton(
            onClick = { mode = AuthScreenMode.EMAIL_FLOW },
            modifier = Modifier.align(Alignment.CenterHorizontally)
          ) {
            Text("Back to Email Sign-In", color = LearnerBlue, fontWeight = FontWeight.SemiBold)
          }
        }
      }

    }
  }

  // Help & Support Modal
  if (showHelpModal) {
    HelpSupportModal(onClose = { showHelpModal = false })
  }
}
