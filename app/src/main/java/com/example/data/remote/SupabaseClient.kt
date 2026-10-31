package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class SupabaseUserSession(
  val userId: String,
  val email: String,
  val accessToken: String,
  val refreshToken: String,
  val username: String = "",
  val displayName: String = "",
  val avatarUrl: String = "",
  val isProfileCompleted: Boolean = false,
  val dateOfBirth: String = "",
  val educationalLevel: String = "",
  val courseOrDegree: String = "",
  val fieldOfStudy: String = "",
  val bio: String = ""
)

object SupabaseClient {
  private const val TAG = "SupabaseClient"
  private const val PREFS_NAME = "supabase_session_prefs"
  private const val KEY_ACCESS_TOKEN = "access_token"
  private const val KEY_REFRESH_TOKEN = "refresh_token"
  private const val KEY_USER_ID = "user_id"
  private const val KEY_USER_EMAIL = "user_email"
  private const val KEY_USERNAME = "username"
  private const val KEY_DISPLAY_NAME = "display_name"
  private const val KEY_AVATAR_URL = "avatar_url"
  private const val KEY_IS_PROFILE_COMPLETED = "is_profile_completed"
  private const val KEY_DOB = "date_of_birth"
  private const val KEY_EDU_LEVEL = "educational_level"
  private const val KEY_COURSE = "course_or_degree"
  private const val KEY_FIELD_OF_STUDY = "field_of_study"
  private const val KEY_BIO = "bio"

  private const val CONFIG_PREFS = "supabase_config_prefs"
  private const val KEY_RUNTIME_URL = "runtime_supabase_url"
  private const val KEY_RUNTIME_KEY = "runtime_supabase_anon_key"
  private const val KEY_RUNTIME_GOOGLE_CLIENT_ID = "runtime_google_client_id"

  private var appContext: Context? = null

  private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

