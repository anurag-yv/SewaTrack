package com.smartcity.citizen.data.repository

import com.smartcity.citizen.data.local.dao.CampusBinDao
import com.smartcity.citizen.data.local.entity.CampusBinEntity
import com.smartcity.citizen.model.BinStatus
import com.smartcity.citizen.model.CampusBin
import com.smartcity.citizen.model.WasteCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface CampusBinRepository {
    val allBinsFlow: Flow<List<CampusBin>>
    suspend fun initializeSeedData()
    suspend fun getBinById(id: String): CampusBin?
    suspend fun updateBinStatus(id: String, status: BinStatus)
    suspend fun addBin(bin: CampusBin)
}

class CampusBinRepositoryImpl(
    private val dao: CampusBinDao
) : CampusBinRepository {

    override val allBinsFlow: Flow<List<CampusBin>> = dao.getAllBinsFlow().map { entities ->
        if (entities.isEmpty()) {
            defaultCampusBins
        } else {
            entities.map { it.toCampusBin() }
        }
    }

    override suspend fun initializeSeedData() {
        if (dao.getBinCount() == 0) {
            dao.insertAll(defaultCampusBins.map { it.toEntity() })
        }
    }

    override suspend fun getBinById(id: String): CampusBin? {
        val entity = dao.getBinById(id)
        return entity?.toCampusBin() ?: defaultCampusBins.find { it.id == id }
    }

    override suspend fun updateBinStatus(id: String, status: BinStatus) {
        initializeSeedData()
        dao.updateBinStatus(id, status)
    }

    override suspend fun addBin(bin: CampusBin) {
        initializeSeedData()
        dao.insertBin(bin.toEntity())
    }

    private fun CampusBinEntity.toCampusBin(): CampusBin {
        val categories = acceptedCategoriesCsv.split(",")
            .mapNotNull { name ->
                runCatching { WasteCategory.valueOf(name.trim()) }.getOrNull()
            }
        return CampusBin(
            id = id,
            name = name,
            locationDescription = locationDescription,
            latitude = latitude,
            longitude = longitude,
            acceptedCategories = if (categories.isEmpty()) listOf(WasteCategory.RECYCLABLE_DRY) else categories,
            status = status,
            buildingCode = buildingCode
        )
    }

    private fun CampusBin.toEntity(): CampusBinEntity {
        return CampusBinEntity(
            id = id,
            name = name,
            locationDescription = locationDescription,
            latitude = latitude,
            longitude = longitude,
            acceptedCategoriesCsv = acceptedCategories.joinToString(",") { it.name },
            status = status,
            buildingCode = buildingCode
        )
    }

    companion object {
        // Realistically modeled Lovely Professional University (LPU, Phagwara, Punjab) campus waste stations
        val defaultCampusBins = listOf(
            CampusBin(
                id = "bin_lpu_block34",
                name = "Block 34 CSE & Tech Hub EcoStation",
                locationDescription = "Ground Floor Central Atrium, near Coding Labs & Lift Lobby",
                latitude = 31.2536,
                longitude = 75.7037,
                acceptedCategories = listOf(
                    WasteCategory.PAPER_CARDBOARD,
                    WasteCategory.PLASTIC_METAL,
                    WasteCategory.E_WASTE,
                    WasteCategory.RECYCLABLE_DRY
                ),
                status = BinStatus.OPERATIONAL,
                buildingCode = "Block 34 (CSE)"
            ),
            CampusBin(
                id = "bin_lpu_unimall",
                name = "Central Library & Uni-Mall Station",
                locationDescription = "Level 1 Main Walkway, opposite Food Court entrance",
                latitude = 31.2548,
                longitude = 75.7025,
                acceptedCategories = listOf(
                    WasteCategory.PAPER_CARDBOARD,
                    WasteCategory.PLASTIC_METAL,
                    WasteCategory.ORGANIC_WET,
                    WasteCategory.RECYCLABLE_DRY
                ),
                status = BinStatus.OPERATIONAL,
                buildingCode = "Uni-Mall / Library"
            ),
            CampusBin(
                id = "bin_lpu_unistreet",
                name = "Uni-Street Food Kiosks & Tuck Shops",
                locationDescription = "Pedestrian walkway beside Nescafe & Juice Counters",
                latitude = 31.2550,
                longitude = 75.7045,
                acceptedCategories = listOf(
                    WasteCategory.ORGANIC_WET,
                    WasteCategory.PLASTIC_METAL,
                    WasteCategory.RECYCLABLE_DRY
                ),
                status = BinStatus.NEAR_FULL,
                buildingCode = "Uni-Street"
            ),
            CampusBin(
                id = "bin_lpu_block38",
                name = "Block 38 Robotics & E-Waste Drop-off",
                locationDescription = "2nd Floor Hardware Innovation Lab corridor",
                latitude = 31.2529,
                longitude = 75.7052,
                acceptedCategories = listOf(
                    WasteCategory.E_WASTE,
                    WasteCategory.PLASTIC_METAL
                ),
                status = BinStatus.OPERATIONAL,
                buildingCode = "Block 38 (ECE)"
            ),
            CampusBin(
                id = "bin_lpu_unipolis",
                name = "Baldev Raj Mittal Unipolis Amphitheatre",
                locationDescription = "Open-Air Stage South Perimeter, beside Seating Steps",
                latitude = 31.2540,
                longitude = 75.7058,
                acceptedCategories = listOf(
                    WasteCategory.PLASTIC_METAL,
                    WasteCategory.RECYCLABLE_DRY,
                    WasteCategory.PAPER_CARDBOARD
                ),
                status = BinStatus.OPERATIONAL,
                buildingCode = "Unipolis"
            ),
            CampusBin(
                id = "bin_lpu_sdm",
                name = "Shanti Devi Mittal Auditorium (SDM)",
                locationDescription = "Auditorium Main Reception & Exhibition Hallway",
                latitude = 31.2515,
                longitude = 75.7042,
                acceptedCategories = listOf(
                    WasteCategory.PAPER_CARDBOARD,
                    WasteCategory.PLASTIC_METAL,
                    WasteCategory.RECYCLABLE_DRY
                ),
                status = BinStatus.OPERATIONAL,
                buildingCode = "SDM Auditorium"
            ),
            CampusBin(
                id = "bin_lpu_hostel_bh",
                name = "Hostel Quad (BH-1 & BH-4)",
                locationDescription = "Mess Dining Entrance & Courtyard Recreation Area",
                latitude = 31.2562,
                longitude = 75.7065,
                acceptedCategories = listOf(
                    WasteCategory.ORGANIC_WET,
                    WasteCategory.PLASTIC_METAL,
                    WasteCategory.PAPER_CARDBOARD,
                    WasteCategory.RECYCLABLE_DRY
                ),
                status = BinStatus.OPERATIONAL,
                buildingCode = "Hostel BH-4"
            ),
            CampusBin(
                id = "bin_lpu_hospital",
                name = "LPU Uni-Hospital & Healthcare Clinic",
                locationDescription = "Emergency & Pharmacy Outer Wing, Sanitized Disposal Zone",
                latitude = 31.2508,
                longitude = 75.7018,
                acceptedCategories = listOf(
                    WasteCategory.HAZARDOUS_SANITARY,
                    WasteCategory.GLASS
                ),
                status = BinStatus.OPERATIONAL,
                buildingCode = "Uni-Hospital"
            )
        )
    }
}
