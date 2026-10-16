package com.smartcity.citizen.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartcity.citizen.model.WasteCategory
import java.util.UUID

@Entity(tableName = "waste_records")
data class WasteRecordEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val imageUri: String = "",
    val predictedCategory: WasteCategory,
    val confirmedCategory: WasteCategory,
    val confidence: Float,
    val isUncertain: Boolean,
    val detectedLabel: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
