package com.smartcity.citizen.ai

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.smartcity.citizen.data.models.IssueType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ImageClassifier(private val context: Context) {

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.DEFAULT_OPTIONS
    )

    suspend fun classify(bitmap: Bitmap): IssueType = withContext(Dispatchers.IO) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val labels = labeler.process(image).await()
        val labelTexts = labels.map { it.text.lowercase() }
        return@withContext when {
            labelTexts.any { it.contains("pothole") || it.contains("road") || it.contains("asphalt") } -> IssueType.POTHOLE
            labelTexts.any { it.contains("garbage") || it.contains("trash") || it.contains("litter") || it.contains("rubbish") } -> IssueType.GARBAGE
            labelTexts.any { it.contains("streetlight") || it.contains("lamp") || it.contains("light pole") } -> IssueType.STREETLIGHT
            labelTexts.any { it.contains("water") || it.contains("leak") || it.contains("pipe") || it.contains("plumbing") } -> IssueType.WATER_LEAKAGE
            else -> IssueType.OTHER
        }
    }
}