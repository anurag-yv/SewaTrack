package com.smartcity.citizen.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartcity.citizen.data.local.entity.CampusBinEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Campus Waste Bins
 * Fulfills CSE226 Unit IV: Local Data Persistence & Unit V: Integration with Location Services
 */
@Dao
interface CampusBinDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bins: List<CampusBinEntity>)

    @Query("SELECT * FROM campus_bins")
    fun getAllBinsFlow(): Flow<List<CampusBinEntity>>

    @Query("SELECT * FROM campus_bins WHERE id = :id LIMIT 1")
    suspend fun getBinById(id: String): CampusBinEntity?

    @Query("SELECT COUNT(*) FROM campus_bins")
    suspend fun getBinCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBin(bin: CampusBinEntity)

    @Query("UPDATE campus_bins SET status = :status WHERE id = :id")
    suspend fun updateBinStatus(id: String, status: com.smartcity.citizen.model.BinStatus)
}
