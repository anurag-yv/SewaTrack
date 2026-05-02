package com.smartcity.citizen.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun getCurrentUserId(): String? = auth.currentUser?.uid
    fun isLoggedIn(): Boolean = auth.currentUser != null

    suspend fun getUserRole(): String? {
        val uid = getCurrentUserId() ?: return null
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            doc.getString("role") ?: "citizen"
        } catch (e: Exception) { "citizen" }
    }
    suspend fun getUserName(): String? {
        val uid = getCurrentUserId() ?: return null
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            doc.getString("name")
        } catch (e: Exception) { null }
    }

    fun logout() {
        auth.signOut()
    }
}