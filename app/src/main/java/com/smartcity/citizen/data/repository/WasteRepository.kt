package com.smartcity.citizen.data.repository

import com.smartcity.citizen.data.local.dao.WasteRecordDao
import com.smartcity.citizen.data.local.entity.WasteRecordEntity
import com.smartcity.citizen.model.DailyTip
import com.smartcity.citizen.model.DisposalGuideline
import com.smartcity.citizen.model.WasteCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface WasteRepository {
    val allRecordsFlow: Flow<List<WasteRecordEntity>>
    val recordCountFlow: Flow<Int>
    val recentRecordsFlow: Flow<List<WasteRecordEntity>>

    suspend fun saveRecord(record: WasteRecordEntity): Long
    suspend fun updateRecord(record: WasteRecordEntity): Int
    suspend fun deleteRecord(record: WasteRecordEntity): Int
    suspend fun getRecordById(id: String): WasteRecordEntity?
    fun getDisposalGuideline(category: WasteCategory): DisposalGuideline
    fun getAllGuidelines(): List<DisposalGuideline>
    fun getDailyTips(): List<DailyTip>
    fun getTodayTip(): DailyTip
}

class WasteRepositoryImpl(
    private val dao: WasteRecordDao
) : WasteRepository {

    override val allRecordsFlow: Flow<List<WasteRecordEntity>> = dao.getAllRecordsFlow()
    override val recordCountFlow: Flow<Int> = dao.getRecordCountFlow()
    override val recentRecordsFlow: Flow<List<WasteRecordEntity>> = dao.getRecentRecordsFlow(5)

    override suspend fun saveRecord(record: WasteRecordEntity): Long = dao.insertRecord(record)
    override suspend fun updateRecord(record: WasteRecordEntity): Int = dao.updateRecord(record)
    override suspend fun deleteRecord(record: WasteRecordEntity): Int = dao.deleteRecord(record)
    override suspend fun getRecordById(id: String): WasteRecordEntity? = dao.getRecordById(id)

    override fun getDisposalGuideline(category: WasteCategory): DisposalGuideline {
        return guidelinesMap[category] ?: defaultGuideline(category)
    }

    override fun getAllGuidelines(): List<DisposalGuideline> {
        return WasteCategory.entries.map { getDisposalGuideline(it) }
    }

    override fun getDailyTips(): List<DailyTip> = dailyTipsList

    override fun getTodayTip(): DailyTip {
        val dayIndex = (System.currentTimeMillis() / (1000 * 60 * 60 * 24) % dailyTipsList.size).toInt()
        return dailyTipsList[dayIndex]
    }

    private fun defaultGuideline(category: WasteCategory) = DisposalGuideline(
        category = category,
        recommendedBin = category.binColorName,
        binColorHex = category.hexColor,
        preparationSteps = listOf("Empty contents", "Segregate into designated bin"),
        commonCampusExamples = listOf("General waste")
    )

    companion object {
        val guidelinesMap: Map<WasteCategory, DisposalGuideline> = mapOf(
            WasteCategory.ORGANIC_WET to DisposalGuideline(
                category = WasteCategory.ORGANIC_WET,
                recommendedBin = "Green Bin (Compost / Biodegradable)",
                binColorHex = WasteCategory.ORGANIC_WET.hexColor,
                preparationSteps = listOf(
                    "Remove any plastic wrapping or stickers from fruits",
                    "Drain excessive liquid before discarding",
                    "Do not place plastic cutlery or styrofoam containers here"
                ),
                safetyWarning = "Do not mix with animal bones or toxic garden chemicals in campus composting.",
                commonCampusExamples = listOf(
                    "Canteen food scraps", "Fruit peels & apple cores", "Coffee grounds & tea bags",
                    "Fallen leaves", "Leftover vegetable salad", "Biodegradable paper napkins"
                )
            ),
            WasteCategory.RECYCLABLE_DRY to DisposalGuideline(
                category = WasteCategory.RECYCLABLE_DRY,
                recommendedBin = "Blue Bin (Clean Dry Recyclables)",
                binColorHex = WasteCategory.RECYCLABLE_DRY.hexColor,
                preparationSteps = listOf(
                    "Ensure items are clean and reasonably dry",
                    "Flatten corrugated packaging to conserve bin volume",
                    "Separate plastic liners from outer paper boxes"
                ),
                safetyWarning = "Contaminated dry waste with gravy or oil cannot be recycled and spoils other paper.",
                commonCampusExamples = listOf(
                    "Delivery cardboard packaging", "Clean plastic wrappers", "Tetra Pak juice cartons",
                    "Stationery blister packs", "Packaging foam peanuts"
                )
            ),
            WasteCategory.PAPER_CARDBOARD to DisposalGuideline(
                category = WasteCategory.PAPER_CARDBOARD,
                recommendedBin = "Blue / White Bin (Paper Recycling Hub)",
                binColorHex = WasteCategory.PAPER_CARDBOARD.hexColor,
                preparationSteps = listOf(
                    "Remove plastic spirals and metal clips if possible",
                    "Keep flat and avoid crumpling excessively",
                    "Shred confidential notes before campus recycling drop-off"
                ),
                safetyWarning = "Waxed paper, greasy pizza box bottoms, and glossy thermal receipts are not recyclable.",
                commonCampusExamples = listOf(
                    "Exam answer sheets", "College notebooks & lab manuals", "Study handouts & syllabus copies",
                    "Newspapers & library periodicals", "Corrugated project presentation boxes"
                )
            ),
            WasteCategory.PLASTIC_METAL to DisposalGuideline(
                category = WasteCategory.PLASTIC_METAL,
                recommendedBin = "Blue / Yellow Bin (Plastic & Metal)",
                binColorHex = WasteCategory.PLASTIC_METAL.hexColor,
                preparationSteps = listOf(
                    "Rinse thoroughly to remove remaining liquids or grease",
                    "Crush bottles and cans to save space",
                    "Keep caps attached so they do not get lost in sorting machinery"
                ),
                safetyWarning = "Never place aerosol cans that are under pressure; ensure they are fully discharged.",
                commonCampusExamples = listOf(
                    "PET water bottles", "Soda & energy drink aluminum cans", "Plastic food tubs & lids",
                    "Sanitizer empty bottles", "Plastic bottle caps & drink trays"
                )
            ),
            WasteCategory.GLASS to DisposalGuideline(
                category = WasteCategory.GLASS,
                recommendedBin = "Teal Bin (Glassware Only)",
                binColorHex = WasteCategory.GLASS.hexColor,
                preparationSteps = listOf(
                    "Empty beverage dregs and rinse container",
                    "Remove metal lids or corks and place them in metal bin",
                    "Wrap broken glass in newspaper and label before disposal"
                ),
                safetyWarning = "Wear protective gloves if handling broken glass. Report laboratory broken glassware directly to lab attendant.",
                commonCampusExamples = listOf(
                    "Glass soft drink bottles", "Preserve jars", "Clean test tubes & beakers",
                    "Glass cosmetic containers"
                )
            ),
            WasteCategory.E_WASTE to DisposalGuideline(
                category = WasteCategory.E_WASTE,
                recommendedBin = "Red Bin (Campus Tech / E-Waste Hub)",
                binColorHex = WasteCategory.E_WASTE.hexColor,
                preparationSteps = listOf(
                    "Tape battery terminals (especially 9V and Lithium cells) to prevent sparks",
                    "Back up and erase personal data from phones/storage drives",
                    "Drop off at designated Department E-Waste stations"
                ),
                safetyWarning = "Do not crush, pierce, or dispose of lithium-ion batteries in ordinary trash. Fire hazard!",
                commonCampusExamples = listOf(
                    "Damaged USB cables & chargers", "AA / AAA / Lithium-ion batteries", "Broken headphones & earbuds",
                    "Obsolete computer mice & keyboards", "Arduino / Raspberry Pi circuit boards"
                )
            ),
            WasteCategory.HAZARDOUS_SANITARY to DisposalGuideline(
                category = WasteCategory.HAZARDOUS_SANITARY,
                recommendedBin = "Yellow Bin (Biohazard & Sanitary)",
                binColorHex = WasteCategory.HAZARDOUS_SANITARY.hexColor,
                preparationSteps = listOf(
                    "Wrap securely in dedicated newspaper or paper bag",
                    "Mark with a red cross or disposal symbol if possible",
                    "Deposit directly in sanitary incinerator bin or campus medical clinic"
                ),
                safetyWarning = "Biohazard & health risk. Never mix with standard student recyclables.",
                commonCampusExamples = listOf(
                    "Face masks & medical gloves", "Sanitary napkins & diapers", "Chemical spill absorbent pads",
                    "Expired medications & blister packs", "Antiseptic cotton swabs"
                )
            )
        )

        val dailyTipsList = listOf(
            DailyTip(
                id = 1,
                title = "BYOM (Bring Your Own Mug)",
                description = "Campus cafeterias discard over 500 paper cups daily. Bringing a reusable thermos saves trees and keeps coffee warmer.",
                challenge = "Avoid single-use cups all day by using your own tumbler.",
                category = WasteCategory.RECYCLABLE_DRY
            ),
            DailyTip(
                id = 2,
                title = "Duplex Printing Habit",
                description = "When printing assignments and lab records, print double-sided (duplex) to cut paper usage by exactly 50%.",
                challenge = "Submit your next report printed double-sided or digitally.",
                category = WasteCategory.PAPER_CARDBOARD
            ),
            DailyTip(
                id = 3,
                title = "Rinse Before You Bin",
                description = "A single greasy plastic container can contaminate an entire 50-liter batch of clean recyclable paper in the campus bin.",
                challenge = "Rinse your beverage bottle or food container before dropping it into the Blue Bin.",
                category = WasteCategory.PLASTIC_METAL
            ),
            DailyTip(
                id = 4,
                title = "Safe Battery Recycling",
                description = "Dead laptop and calculator batteries contain heavy metals like Cadmium and Lithium that leach into soil if thrown in regular trash.",
                challenge = "Locate the nearest Red E-Waste bin at the Engineering Block and deposit any dead batteries.",
                category = WasteCategory.E_WASTE
            ),
            DailyTip(
                id = 5,
                title = "Compost Your Cafeteria Lunch",
                description = "Food scraps left in the Green Bin are converted into nutrient-rich compost for the campus botanical garden.",
                challenge = "Segregate fruit peels and canteen scraps into the Green Compost Bin after lunch.",
                category = WasteCategory.ORGANIC_WET
            )
        )
    }
}
