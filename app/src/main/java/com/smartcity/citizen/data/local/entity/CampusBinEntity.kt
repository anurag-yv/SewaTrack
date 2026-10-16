package com.smartcity.citizen.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartcity.citizen.model.BinStatus

@Entity(tableName = "campus_bins")
data class CampusBinEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val locationDescription: String,
    val latitude: Double,
    val longitude: Double,
    val acceptedCategoriesCsv: String, // comma-separated WasteCategory names
    val status: BinStatus = BinStatus.OPERATIONAL,
    val buildingCode: String = "Campus Main"
)
