package com.smartcity.citizen.data.models

import java.util.UUID

data class Complaint(
    val id: String = UUID.randomUUID().toString(),
    val imageUri: String = "",
    val issueType: IssueType = IssueType.OTHER,
    val description: String = "",
    val location: String = "",
    val dateTime: Long = System.currentTimeMillis(),
    val status: Status = Status.SUBMITTED,
    val assignedTo: String? = null,
    val resolutionNote: String? = null,
    val reportedBy: String = "",             // UID of citizen who reported
    val estimatedCost: Int = 0,         // admin sets budget
    val rating: Int = 0                  // user rating (0-5) - 0 means not rated yet
)

enum class IssueType {
    POTHOLE, GARBAGE, STREETLIGHT, WATER_LEAKAGE, OTHER
}

enum class Status {
    SUBMITTED, IN_PROGRESS, RESOLVED
}