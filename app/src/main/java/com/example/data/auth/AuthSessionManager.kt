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
            // Default initial guest session for zero-barrier instant exploration
            val defaultGuest = UserProfile(
                id = "guest_nova_user",
                displayName = "Cyber Explorer",
                email = "explorer@novamusic.space",
                photoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&q=80",
                provider = "guest",
                token = "guest_token_session",
                isPremium = true,
                joinedDate = getFormattedCurrentDate(),
                bio = "Holographic audio deck pilot."
            )
            _currentUser.value = defaultGuest
            _isLoggedIn.value = false
        }
    }

    /**
     * Signs in or signs up with Google, updating SharedPreferences globally.
     */
    fun signInWithGoogle(
        accountName: String,
        accountEmail: String,
        photoUrl: String? = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&q=80"
    ) {
        val userId = "google_" + UUID.nameUUIDFromBytes(accountEmail.toByteArray()).toString().take(12)
        val token = "g_token_" + UUID.randomUUID().toString().take(16)
        val joinDate = getFormattedCurrentDate()

        prefs.edit().apply {
            putBoolean(KEY_LOGGED_IN, true)
            putString(KEY_USER_ID, userId)
            putString(KEY_DISPLAY_NAME, accountName)
            putString(KEY_EMAIL, accountEmail)
            putString(KEY_PHOTO_URL, photoUrl)
            putString(KEY_AUTH_PROVIDER, "google.com")
            putString(KEY_AUTH_TOKEN, token)
            putBoolean(KEY_IS_PREMIUM, true)
            putString(KEY_JOINED_DATE, joinDate)
            putString(KEY_BIO, "Signed in via Google. Holographic Audio Master VIP.")
            apply()
        }

        val updated = UserProfile(
            id = userId,
            displayName = accountName,
            email = accountEmail,
            photoUrl = photoUrl,
            provider = "google.com",
            token = token,
            isPremium = true,
            joinedDate = joinDate,
            bio = "Signed in via Google. Holographic Audio Master VIP."
        )
        _currentUser.value = updated
        _isLoggedIn.value = true
    }

    /**
     * Signs up with custom email & password, saving credentials into SharedPreferences.
     */
    fun signUpWithEmail(name: String, email: String, bio: String = "") {
        val userId = "user_" + UUID.randomUUID().toString().take(10)
        val token = "jwt_" + UUID.randomUUID().toString().take(16)
        val joinDate = getFormattedCurrentDate()
        val defaultAvatar = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=200&q=80"

        prefs.edit().apply {
            putBoolean(KEY_LOGGED_IN, true)
            putString(KEY_USER_ID, userId)
            putString(KEY_DISPLAY_NAME, name)
            putString(KEY_EMAIL, email)
            putString(KEY_PHOTO_URL, defaultAvatar)
            putString(KEY_AUTH_PROVIDER, "email")
            putString(KEY_AUTH_TOKEN, token)
            putBoolean(KEY_IS_PREMIUM, true)
            putString(KEY_JOINED_DATE, joinDate)
            putString(KEY_BIO, if (bio.isNotEmpty()) bio else "Nova Sound Explorer.")
            apply()
        }

        val updated = UserProfile(
            id = userId,
            displayName = name,
            email = email,
            photoUrl = defaultAvatar,
            provider = "email",
            token = token,
            isPremium = true,
            joinedDate = joinDate,
            bio = if (bio.isNotEmpty()) bio else "Nova Sound Explorer."
        )
        _currentUser.value = updated
        _isLoggedIn.value = true
    }

    /**
     * Signs in with existing email credentials.
     */
    fun signInWithEmail(email: String) {
        val name = email.substringBefore("@").replace(".", " ").capitalize(Locale.ROOT)
        signUpWithEmail(name, email)
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
     * Toggles VIP/Hi-Res Premium audio membership status.
     */
    fun togglePremium(isPremium: Boolean) {
        val current = _currentUser.value ?: return
        val updated = current.copy(isPremium = isPremium)
        prefs.edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply()
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
        _currentUser.value = UserProfile(
            id = "guest_nova_user",
            displayName = "Guest Pilot",
            email = "guest@novamusic.space",
            photoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&q=80",
            provider = "guest",
            isPremium = false,
            joinedDate = getFormattedCurrentDate(),
            bio = "Tap to sign in with Google or create an account."
        )
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
