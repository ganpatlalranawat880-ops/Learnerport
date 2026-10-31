package com.example.data.security

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

object AppLockManager {
  private const val PREFS_NAME = "learnerport_security_prefs"
  private const val KEY_PIN_HASH = "security_pin_hash"
  private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
  private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
  private const val KEY_LOCKOUT_UNTIL = "lockout_until"

  private val _isUnlockedThisSession = MutableStateFlow(false)
  val isUnlockedThisSession: StateFlow<Boolean> = _isUnlockedThisSession.asStateFlow()

  fun isAppLockEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_APP_LOCK_ENABLED, false) && prefs.getString(KEY_PIN_HASH, null) != null
  }

  fun isPinConfigured(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_PIN_HASH, null) != null
  }

  fun setPin(context: Context, pin: String): Boolean {
    if (pin.length != 4 || !pin.all { it.isDigit() }) return false
    val hash = hashPin(pin)
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
      .putString(KEY_PIN_HASH, hash)
      .putBoolean(KEY_APP_LOCK_ENABLED, true)
      .putInt(KEY_FAILED_ATTEMPTS, 0)
      .putLong(KEY_LOCKOUT_UNTIL, 0L)
      .apply()
    _isUnlockedThisSession.value = true
    return true
  }

  fun verifyPin(context: Context, pin: String): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false

    val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
    if (System.currentTimeMillis() < lockoutUntil) {
      return false
    }

    val incomingHash = hashPin(pin)
    val isMatch = storedHash == incomingHash

    if (isMatch) {
      prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).putLong(KEY_LOCKOUT_UNTIL, 0L).apply()
      _isUnlockedThisSession.value = true
      return true
    } else {
      val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
      val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts)
      if (attempts >= 5) {
        editor.putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + 30_000L) // 30 sec lockout
      }
      editor.apply()
      return false
    }
  }

  fun getLockoutSecondsRemaining(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
    val diff = lockoutUntil - System.currentTimeMillis()
    return if (diff > 0) (diff / 1000).toInt() else 0
  }

  fun disableLock(context: Context, currentPin: String): Boolean {
    if (!verifyPin(context, currentPin)) return false
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
      .putBoolean(KEY_APP_LOCK_ENABLED, false)
      .remove(KEY_PIN_HASH)
      .apply()
    _isUnlockedThisSession.value = true
    return true
  }

  fun resetLock(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
      .remove(KEY_PIN_HASH)
      .putBoolean(KEY_APP_LOCK_ENABLED, false)
      .putInt(KEY_FAILED_ATTEMPTS, 0)
      .putLong(KEY_LOCKOUT_UNTIL, 0L)
      .apply()
    _isUnlockedThisSession.value = true
  }

  fun lockApp() {
    _isUnlockedThisSession.value = false
  }

  fun markUnlocked() {
    _isUnlockedThisSession.value = true
  }

  private fun hashPin(pin: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val salt = "LearnerportSecuritySalt_2026"
    val digest = md.digest((salt + pin).toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }
}
