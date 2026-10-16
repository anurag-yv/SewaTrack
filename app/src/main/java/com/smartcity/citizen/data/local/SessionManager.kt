package com.smartcity.citizen.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.smartcity.citizen.model.UserRole
import com.smartcity.citizen.model.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

private val Context.sessionDataStore by preferencesDataStore(name = "sortit_user_session")

/**
 * Offline-First Session & RBAC Role Manager
 * Fulfills CSE226 Unit IV: DataStore Preferences & Unit III: State Management
 */
class SessionManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val KEY_USER_ID = stringPreferencesKey("session_user_id")
    private val KEY_NAME = stringPreferencesKey("session_name")
    private val KEY_EMAIL = stringPreferencesKey("session_email")
    private val KEY_ROLE = stringPreferencesKey("session_role")
    private val KEY_CAMPUS_ID = stringPreferencesKey("session_campus_id")
    private val KEY_DEPT = stringPreferencesKey("session_dept")

    // Default seeded session
    private val defaultStudent = UserSession(
        userId = "stu_001",
        name = "Aarav Sharma",
        email = "aarav.student@campus.edu",
        role = UserRole.STUDENT,
        campusId = "CU-2026-CS88",
        department = "School of Computer Science & Engineering",
        ecoScore = 480,
        binsClearedCount = 0
    )

    private val _currentSession = MutableStateFlow<UserSession>(defaultStudent)
    val currentSession: StateFlow<UserSession> = _currentSession.asStateFlow()

    init {
        scope.launch {
            loadSessionFromDataStore()
        }
    }

    private suspend fun loadSessionFromDataStore() {
        val prefs = context.sessionDataStore.data.first()
        val email = prefs[KEY_EMAIL]
        if (email != null) {
            val roleStr = prefs[KEY_ROLE]
            _currentSession.value = UserSession(
                userId = prefs[KEY_USER_ID] ?: "user_default",
                name = prefs[KEY_NAME] ?: "Campus Citizen",
                email = email,
                role = UserRole.fromKey(roleStr),
                campusId = prefs[KEY_CAMPUS_ID] ?: "CU-2026",
                department = prefs[KEY_DEPT] ?: "Campus Sustainability"
            )
        }
    }

    fun login(email: String, role: UserRole, name: String = "", campusId: String = "") {
        val resolvedName = if (name.isNotBlank()) name else when (role) {
            UserRole.STUDENT -> "Aarav Sharma"
            UserRole.WORKER -> "Ramesh Kumar (Facility Staff)"
            UserRole.ADMIN -> "Dr. Priya Sen (Sustainability Lead)"
        }
        val resolvedCampusId = if (campusId.isNotBlank()) campusId else when (role) {
            UserRole.STUDENT -> "CU-2026-CS88"
            UserRole.WORKER -> "CU-STAFF-W42"
            UserRole.ADMIN -> "CU-ADMIN-001"
        }
        val session = UserSession(
            userId = UUID.randomUUID().toString(),
            name = resolvedName,
            email = email,
            role = role,
            campusId = resolvedCampusId,
            department = when (role) {
                UserRole.STUDENT -> "School of Computer Science & Engineering"
                UserRole.WORKER -> "Campus Green Operations & Waste Facility"
                UserRole.ADMIN -> "Office of Campus Sustainability & Compliance"
            }
        )
        _currentSession.value = session
        persistSession(session)
    }

    fun register(name: String, email: String, role: UserRole, campusId: String, dept: String) {
        val session = UserSession(
            userId = UUID.randomUUID().toString(),
            name = name.trim(),
            email = email.trim(),
            role = role,
            campusId = campusId.trim(),
            department = dept.trim().ifBlank { "University Campus" }
        )
        _currentSession.value = session
        persistSession(session)
    }

    fun switchRole(role: UserRole) {
        val current = _currentSession.value
        val switched = current.copy(
            role = role,
            name = when (role) {
                UserRole.STUDENT -> "Aarav Sharma"
                UserRole.WORKER -> "Ramesh Kumar (Facility Staff)"
                UserRole.ADMIN -> "Dr. Priya Sen (Sustainability Lead)"
            },
            email = when (role) {
                UserRole.STUDENT -> "aarav.student@campus.edu"
                UserRole.WORKER -> "ramesh.worker@campus.edu"
                UserRole.ADMIN -> "priya.admin@campus.edu"
            },
            campusId = when (role) {
                UserRole.STUDENT -> "CU-2026-CS88"
                UserRole.WORKER -> "CU-STAFF-W42"
                UserRole.ADMIN -> "CU-ADMIN-001"
            }
        )
        _currentSession.value = switched
        persistSession(switched)
    }

    fun logout() {
        _currentSession.value = defaultStudent
        scope.launch {
            context.sessionDataStore.edit { it.clear() }
        }
    }

    private fun persistSession(session: UserSession) {
        scope.launch {
            context.sessionDataStore.edit { prefs ->
                prefs[KEY_USER_ID] = session.userId
                prefs[KEY_NAME] = session.name
                prefs[KEY_EMAIL] = session.email
                prefs[KEY_ROLE] = session.role.roleKey
                prefs[KEY_CAMPUS_ID] = session.campusId
                prefs[KEY_DEPT] = session.department
            }
        }
    }

    companion object {
        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
