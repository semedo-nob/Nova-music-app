package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Global User Profile model representing authenticated user credentials
 * persisted in Android SharedPreferences.
 */
data class UserProfile(
    val id: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val provider: String = "google.com", // "google.com", "email", or "guest"
    val token: String? = null,
    val isPremium: Boolean = true,
    val joinedDate: String = "",
    val bio: String = "Cyber-audio enthusiast & sound explorer."
)

/**
 * Manages persistent user credentials and authentication state across the entire application
 * using Android SharedPreferences.
 */
class AuthSessionManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        loadSession()
    }

    /**
     * Loads existing user session from SharedPreferences on application startup.
     */
    private fun loadSession() {
        val loggedIn = prefs.getBoolean(KEY_LOGGED_IN, false)
        if (loggedIn) {
            val user = UserProfile(
                id = prefs.getString(KEY_USER_ID, UUID.randomUUID().toString()) ?: UUID.randomUUID().toString(),
                displayName = prefs.getString(KEY_DISPLAY_NAME, "Nova Pilot") ?: "Nova Pilot",
                email = prefs.getString(KEY_EMAIL, "pilot@novamusic.space") ?: "pilot@novamusic.space",
                photoUrl = prefs.getString(KEY_PHOTO_URL, null),
                provider = prefs.getString(KEY_AUTH_PROVIDER, "google.com") ?: "google.com",
                token = prefs.getString(KEY_AUTH_TOKEN, null),
                isPremium = prefs.getBoolean(KEY_IS_PREMIUM, true),
                joinedDate = prefs.getString(KEY_JOINED_DATE, getFormattedCurrentDate()) ?: getFormattedCurrentDate(),
                bio = prefs.getString(KEY_BIO, "Exploring soundwaves through the holographic deck.") ?: ""
            )
            _currentUser.value = user
            _isLoggedIn.value = true
        } else {
            _currentUser.value = null
            _isLoggedIn.value = false
        }
    }

    fun createProfile(displayName: String, photoUrl: String?) {
        val userId = "user_" + UUID.randomUUID().toString().take(10)
        val joinDate = getFormattedCurrentDate()
        prefs.edit().apply {
            putBoolean(KEY_LOGGED_IN, true)
            putString(KEY_USER_ID, userId)
            putString(KEY_DISPLAY_NAME, displayName.trim())
            putString(KEY_EMAIL, "")
            putString(KEY_PHOTO_URL, photoUrl)
            putString(KEY_AUTH_PROVIDER, "local")
            putBoolean(KEY_IS_PREMIUM, false)
            putString(KEY_JOINED_DATE, joinDate)
            putString(KEY_BIO, "")
            apply()
        }
        _currentUser.value = UserProfile(
            id = userId,
            displayName = displayName.trim(),
            email = "",
            photoUrl = photoUrl,
            provider = "local",
            isPremium = false,
            joinedDate = joinDate,
            bio = ""
        )
        _isLoggedIn.value = true
    }

    /**
     * Updates editable user profile properties in SharedPreferences.
     */
    fun updateProfile(displayName: String, bio: String, photoUrl: String? = null) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            displayName = displayName,
            bio = bio,
            photoUrl = photoUrl ?: current.photoUrl
        )

        prefs.edit().apply {
            putString(KEY_DISPLAY_NAME, updated.displayName)
            putString(KEY_BIO, updated.bio)
            if (updated.photoUrl != null) {
                putString(KEY_PHOTO_URL, updated.photoUrl)
            }
            apply()
        }
        _currentUser.value = updated
    }

    /**
     * Signs out user and clears credentials from SharedPreferences.
     */
    fun signOut() {
        prefs.edit().apply {
            putBoolean(KEY_LOGGED_IN, false)
            remove(KEY_USER_ID)
            remove(KEY_DISPLAY_NAME)
            remove(KEY_EMAIL)
            remove(KEY_PHOTO_URL)
            remove(KEY_AUTH_PROVIDER)
            remove(KEY_AUTH_TOKEN)
            apply()
        }

        _isLoggedIn.value = false
        _currentUser.value = null
    }

    private fun getFormattedCurrentDate(): String {
        return SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())
    }

    companion object {
        private const val PREFS_NAME = "nova_music_user_session"
        private const val KEY_LOGGED_IN = "auth_is_logged_in"
        private const val KEY_USER_ID = "auth_user_id"
        private const val KEY_DISPLAY_NAME = "auth_display_name"
        private const val KEY_EMAIL = "auth_email"
        private const val KEY_PHOTO_URL = "auth_photo_url"
        private const val KEY_AUTH_PROVIDER = "auth_provider"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_IS_PREMIUM = "auth_is_premium"
        private const val KEY_JOINED_DATE = "auth_joined_date"
        private const val KEY_BIO = "auth_bio"
    }
}