  fun getSupabaseUrl(context: Context? = null): String {
    val ctx = context ?: appContext
    val runtime = ctx?.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)?.getString(KEY_RUNTIME_URL, null)
    if (!runtime.isNullOrBlank()) return runtime.trim().removeSuffix("/")
    return try {
      BuildConfig.SUPABASE_URL.trim().removeSuffix("/")
    } catch (e: Throwable) {
      ""
    }
  }

  fun getSupabaseAnonKey(context: Context? = null): String {
    val ctx = context ?: appContext
    val runtime = ctx?.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)?.getString(KEY_RUNTIME_KEY, null)
    if (!runtime.isNullOrBlank()) return runtime.trim()
    return try {
      BuildConfig.SUPABASE_ANON_KEY.trim()
    } catch (e: Throwable) {
      ""
    }
  }

  fun getGoogleServerClientId(context: Context? = null): String {
    val ctx = context ?: appContext
    val runtime = ctx?.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)?.getString(KEY_RUNTIME_GOOGLE_CLIENT_ID, null)
    if (!runtime.isNullOrBlank()) return runtime.trim()
    return try {
      val field = BuildConfig::class.java.getField("GOOGLE_SERVER_CLIENT_ID")
      (field.get(null) as? String)?.trim() ?: ""
    } catch (e: Throwable) {
      ""
    }
  }

  fun setRuntimeCredentials(context: Context, url: String, anonKey: String, googleClientId: String = "") {
    appContext = context.applicationContext
    context.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE).edit()
      .putString(KEY_RUNTIME_URL, url.trim().removeSuffix("/"))
      .putString(KEY_RUNTIME_KEY, anonKey.trim())
      .putString(KEY_RUNTIME_GOOGLE_CLIENT_ID, googleClientId.trim())
      .apply()
  }

  val supabaseUrl: String
    get() = getSupabaseUrl()

  val supabaseAnonKey: String
    get() = getSupabaseAnonKey()

  fun isConfigured(context: Context? = null): Boolean {
    val url = getSupabaseUrl(context)
    val key = getSupabaseAnonKey(context)
    return url.isNotBlank() &&
      !url.contains("your-project-ref") &&
      !url.contains("your-project-id") &&
      url.startsWith("https://") &&
      key.isNotBlank() &&
      !key.contains("placeholder")
  }

  fun getGoogleBrowserAuthUrl(redirectTo: String = "learnerport://login-callback"): String {
    return "$supabaseUrl/auth/v1/authorize?provider=google&redirect_to=$redirectTo"
  }

  suspend fun handleOAuthCallback(context: Context?, uri: android.net.Uri): Result<SupabaseUserSession> = withContext(Dispatchers.IO) {
    try {
      var accessToken: String? = uri.getQueryParameter("access_token")
      var refreshToken: String? = uri.getQueryParameter("refresh_token")

      // Supabase OAuth implicit flow redirects with hash fragment: #access_token=...&refresh_token=...
      val fragment = uri.fragment
      if (fragment != null) {
        val pairs = fragment.split("&")
        for (pair in pairs) {
          val parts = pair.split("=")
          if (parts.size == 2) {
            if (parts[0] == "access_token") accessToken = parts[1]
            if (parts[0] == "refresh_token") refreshToken = parts[1]
          }
        }
      }

      if (accessToken.isNullOrBlank()) {
        val errorDesc = uri.getQueryParameter("error_description") ?: uri.getQueryParameter("error") ?: "No access token in callback"
        return@withContext Result.failure(IOException(errorDesc))
      }

      val url = "$supabaseUrl/auth/v1/user"
      val request = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer $accessToken")
        .header("apikey", supabaseAnonKey)
        .get()
        .build()

      val response = okHttpClient.newCall(request).execute()
      val body = response.body?.string() ?: ""
      if (!response.isSuccessful) {
        return@withContext Result.failure(IOException("Failed to fetch user profile: HTTP ${response.code}"))
      }

      val userJson = JSONObject(body)
      val userId = userJson.getString("id")
      val email = userJson.optString("email", "")
      val userMeta = userJson.optJSONObject("user_metadata")
      val displayName = userMeta?.optString("full_name") ?: userMeta?.optString("name") ?: email.substringBefore("@")
      val avatarUrl = userMeta?.optString("avatar_url") ?: userMeta?.optString("picture") ?: ""

      val profileRes = fetchProfile(userId, accessToken)
      val existing = profileRes.getOrNull()
      val isCompleted = existing?.isProfileCompleted == true && !existing.username.isNullOrBlank()

      val session = SupabaseUserSession(
        userId = userId,
        email = email,
        accessToken = accessToken,
        refreshToken = refreshToken ?: "",
        username = existing?.username ?: "",
        displayName = existing?.displayName?.takeIf { it.isNotBlank() } ?: displayName,
        avatarUrl = existing?.avatarUrl?.takeIf { it.isNotBlank() } ?: avatarUrl,
        isProfileCompleted = isCompleted,
        dateOfBirth = existing?.dateOfBirth ?: "",
        educationalLevel = existing?.educationalLevel ?: "",
        courseOrDegree = existing?.courseOrDegree ?: "",
        fieldOfStudy = existing?.fieldOfStudy ?: "",
        bio = existing?.bio ?: ""
      )
      saveSession(context, session)
      Result.success(session)
    } catch (e: Throwable) {
      Log.e(TAG, "OAuth callback handling failed", e)
      Result.failure(e)
    }
  }

  private val _currentSession = MutableStateFlow<SupabaseUserSession?>(null)
  val currentSession: StateFlow<SupabaseUserSession?> = _currentSession.asStateFlow()

  val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .addInterceptor { chain ->
        val original = chain.request()
        val currentKey = supabaseAnonKey
        val token = _currentSession.value?.accessToken?.takeIf { it.isNotBlank() } ?: currentKey
        val requestBuilder = original.newBuilder()
          .header("apikey", currentKey)
          .header("Authorization", "Bearer $token")
        if (original.header("Content-Type") == null) {
          requestBuilder.header("Content-Type", "application/json")
        }
        chain.proceed(requestBuilder.build())
      }
      .build()
  }

  fun init(context: Context) {
    appContext = context.applicationContext
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val token = prefs.getString(KEY_ACCESS_TOKEN, null)
    val userId = prefs.getString(KEY_USER_ID, null)
    val email = prefs.getString(KEY_USER_EMAIL, null)

    if (!token.isNullOrBlank() && !userId.isNullOrBlank() && !email.isNullOrBlank()) {
      _currentSession.value = SupabaseUserSession(
        userId = userId,
        email = email,
        accessToken = token,
        refreshToken = prefs.getString(KEY_REFRESH_TOKEN, "") ?: "",
        username = prefs.getString(KEY_USERNAME, "") ?: "",
        displayName = prefs.getString(KEY_DISPLAY_NAME, "") ?: "",
        avatarUrl = prefs.getString(KEY_AVATAR_URL, "") ?: "",
        isProfileCompleted = prefs.getBoolean(KEY_IS_PROFILE_COMPLETED, false),
        dateOfBirth = prefs.getString(KEY_DOB, "") ?: "",
        educationalLevel = prefs.getString(KEY_EDU_LEVEL, "") ?: "",
        courseOrDegree = prefs.getString(KEY_COURSE, "") ?: "",
        fieldOfStudy = prefs.getString(KEY_FIELD_OF_STUDY, "") ?: "",
        bio = prefs.getString(KEY_BIO, "") ?: ""
      )
    }
  }

  fun saveSession(context: Context?, session: SupabaseUserSession?) {
    _currentSession.value = session
    if (context == null) return
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (session != null) {
      prefs.edit()
        .putString(KEY_ACCESS_TOKEN, session.accessToken)
        .putString(KEY_REFRESH_TOKEN, session.refreshToken)
        .putString(KEY_USER_ID, session.userId)
        .putString(KEY_USER_EMAIL, session.email)
        .putString(KEY_USERNAME, session.username)
        .putString(KEY_DISPLAY_NAME, session.displayName)
        .putString(KEY_AVATAR_URL, session.avatarUrl)
        .putBoolean(KEY_IS_PROFILE_COMPLETED, session.isProfileCompleted)
        .putString(KEY_DOB, session.dateOfBirth)
        .putString(KEY_EDU_LEVEL, session.educationalLevel)
        .putString(KEY_COURSE, session.courseOrDegree)
        .putString(KEY_FIELD_OF_STUDY, session.fieldOfStudy)
        .putString(KEY_BIO, session.bio)
        .apply()
    } else {
      prefs.edit().clear().apply()
    }
  }

  suspend fun signUpWithEmail(
    context: Context?,
    email: String,
    password: String,
    displayName: String
  ): Result<SupabaseUserSession> = withContext(Dispatchers.IO) {
    if (!isConfigured()) {
      return@withContext Result.failure(
        IllegalStateException("Supabase credentials not configured in .env or Secrets. Set SUPABASE_URL and SUPABASE_ANON_KEY to proceed.")
      )
    }

    try {
      val url = "$supabaseUrl/auth/v1/signup"
      val bodyJson = JSONObject().apply {
        put("email", email.trim())
        put("password", password)
        put("data", JSONObject().apply {
          put("display_name", displayName.trim())
        })
      }

      val request = Request.Builder()
        .url(url)
        .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
        .build()

      val response = okHttpClient.newCall(request).execute()
      val responseBody = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        val errObj = try { JSONObject(responseBody) } catch (e: Exception) { null }
        val msg = errObj?.optString("msg") ?: errObj?.optString("error_description") ?: "Sign up failed (HTTP ${response.code})"
        val userFriendly = when {
          msg.contains("already registered", ignoreCase = true) || msg.contains("already exists", ignoreCase = true) ->
            "An account with this email address already exists. Please log in instead."
          msg.contains("Password should be at least", ignoreCase = true) ->
            "Password must be at least 6 characters."
          msg.contains("invalid email", ignoreCase = true) ->
            "Please provide a valid email address."
          else -> msg
        }
        return@withContext Result.failure(IOException(userFriendly))
      }

      val json = JSONObject(responseBody)
      val accessToken = json.optString("access_token", "")
      val refreshToken = json.optString("refresh_token", "")
      val userObj = json.optJSONObject("user") ?: json
      val userId = userObj.optString("id")

      if (userId.isBlank()) {
        return@withContext Result.failure(IOException("Sign up response did not contain a valid user ID."))
      }

      // Check if email confirmation is required by Supabase project
      val emailConfirmedAt = userObj.optString("email_confirmed_at", "")
      val confirmationSentAt = userObj.optString("confirmation_sent_at", "")
      if (accessToken.isBlank() && confirmationSentAt.isNotBlank() && emailConfirmedAt.isBlank()) {
        return@withContext Result.failure(
          IOException("A confirmation email has been sent to $email. Please verify your email before logging in.")
        )
      }

      val effectiveToken = if (accessToken.isNotBlank()) accessToken else supabaseAnonKey
      val session = SupabaseUserSession(
        userId = userId,
        email = email.trim(),
        accessToken = effectiveToken,
        refreshToken = refreshToken,
        username = "",
        displayName = displayName.trim(),
        isProfileCompleted = false
      )
      saveSession(context, session)
      Result.success(session)
    } catch (e: Throwable) {
      Log.e(TAG, "SignUp error", e)
      Result.failure(e)
    }
  }

  // --- Real 6-Digit Email OTP Delivery ---
  suspend fun sendEmailOtp(email: String): Result<Unit> = withContext(Dispatchers.IO) {
    if (!isConfigured()) {
      return@withContext Result.failure(
        IllegalStateException("Supabase credentials not configured in .env or Secrets. Set SUPABASE_URL and SUPABASE_ANON_KEY to enable direct 6-digit email OTP.")
      )
    }

    try {
      val url = "$supabaseUrl/auth/v1/otp"
      val bodyJson = JSONObject().apply {
        put("email", email.trim())
        put("create_user", true)
      }

      val request = Request.Builder()
        .url(url)
        .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
        .build()

      val response = okHttpClient.newCall(request).execute()
      val responseBody = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        val errObj = try { JSONObject(responseBody) } catch (e: Exception) { null }
        val msg = errObj?.optString("error_description") ?: errObj?.optString("msg") ?: "Failed to send 6-digit OTP (HTTP ${response.code})"
        return@withContext Result.failure(IOException(msg))
      }

      Result.success(Unit)
    } catch (e: Throwable) {
      Log.e(TAG, "sendEmailOtp error", e)
      Result.failure(e)
    }
  }

  // --- Real 6-Digit Email OTP Verification ---
  suspend fun verifyEmailOtp(
    context: Context?,
    email: String,
    token: String
  ): Result<SupabaseUserSession> = withContext(Dispatchers.IO) {
    if (!isConfigured()) {
      return@withContext Result.failure(
        IllegalStateException("Supabase credentials not configured in .env or Secrets.")
      )
    }

    val cleanToken = token.trim()
    if (cleanToken.length != 6 || !cleanToken.all { it.isDigit() }) {
      return@withContext Result.failure(IllegalArgumentException("Verification code must be exactly 6 digits."))
    }

    try {
      val url = "$supabaseUrl/auth/v1/verify"
      val bodyJson = JSONObject().apply {
        put("type", "email")
        put("email", email.trim())
        put("token", cleanToken)
      }

      val request = Request.Builder()
        .url(url)
        .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
        .build()

      val response = okHttpClient.newCall(request).execute()
      val responseBody = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        val errObj = try { JSONObject(responseBody) } catch (e: Exception) { null }
        val msg = errObj?.optString("error_description") ?: errObj?.optString("msg") ?: "OTP verification failed (HTTP ${response.code})"
        val userFriendly = when {
          msg.contains("expired", ignoreCase = true) ->
            "This 6-digit code has expired. Please request a new verification code."
          msg.contains("invalid", ignoreCase = true) || msg.contains("token", ignoreCase = true) ->
            "Invalid 6-digit code. Please check your inbox and enter the exact code."
          else -> msg
        }
        return@withContext Result.failure(IOException(userFriendly))
      }

      val json = JSONObject(responseBody)
      val accessToken = json.optString("access_token", "")
      val refreshToken = json.optString("refresh_token", "")
      val userObj = json.optJSONObject("user") ?: json
      val userId = userObj.optString("id", "")

      if (userId.isBlank() || accessToken.isBlank()) {
        return@withContext Result.failure(IOException("Server did not return a valid session."))
      }

      val profileRes = fetchProfile(userId, accessToken)
      val existing = profileRes.getOrNull()
      val isCompleted = existing?.isProfileCompleted == true && !existing.username.isNullOrBlank()

      val session = SupabaseUserSession(
        userId = userId,
        email = email.trim(),
        accessToken = accessToken,
        refreshToken = refreshToken,
        username = existing?.username ?: "",
        displayName = existing?.displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@"),
        avatarUrl = existing?.avatarUrl ?: "",
        isProfileCompleted = isCompleted,
        dateOfBirth = existing?.dateOfBirth ?: "",
        educationalLevel = existing?.educationalLevel ?: "",
        courseOrDegree = existing?.courseOrDegree ?: "",
        fieldOfStudy = existing?.fieldOfStudy ?: "",
        bio = existing?.bio ?: ""
      )
      saveSession(context, session)
      Result.success(session)
    } catch (e: Throwable) {
      Log.e(TAG, "verifyEmailOtp error", e)
      Result.failure(e)
    }
  }

  suspend fun signInWithEmail(
    context: Context?,
    email: String,
    password: String
  ): Result<SupabaseUserSession> = withContext(Dispatchers.IO) {
    if (!isConfigured()) {
      return@withContext Result.failure(
        IllegalStateException("Supabase credentials not configured in .env or Secrets. Set SUPABASE_URL and SUPABASE_ANON_KEY to proceed.")
      )
    }

    try {
      val url = "$supabaseUrl/auth/v1/token?grant_type=password"
      val bodyJson = JSONObject().apply {
        put("email", email.trim())
        put("password", password)
      }

      val request = Request.Builder()
        .url(url)
        .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
        .build()

      val response = okHttpClient.newCall(request).execute()
      val responseBody = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        val errObj = try { JSONObject(responseBody) } catch (e: Exception) { null }
        val msg = errObj?.optString("error_description") ?: errObj?.optString("msg") ?: "Login failed (HTTP ${response.code})"
        val userFriendly = when {
          msg.contains("Invalid login credentials", ignoreCase = true) ->
            "Incorrect email or password. Please check your credentials."
          msg.contains("Email not confirmed", ignoreCase = true) ->
            "Your email address has not been confirmed yet. Please verify your email via the confirmation link sent to your inbox."
          else -> msg
        }
        return@withContext Result.failure(IOException(userFriendly))
      }

      val json = JSONObject(responseBody)
      val accessToken = json.getString("access_token")
      val refreshToken = json.optString("refresh_token", "")
      val userObj = json.getJSONObject("user")
      val userId = userObj.getString("id")
      val userMeta = userObj.optJSONObject("user_metadata")

      // Fetch profile from public.profiles table
      var dbUsername = userMeta?.optString("username", "") ?: ""
      var dbDisplayName = userMeta?.optString("display_name", "") ?: ""
      var dbAvatar = ""
      var isCompleted = false
      var dbDob = ""
      var dbEdu = ""
      var dbCourse = ""
      var dbField = ""
      var dbBio = ""

      val profileRes = fetchProfile(userId, accessToken)
      if (profileRes.isSuccess) {
        val p = profileRes.getOrNull()
        if (p != null) {
          if (p.username.isNotBlank()) dbUsername = p.username
          if (p.displayName.isNotBlank()) dbDisplayName = p.displayName
          dbAvatar = p.avatarUrl
          isCompleted = p.isProfileCompleted
          dbDob = p.dateOfBirth
          dbEdu = p.educationalLevel
          dbCourse = p.courseOrDegree
          dbField = p.fieldOfStudy
          dbBio = p.bio
        }
      }

      if (dbDisplayName.isBlank()) {
        dbDisplayName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
      }

      val session = SupabaseUserSession(
        userId = userId,
        email = email.trim(),
        accessToken = accessToken,
        refreshToken = refreshToken,
        username = dbUsername,
        displayName = dbDisplayName,
        avatarUrl = dbAvatar,
        isProfileCompleted = isCompleted,
        dateOfBirth = dbDob,
        educationalLevel = dbEdu,
        courseOrDegree = dbCourse,
        fieldOfStudy = dbField,
        bio = dbBio
      )
      saveSession(context, session)
      Result.success(session)
    } catch (e: Throwable) {
      Log.e(TAG, "SignIn error", e)
      Result.failure(e)
    }
  }

  suspend fun signInWithGoogleIdToken(
    context: Context?,
    idToken: String,
    googleEmail: String,
    googleDisplayName: String,
    googlePhotoUrl: String = ""
  ): Result<SupabaseUserSession> = withContext(Dispatchers.IO) {
    if (!isConfigured()) {
      return@withContext Result.failure(
        IllegalStateException("Supabase credentials not configured in .env or Secrets. Set SUPABASE_URL and SUPABASE_ANON_KEY to proceed.")
      )
    }

    try {
      val url = "$supabaseUrl/auth/v1/token?grant_type=id_token"
      val bodyJson = JSONObject().apply {
        put("provider", "google")
        put("id_token", idToken)
      }

      val request = Request.Builder()
        .url(url)
        .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
        .build()

      val response = okHttpClient.newCall(request).execute()
      val responseBody = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        val errObj = try { JSONObject(responseBody) } catch (e: Exception) { null }
        val msg = errObj?.optString("error_description") ?: errObj?.optString("msg") ?: "Google authentication failed (HTTP ${response.code})"
        return@withContext Result.failure(IOException(msg))
      }

      val json = JSONObject(responseBody)
      val accessToken = json.getString("access_token")
      val refreshToken = json.optString("refresh_token", "")
      val userObj = json.getJSONObject("user")
      val userId = userObj.getString("id")
      val email = userObj.optString("email", googleEmail)

      // Check profile completion status from Supabase database
      val profileRes = fetchProfile(userId, accessToken)
      val existingProfile = profileRes.getOrNull()

      val isCompleted = existingProfile?.isProfileCompleted == true && !existingProfile.username.isNullOrBlank()
      val session = SupabaseUserSession(
        userId = userId,
        email = email,
        accessToken = accessToken,
        refreshToken = refreshToken,
        username = existingProfile?.username ?: "",
        displayName = existingProfile?.displayName?.takeIf { it.isNotBlank() } ?: googleDisplayName,
        avatarUrl = existingProfile?.avatarUrl?.takeIf { it.isNotBlank() } ?: googlePhotoUrl,
        isProfileCompleted = isCompleted,
        dateOfBirth = existingProfile?.dateOfBirth ?: "",
        educationalLevel = existingProfile?.educationalLevel ?: "",
        courseOrDegree = existingProfile?.courseOrDegree ?: "",
        fieldOfStudy = existingProfile?.fieldOfStudy ?: "",
        bio = existingProfile?.bio ?: ""
      )
      saveSession(context, session)
      Result.success(session)
    } catch (e: Throwable) {
      Log.e(TAG, "Google auth error", e)
      Result.failure(e)
    }
  }

  suspend fun checkUsernameAvailability(
    username: String,
    excludeUserId: String? = null
  ): Result<Boolean> = withContext(Dispatchers.IO) {
    val clean = username.trim().lowercase()
    if (clean.length < 3) return@withContext Result.success(false)
    if (!clean.matches(Regex("^[a-zA-Z0-9_]{3,30}$"))) return@withContext Result.success(false)

    if (!isConfigured()) {
      return@withContext Result.success(true)
    }

    try {
      var url = "$supabaseUrl/rest/v1/profiles?username=eq.$clean&select=id"
      if (!excludeUserId.isNullOrBlank()) {
        url += "&id=neq.$excludeUserId"
      }

      val request = Request.Builder()
        .url(url)
        .header("Range", "0-0")
        .get()
        .build()

      val response = okHttpClient.newCall(request).execute()
      val body = response.body?.string() ?: "[]"
      if (!response.isSuccessful) {
        return@withContext Result.failure(IOException("Database check failed"))
      }

      val arr = JSONArray(body)
      val isAvailable = arr.length() == 0
      Result.success(isAvailable)
    } catch (e: Throwable) {
      Log.e(TAG, "Username check error", e)
      Result.failure(e)
    }
  }

  suspend fun fetchProfile(
    userId: String,
    tokenOverride: String? = null
  ): Result<UserProfile?> = withContext(Dispatchers.IO) {
    if (!isConfigured()) {
      return@withContext Result.success(null)
    }

    try {
      val url = "$supabaseUrl/rest/v1/profiles?id=eq.$userId&select=*"
      val requestBuilder = Request.Builder().url(url).get()
      if (tokenOverride != null) {
        requestBuilder.header("Authorization", "Bearer $tokenOverride")
      }
      val response = okHttpClient.newCall(requestBuilder.build()).execute()
      val body = response.body?.string() ?: "[]"

      if (!response.isSuccessful) {
        return@withContext Result.failure(IOException("Failed to fetch profile"))
      }

      val arr = JSONArray(body)
      if (arr.length() == 0) {
        return@withContext Result.success(null)
      }

      val obj = arr.getJSONObject(0)
      val userProfile = UserProfile(
        id = obj.getString("id"),
        username = obj.optString("username", ""),
        displayName = obj.optString("display_name", ""),
        bio = obj.optString("bio", ""),
        avatarUrl = obj.optString("avatar_url", ""),
        dateOfBirth = obj.optString("date_of_birth", ""),
        educationalLevel = obj.optString("educational_level", ""),
        courseOrDegree = obj.optString("course_or_degree", ""),
        fieldOfStudy = obj.optString("field_of_study", ""),
        isProfileCompleted = obj.optBoolean("is_profile_completed", false),
        isCurrentUser = true
      )
      Result.success(userProfile)
    } catch (e: Throwable) {
      Log.e(TAG, "fetchProfile error", e)
      Result.failure(e)
    }
  }

  suspend fun uploadAvatar(
    userId: String,
    imageBytes: ByteArray,
    mimeType: String = "image/jpeg"
  ): Result<String> = withContext(Dispatchers.IO) {
    if (!isConfigured()) {
      return@withContext Result.failure(IllegalStateException("Supabase credentials not configured in .env"))
    }

    try {
      val fileName = "avatar_${System.currentTimeMillis()}.jpg"
      val path = "$userId/$fileName"
      val url = "$supabaseUrl/storage/v1/object/avatars/$path"

      val requestBody = imageBytes.toRequestBody(mimeType.toMediaType())
      val request = Request.Builder()
        .url(url)
        .header("x-upsert", "true")
        .header("Content-Type", mimeType)
        .post(requestBody)
        .build()

      val response = okHttpClient.newCall(request).execute()
      val responseBody = response.body?.string() ?: ""

      if (!response.isSuccessful && response.code != 200 && response.code != 201) {
        val errObj = try { JSONObject(responseBody) } catch (e: Exception) { null }
        val msg = errObj?.optString("message") ?: errObj?.optString("error") ?: "Photo upload failed (${response.code})"
        return@withContext Result.failure(IOException(msg))
      }

      val publicUrl = "$supabaseUrl/storage/v1/object/public/avatars/$path"
      Result.success(publicUrl)
    } catch (e: Throwable) {
      Log.e(TAG, "uploadAvatar error", e)
      Result.failure(e)
    }
  }

  suspend fun uploadPdf(
    resourceId: String,
    fileName: String,
    pdfBytes: ByteArray
  ): Result<String> = withContext(Dispatchers.IO) {
    if (!isConfigured()) {
      return@withContext Result.failure(IllegalStateException("Supabase credentials not configured in .env"))
    }
    try {
      val cleanFileName = fileName.replace(" ", "_").ifBlank { "document.pdf" }
      val path = "$resourceId/$cleanFileName"
      val url = "$supabaseUrl/storage/v1/object/pdfs/$path"
      val requestBody = pdfBytes.toRequestBody("application/pdf".toMediaType())
      val request = Request.Builder()
        .url(url)
        .header("x-upsert", "true")
        .header("Content-Type", "application/pdf")
        .post(requestBody)
        .build()

      val response = okHttpClient.newCall(request).execute()
      val responseBody = response.body?.string() ?: ""

      if (!response.isSuccessful && response.code != 200 && response.code != 201) {
        val errObj = try { JSONObject(responseBody) } catch (e: Exception) { null }
        val msg = errObj?.optString("message") ?: errObj?.optString("error") ?: "PDF upload failed (${response.code})"
        return@withContext Result.failure(IOException(msg))
      }

      val publicUrl = "$supabaseUrl/storage/v1/object/public/pdfs/$path"
      Result.success(publicUrl)
    } catch (e: Throwable) {
      Log.e(TAG, "uploadPdf error", e)
      Result.failure(e)
    }
  }

  suspend fun uploadCover(
    resourceId: String,
    imageBytes: ByteArray,
    mimeType: String = "image/jpeg"
  ): Result<String> = withContext(Dispatchers.IO) {
    if (!isConfigured()) {
      return@withContext Result.failure(IllegalStateException("Supabase credentials not configured in .env"))
    }
    try {
      val fileName = "cover_${System.currentTimeMillis()}.jpg"
      val path = "$resourceId/$fileName"
      val url = "$supabaseUrl/storage/v1/object/covers/$path"
      val requestBody = imageBytes.toRequestBody(mimeType.toMediaType())
      val request = Request.Builder()
        .url(url)
        .header("x-upsert", "true")
        .header("Content-Type", mimeType)
        .post(requestBody)
        .build()

      val response = okHttpClient.newCall(request).execute()
      val responseBody = response.body?.string() ?: ""

      if (!response.isSuccessful && response.code != 200 && response.code != 201) {
        val errObj = try { JSONObject(responseBody) } catch (e: Exception) { null }
        val msg = errObj?.optString("message") ?: errObj?.optString("error") ?: "Cover upload failed (${response.code})"
        return@withContext Result.failure(IOException(msg))
      }

      val publicUrl = "$supabaseUrl/storage/v1/object/public/covers/$path"
      Result.success(publicUrl)
    } catch (e: Throwable) {
      Log.e(TAG, "uploadCover error", e)
      Result.failure(e)
    }
  }

  suspend fun updateProfileInDb(
    context: Context?,
    userId: String,
    username: String,
    displayName: String,
    avatarUrl: String,
    dateOfBirth: String,
    educationalLevel: String,
    courseOrDegree: String,
    fieldOfStudy: String,
    bio: String = "",
    isCompleted: Boolean = true
  ): Result<UserProfile> = withContext(Dispatchers.IO) {
    if (!isConfigured()) {
      return@withContext Result.failure(IllegalStateException("Supabase credentials not configured in .env"))
    }

    try {
      val url = "$supabaseUrl/rest/v1/profiles"
      val payload = JSONObject().apply {
        put("id", userId)
        put("username", username.trim().lowercase())
        put("display_name", displayName.trim())
        if (avatarUrl.isNotBlank()) put("avatar_url", avatarUrl)
        if (dateOfBirth.isNotBlank()) put("date_of_birth", dateOfBirth)
        put("educational_level", educationalLevel.trim())
        put("course_or_degree", courseOrDegree.trim())
        put("field_of_study", fieldOfStudy.trim())
        put("bio", bio.trim())
        put("is_profile_completed", isCompleted)
      }

      val request = Request.Builder()
        .url(url)
        .header("Prefer", "resolution=merge-duplicates,return=representation")
        .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
        .build()

      val response = okHttpClient.newCall(request).execute()
      val responseBody = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        val errObj = try { JSONObject(responseBody) } catch (e: Exception) { null }
        val msg = errObj?.optString("message") ?: errObj?.optString("details") ?: "Profile save failed (${response.code})"
        return@withContext Result.failure(IOException(msg))
      }

      val updatedSession = _currentSession.value?.copy(
        username = username.trim().lowercase(),
        displayName = displayName.trim(),
        avatarUrl = avatarUrl,
        dateOfBirth = dateOfBirth,
        educationalLevel = educationalLevel.trim(),
        courseOrDegree = courseOrDegree.trim(),
        fieldOfStudy = fieldOfStudy.trim(),
        bio = bio.trim(),
        isProfileCompleted = isCompleted
      )
      saveSession(context, updatedSession)

      val profile = UserProfile(
        id = userId,
        username = username.trim().lowercase(),
        displayName = displayName.trim(),
        avatarUrl = avatarUrl,
        dateOfBirth = dateOfBirth,
        educationalLevel = educationalLevel.trim(),
        courseOrDegree = courseOrDegree.trim(),
        fieldOfStudy = fieldOfStudy.trim(),
        bio = bio.trim(),
        isProfileCompleted = isCompleted,
        isCurrentUser = true
      )
      Result.success(profile)
    } catch (e: Throwable) {
      Log.e(TAG, "updateProfileInDb error", e)
      Result.failure(e)
    }
  }

  suspend fun signOut(context: Context?): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val token = _currentSession.value?.accessToken
      if (isConfigured() && !token.isNullOrBlank()) {
        val url = "$supabaseUrl/auth/v1/logout"
        val request = Request.Builder().url(url).post("{}".toRequestBody(JSON_MEDIA_TYPE)).build()
        okHttpClient.newCall(request).execute()
      }
    } catch (e: Throwable) {
      Log.w(TAG, "SignOut network call non-fatal", e)
    } finally {
      saveSession(context, null)
    }
    Result.success(Unit)
  }

  suspend fun resetPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
    if (!isConfigured()) {
      return@withContext Result.failure(
        IllegalStateException("Supabase credentials not configured in .env or Secrets. Set SUPABASE_URL and SUPABASE_ANON_KEY to proceed.")
      )
    }
    try {
      val url = "$supabaseUrl/auth/v1/recover"
      val bodyJson = JSONObject().apply { put("email", email.trim()) }
      val request = Request.Builder().url(url).post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE)).build()
      val response = okHttpClient.newCall(request).execute()
      val responseBody = response.body?.string() ?: ""

      if (response.isSuccessful) {
        Result.success(Unit)
      } else {
        val errObj = try { JSONObject(responseBody) } catch (e: Exception) { null }
        val msg = errObj?.optString("msg") ?: errObj?.optString("error_description") ?: "Password reset failed"
        Result.failure(IOException(msg))
      }
    } catch (e: Throwable) {
      Result.failure(e)
    }
  }

  suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
    if (!isConfigured()) {
      return@withContext Result.success(false)
    }
    try {
      val url = "$supabaseUrl/rest/v1/profiles?select=count&limit=1"
      val request = Request.Builder()
        .url(url)
        .header("Range", "0-0")
        .get()
        .build()

      val response = okHttpClient.newCall(request).execute()
      Result.success(response.isSuccessful)
    } catch (e: Throwable) {
      Result.failure(e)
    }
  }
}
