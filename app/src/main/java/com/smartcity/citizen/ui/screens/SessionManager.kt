package com.smartcity.citizen.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user session data by saving and retrieving information from SharedPreferences.
 * It acts as a single source of truth for user-related data within the app.
 */
class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_ROLE = "user_role"
    }

    /**
     * Saves the user's unique ID.
     * @param userId The unique identifier for the logged-in user.
     */
    fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    /**
     * Retrieves the saved user ID.
     * @return The user ID if it exists, otherwise null.
     */
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    /**
     * Saves the user's role (e.g., "citizen", "employee", "admin").
     * @param role The role of the logged-in user.
     */
    fun saveUserRole(role: String) {
        prefs.edit().putString(KEY_USER_ROLE, role).apply()
    }

    /**
     * Retrieves the saved user role.
     * @return The user's role if it exists, otherwise a default value of "citizen".
     */
    fun getUserRole(): String {
        return prefs.getString(KEY_USER_ROLE, "citizen") ?: "citizen"
    }

    /**
     * Clears all user session data, effectively logging the user out.
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}