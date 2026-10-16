package com.smartcity.citizen.data.local

import androidx.room.TypeConverter
import com.smartcity.citizen.model.BinStatus
import com.smartcity.citizen.model.WasteCategory
import java.util.Date

/**
 * Room Type Converters for Enums, Dates, and Collections
 * Fulfills CSE226 Unit IV: Local Data Persistence Type Converters
 */
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromWasteCategory(category: WasteCategory?): String? = category?.name

    @TypeConverter
    fun toWasteCategory(value: String?): WasteCategory? =
        value?.let { runCatching { WasteCategory.valueOf(it) }.getOrDefault(WasteCategory.RECYCLABLE_DRY) }

    @TypeConverter
    fun fromBinStatus(status: BinStatus?): String? = status?.name

    @TypeConverter
    fun toBinStatus(value: String?): BinStatus? =
        value?.let { runCatching { BinStatus.valueOf(it) }.getOrDefault(BinStatus.OPERATIONAL) }
}
