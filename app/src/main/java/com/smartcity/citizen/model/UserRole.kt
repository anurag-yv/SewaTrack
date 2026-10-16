package com.smartcity.citizen.model

/**
 * Role-Based Access Control (RBAC) Definition for SortIt
 * Fulfills CSE226: User Roles, Separation of Concerns & State Management
 */
enum class UserRole(
    val roleKey: String,
    val displayName: String,
    val badgeTitle: String,
    val description: String
) {
    STUDENT(
        roleKey = "student",
        displayName = "Student",
        badgeTitle = "🎓 University Student",
        description = "AI waste scan, disposal guides, bin locator, and personal sorting history."
    ),
    WORKER(
        roleKey = "worker",
        displayName = "Green Staff",
        badgeTitle = "👷 Facility Waste Worker",
        description = "Clear overflowing bins, inspect collection zones, and log bin status updates."
    ),
    ADMIN(
        roleKey = "admin",
        displayName = "Administrator",
        badgeTitle = "🛡️ Sustainability Admin",
        description = "Campus-wide diversion analytics, bin health monitoring, and staff coordination."
    );

    companion object {
        fun fromKey(key: String?): UserRole {
            return when (key?.lowercase()?.trim()) {
                "worker", "employee", "green staff" -> WORKER
                "admin", "administrator" -> ADMIN
                else -> STUDENT
            }
        }
    }
}

data class UserSession(
    val userId: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val campusId: String = "CU-2026",
    val department: String = "School of Computer Science & Engineering",
    val ecoScore: Int = 420,
    val binsClearedCount: Int = 18
)
