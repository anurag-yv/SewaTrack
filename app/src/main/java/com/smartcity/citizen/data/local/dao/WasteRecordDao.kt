package com.smartcity.citizen.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartcity.citizen.data.local.entity.WasteRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Waste Records (CRUD + Flow)
 * Fulfills CSE226 Unit IV: DAOs, Room with Flow, CRUD Operations
 */
@Dao
interface WasteRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: WasteRecordEntity): Long

    @Update
    suspend fun updateRecord(record: WasteRecordEntity): Int

    @Delete
    suspend fun deleteRecord(record: WasteRecordEntity): Int

    @Query("SELECT * FROM waste_records ORDER BY timestamp DESC")
    fun getAllRecordsFlow(): Flow<List<WasteRecordEntity>>

    @Query("SELECT * FROM waste_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: String): WasteRecordEntity?

    @Query("SELECT * FROM waste_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentRecordsFlow(limit: Int = 5): Flow<List<WasteRecordEntity>>

    @Query("SELECT COUNT(*) FROM waste_records")
    fun getRecordCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM waste_records WHERE confirmedCategory = :category")
    fun getCountForCategory(category: String): Flow<Int>

    @Query("DELETE FROM waste_records")
    suspend fun deleteAll()
}
