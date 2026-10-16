package com.smartcity.citizen.model

/**
 * Standard Campus Waste Segregation Categories
 */
enum class WasteCategory(
    val displayName: String,
    val binColorName: String,
    val hexColor: Long,
    val iconDescription: String
) {
    ORGANIC_WET(
        displayName = "Wet & Organic",
        binColorName = "Green Bin",
        hexColor = 0xFF2E7D32, // Dark Green
        iconDescription = "Food scraps, fruit peels, leaves, biodegradable cafeteria waste"
    ),
    RECYCLABLE_DRY(
        displayName = "Dry & Recyclable",
        binColorName = "Blue Bin",
        hexColor = 0xFF1565C0, // Blue
        iconDescription = "Clean packaging cartons, plastic wraps, recyclable cardboard"
    ),
    PAPER_CARDBOARD(
        displayName = "Paper & Cardboard",
        binColorName = "Blue / White Bin",
        hexColor = 0xFF0288D1, // Cyan/Light Blue
        iconDescription = "Exam papers, notebooks, corrugated cartons, printouts"
    ),
    PLASTIC_METAL(
        displayName = "Plastic & Metal",
        binColorName = "Blue / Yellow Bin",
        hexColor = 0xFFF57C00, // Amber/Orange
        iconDescription = "Beverage bottles, soda cans, plastic containers, bottle caps"
    ),
    GLASS(
        displayName = "Glassware",
        binColorName = "Teal Bin",
        hexColor = 0xFF00897B, // Teal
        iconDescription = "Glass beverage bottles, unbroken lab glass containers"
    ),
    E_WASTE(
        displayName = "E-Waste & Batteries",
        binColorName = "Red Bin / Tech Hub",
        hexColor = 0xFFC62828, // Red
        iconDescription = "USB cables, old chargers, batteries, electronics, circuit boards"
    ),
    HAZARDOUS_SANITARY(
        displayName = "Hazardous & Sanitary",
        binColorName = "Yellow / Biohazard Bin",
        hexColor = 0xFFD81B60, // Deep Pink/Red
        iconDescription = "Sanitary waste, masks, chemical lab residue, expired medicines"
    )
}

/**
 * Detailed disposal preparation guidelines
 */
data class DisposalGuideline(
    val category: WasteCategory,
    val recommendedBin: String,
    val binColorHex: Long,
    val preparationSteps: List<String>,
    val safetyWarning: String? = null,
    val commonCampusExamples: List<String>
)

/**
 * Classification inference output
 */
data class ClassificationResult(
    val predictedCategory: WasteCategory,
    val confidence: Float,               // 0.0f to 1.0f
    val isUncertain: Boolean,             // true if confidence < 0.60
    val detectedLabel: String,
    val guideline: DisposalGuideline,
    val alternativeCategory: WasteCategory? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Campus waste collection point entity model
 */
data class CampusBin(
    val id: String,
    val name: String,
    val locationDescription: String,
    val latitude: Double,
    val longitude: Double,
    val acceptedCategories: List<WasteCategory>,
    val status: BinStatus = BinStatus.OPERATIONAL,
    val buildingCode: String = "Campus Main"
)

enum class BinStatus {
    OPERATIONAL,
    NEAR_FULL,
    MAINTENANCE
}

/**
 * Daily sustainability tip and challenge
 */
data class DailyTip(
    val id: Int,
    val title: String,
    val description: String,
    val challenge: String,
    val category: WasteCategory
)
