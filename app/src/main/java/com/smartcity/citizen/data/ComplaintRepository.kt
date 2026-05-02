package com.smartcity.citizen.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.smartcity.citizen.data.models.Complaint
import com.smartcity.citizen.data.models.IssueType
import com.smartcity.citizen.data.models.Status
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

class ComplaintRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val complaintsCollection = firestore.collection("complaints")
    private val TAG = "ComplaintRepository"

    // For admin only – shows ALL complaints
    fun getAllComplaints(): Flow<List<Complaint>> = callbackFlow {
        val listener = complaintsCollection
            .orderBy("dateTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching complaints", error)
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toComplaint() } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.distinctUntilChanged()

    // For employee – shows only complaints assigned to them
    fun getComplaintsForEmployee(employeeId: String): Flow<List<Complaint>> = callbackFlow {
        val listener = complaintsCollection
            .whereEqualTo("assignedTo", employeeId)
            .orderBy("dateTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching employee complaints", error)
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toComplaint() } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.distinctUntilChanged()

    // For citizen – shows ONLY complaints reported by this user
    fun getComplaintsForUser(userId: String): Flow<List<Complaint>> = callbackFlow {
        val listener = complaintsCollection
            .whereEqualTo("reportedBy", userId)
            .orderBy("dateTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching user complaints", error)
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toComplaint() } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.distinctUntilChanged()

    // Get a single complaint by its ID
    suspend fun getComplaintById(complaintId: String): Complaint? {
        return try {
            val doc = complaintsCollection.document(complaintId).get().await()
            doc.toComplaint()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching complaint by ID: $complaintId", e)
            null
        }
    }

    suspend fun addComplaint(complaint: Complaint): Boolean {
        return try {
            var complaintToSave = complaint
            if (complaint.id.isEmpty()) {
                val newId = complaintsCollection.document().id
                complaintToSave = complaint.copy(id = newId)
            }
            complaintsCollection.document(complaintToSave.id)
                .set(complaintToSave.toMap())
                .await()
            Log.d(TAG, "Complaint added: ${complaintToSave.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding complaint", e)
            false
        }
    }

    suspend fun updateStatus(complaintId: String, newStatus: Status): Boolean {
        return try {
            complaintsCollection.document(complaintId)
                .update("status", newStatus.name)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating status", e)
            false
        }
    }

    suspend fun updateRating(complaintId: String, rating: Int): Boolean {
        return try {
            complaintsCollection.document(complaintId)
                .update("rating", rating)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating rating", e)
            false
        }
    }

    suspend fun assignEmployee(complaintId: String, employeeUid: String): Boolean {
        return try {
            complaintsCollection.document(complaintId)
                .update("assignedTo", employeeUid)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error assigning employee", e)
            false
        }
    }

    suspend fun addResolutionNote(complaintId: String, note: String): Boolean {
        return try {
            complaintsCollection.document(complaintId)
                .update("resolutionNote", note)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding resolution note", e)
            false
        }
    }

    suspend fun deleteComplaint(complaintId: String): Boolean {
        return try {
            complaintsCollection.document(complaintId).delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting complaint", e)
            false
        }
    }
    suspend fun deleteEmployee(employeeId: String): Boolean {
        return try {
            firestore.collection("users").document(employeeId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
    // ---------- Helper Extensions ----------

    private fun com.google.firebase.firestore.DocumentSnapshot.toComplaint(): Complaint? {
        return try {
            val id = this.id
            val imageUri = getString("imageUri") ?: ""
            val issueTypeName = getString("issueType") ?: ""
            val issueType = try {
                IssueType.valueOf(issueTypeName)
            } catch (e: Exception) {
                IssueType.OTHER
            }
            val description = getString("description") ?: ""
            val location = getString("location") ?: ""
            val dateTime = getLong("dateTime") ?: System.currentTimeMillis()
            val statusName = getString("status") ?: ""
            val status = try {
                Status.valueOf(statusName)
            } catch (e: Exception) {
                Status.SUBMITTED
            }
            val assignedTo = getString("assignedTo")
            val resolutionNote = getString("resolutionNote")
            val reportedBy = getString("reportedBy") ?: ""

            // Firestore returns Double; convert to Int
            val estimatedCost = when (val cost = getDouble("estimatedCost")) {
                null -> 0
                else -> cost.toInt()
            }

            val rating = when (val r = getLong("rating")) {
                null -> 0
                else -> r.toInt()
            }

            Complaint(
                id = id,
                imageUri = imageUri,
                issueType = issueType,
                description = description,
                location = location,
                dateTime = dateTime,
                status = status,
                assignedTo = assignedTo,
                resolutionNote = resolutionNote,
                reportedBy = reportedBy,
                estimatedCost = estimatedCost,
                rating = rating
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting document to Complaint", e)
            null
        }
    }

    private fun Complaint.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "imageUri" to imageUri,
        "issueType" to issueType.name,
        "description" to description,
        "location" to location,
        "dateTime" to dateTime,
        "status" to status.name,
        "assignedTo" to assignedTo,
        "resolutionNote" to resolutionNote,
        "reportedBy" to reportedBy,
        "estimatedCost" to estimatedCost.toDouble(),
        "rating" to rating
    )
}