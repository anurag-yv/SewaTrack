package com.smartcity.citizen.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartcity.citizen.ai.WasteClassifier
import com.smartcity.citizen.data.local.SortItDatabase
import com.smartcity.citizen.data.local.entity.WasteRecordEntity
import com.smartcity.citizen.data.repository.WasteRepository
import com.smartcity.citizen.data.repository.WasteRepositoryImpl
import com.smartcity.citizen.model.ClassificationResult
import com.smartcity.citizen.model.WasteCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

sealed interface ClassificationUiState {
    object Idle : ClassificationUiState
    data class ImageSelected(val uri: Uri, val previewBitmap: Bitmap) : ClassificationUiState
    object Classifying : ClassificationUiState
    data class Success(
        val result: ClassificationResult,
        val imageUri: Uri,
        val confirmedCategory: WasteCategory,
        val isSaved: Boolean = false
    ) : ClassificationUiState
    data class Error(val message: String) : ClassificationUiState
}

/**
 * ViewModel for Waste Classification
 * Fulfills CSE226 Unit III: MVVM, State Management, ViewModel, StateFlow, Coroutines
 */
class WasteClassifierViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: WasteRepository = WasteRepositoryImpl(
        SortItDatabase.getDatabase(application).wasteRecordDao()
    ),
    private val classifier: WasteClassifier = WasteClassifier(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ClassificationUiState>(ClassificationUiState.Idle)
    val uiState: StateFlow<ClassificationUiState> = _uiState.asStateFlow()

    // SharedFlow for one-time events (CSE226 Unit II: SharedFlow for reactive event emission)
    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    var currentCapturedUri: Uri? = null

    fun onImageSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ClassificationUiState.Classifying
            try {
                val bitmap = loadAndScaleBitmap(uri)
                if (bitmap == null) {
                    _uiState.value = ClassificationUiState.Error("Unable to decode image. Please choose another photo.")
                    _eventFlow.emit("Failed to load image")
                    return@launch
                }

                _uiState.value = ClassificationUiState.ImageSelected(uri, bitmap)

                // Run classification with timeout (CSE226 Unit II: Timeouts and Cancellation)
                withTimeout(10000L) {
                    val result = classifier.classifyImage(bitmap)
                    _uiState.value = ClassificationUiState.Success(
                        result = result,
                        imageUri = uri,
                        confirmedCategory = result.predictedCategory
                    )
                    if (result.isUncertain) {
                        _eventFlow.emit("Low confidence prediction (${(result.confidence * 100).toInt()}%). Please verify category.")
                    } else {
                        _eventFlow.emit("Classified as ${result.predictedCategory.displayName}")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = ClassificationUiState.Error(e.message ?: "Classification failed")
                _eventFlow.emit("Error during classification: ${e.message}")
            }
        }
    }

    fun updateConfirmedCategory(newCategory: WasteCategory) {
        val currentState = _uiState.value
        if (currentState is ClassificationUiState.Success) {
            _uiState.value = currentState.copy(confirmedCategory = newCategory)
            viewModelScope.launch {
                _eventFlow.emit("Updated to ${newCategory.displayName}")
            }
        }
    }

    fun saveClassificationRecord(notes: String = "") {
        val currentState = _uiState.value
        if (currentState is ClassificationUiState.Success) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val record = WasteRecordEntity(
                        id = UUID.randomUUID().toString(),
                        imageUri = currentState.imageUri.toString(),
                        predictedCategory = currentState.result.predictedCategory,
                        confirmedCategory = currentState.confirmedCategory,
                        confidence = currentState.result.confidence,
                        isUncertain = currentState.result.isUncertain,
                        detectedLabel = currentState.result.detectedLabel,
                        notes = notes,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.saveRecord(record)
                    withContext(Dispatchers.Main) {
                        _uiState.value = currentState.copy(isSaved = true)
                        _eventFlow.emit("Saved to your waste sorting history!")
                    }
                } catch (e: Exception) {
                    _eventFlow.emit("Failed to save: ${e.message}")
                }
            }
        }
    }

    fun reset() {
        _uiState.value = ClassificationUiState.Idle
        currentCapturedUri = null
    }

    private suspend fun loadAndScaleBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = getApplication<Application>().contentResolver
            val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                val input = contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(input)
                input?.close()
                bmp
            } ?: return@withContext null

            // Downscale to max 800px preserving aspect ratio for fast processing & UI responsiveness
            val maxSize = 800
            val ratio = minOf(maxSize.toFloat() / rawBitmap.width, maxSize.toFloat() / rawBitmap.height, 1.0f)
            if (ratio < 1.0f) {
                val scaled = Bitmap.createScaledBitmap(
                    rawBitmap,
                    (rawBitmap.width * ratio).toInt(),
                    (rawBitmap.height * ratio).toInt(),
                    true
                )
                if (scaled != rawBitmap) rawBitmap.recycle()
                scaled
            } else {
                rawBitmap
            }
        } catch (e: Exception) {
            null
        }
    }
}
