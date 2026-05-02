package com.smartcity.citizen.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.smartcity.citizen.BuildConfig
import com.smartcity.citizen.data.models.IssueType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class AIDescriptionGenerator {

    companion object {
        private const val TAG = "AIDescriptionGenerator"
    }

    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun generateDescription(
        rawDescription: String,
        issueType: IssueType,
        location: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Convert this into a formal municipal complaint:
                
                Issue: ${issueType.name.replace("_", " ")}
                Location: $location
                Details: $rawDescription
                
                Keep it under 3 sentences.
            """.trimIndent()

            Log.d(TAG, "Prompt: $prompt")

            val response = withTimeoutOrNull(15000L) {
                model.generateContent(prompt)
            }

            val result = response?.text?.trim()

            if (result.isNullOrEmpty()) {
                Log.e(TAG, "Empty response from Gemini")
                "Unable to generate description. Try again."
            } else {
                Log.d(TAG, "Response: $result")
                result
            }

        } catch (e: Exception) {
            Log.e(TAG, "FULL ERROR", e)
            return@withContext "ERROR: ${e.message}"
        }
    }
}