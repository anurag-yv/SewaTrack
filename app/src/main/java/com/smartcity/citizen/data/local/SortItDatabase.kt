package com.smartcity.citizen.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.smartcity.citizen.data.local.dao.CampusBinDao
import com.smartcity.citizen.data.local.dao.WasteRecordDao
import com.smartcity.citizen.data.local.entity.CampusBinEntity
import com.smartcity.citizen.data.local.entity.WasteRecordEntity

/**
 * SortIt Local Room Database (Offline-First Single Source of Truth)
 * Fulfills CSE226 Unit IV: Room Database Architecture & Offline Data Persistence
 */
@Database(
    entities = [
        WasteRecordEntity::class,
        CampusBinEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SortItDatabase : RoomDatabase() {

    abstract fun wasteRecordDao(): WasteRecordDao
    abstract fun campusBinDao(): CampusBinDao

    companion object {
        @Volatile
        private var INSTANCE: SortItDatabase? = null

        fun getDatabase(context: Context): SortItDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SortItDatabase::class.java,
                    "sortit_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
