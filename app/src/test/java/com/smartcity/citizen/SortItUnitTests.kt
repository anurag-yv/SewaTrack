package com.smartcity.citizen

import com.smartcity.citizen.data.repository.CampusBinRepositoryImpl
import com.smartcity.citizen.data.repository.WasteRepositoryImpl
import com.smartcity.citizen.model.WasteCategory
import org.junit.Assert.*
import org.junit.Test

/**
 * SortIt Automated Unit Tests
 * Fulfills CSE226 Unit VI: Basic Unit Testing using JUnit
 */
class SortItUnitTests {

    @Test
    fun testAllSevenCategoriesHaveDisposalGuidelines() {
        val allCategories = WasteCategory.entries
        assertEquals("There must be exactly 7 waste categories supported", 7, allCategories.size)

        for (category in allCategories) {
            val guideline = WasteRepositoryImpl.guidelinesMap[category]
            assertNotNull("Guideline must exist for ${category.name}", guideline)
            assertTrue("Guideline must have preparation steps", guideline!!.preparationSteps.isNotEmpty())
            assertTrue("Guideline must list common campus examples", guideline.commonCampusExamples.isNotEmpty())
            assertNotNull("Guideline must define a recommended bin", guideline.recommendedBin)
        }
    }

    @Test
    fun testUncertaintyThresholdDetection() {
        val threshold = 0.60f

        val highConfidence = 0.88f
        val lowConfidence = 0.42f
        val edgeConfidence = 0.599f

        assertFalse("High confidence should not be flagged as uncertain", highConfidence < threshold)
        assertTrue("Low confidence should be flagged as uncertain", lowConfidence < threshold)
        assertTrue("Edge confidence below 0.60 should be uncertain", edgeConfidence < threshold)
    }

    @Test
    fun testCategoryColorFormatting() {
        for (category in WasteCategory.entries) {
            assertTrue("Category color hex must be non-zero", category.hexColor != 0L)
            assertTrue("Category display name must not be blank", category.displayName.isNotBlank())
            assertTrue("Category bin color name must be specified", category.binColorName.isNotBlank())
        }
    }

    @Test
    fun testCampusBinsHaveDistinctLocationsAndCoordinates() {
        val bins = CampusBinRepositoryImpl.defaultCampusBins
        assertTrue("At least 5 campus collection stations should be seeded", bins.size >= 5)

        for (bin in bins) {
            assertTrue("Bin name must be non-empty", bin.name.isNotBlank())
            assertTrue("Building code must be non-empty", bin.buildingCode.isNotBlank())
            assertTrue("Latitude must be within valid range", bin.latitude in -90.0..90.0)
            assertTrue("Longitude must be within valid range", bin.longitude in -180.0..180.0)
            assertTrue("Bin must accept at least 1 waste stream", bin.acceptedCategories.isNotEmpty())
        }
    }

    @Test
    fun testDailyEcoTipsAndChallengesIntegrity() {
        val tips = WasteRepositoryImpl.dailyTipsList
        assertTrue("Daily tips collection must contain entries", tips.isNotEmpty())

        for (tip in tips) {
            assertTrue("Tip title must be descriptive", tip.title.length > 5)
            assertTrue("Tip description must provide context", tip.description.length > 15)
            assertTrue("Student challenge must be actionable", tip.challenge.isNotBlank())
            assertNotNull("Category must be linked", tip.category)
        }
    }

    @Test
    fun testSoftmaxProbabilitiesSumToOne() {
        val logits = floatArrayOf(2.5f, 1.0f, 0.2f, -1.0f, 3.2f, 0.5f, -0.5f)
        var maxLogit = Float.NEGATIVE_INFINITY
        for (l in logits) if (l > maxLogit) maxLogit = l

        var sum = 0.0f
        val exp = FloatArray(logits.size)
        for (i in logits.indices) {
            exp[i] = Math.exp((logits[i] - maxLogit).toDouble()).toFloat()
            sum += exp[i]
        }
        val probabilities = FloatArray(logits.size) { exp[it] / sum }

        var total = 0f
        for (p in probabilities) total += p

        assertEquals(1.0f, total, 0.001f)
        assertTrue(probabilities[4] > probabilities[0]) // 3.2 is max logit
    }
}
