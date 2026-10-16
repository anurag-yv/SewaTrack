package com.smartcity.citizen.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcity.citizen.data.repository.WasteRepository
import com.smartcity.citizen.data.repository.WasteRepositoryImpl
import com.smartcity.citizen.model.DisposalGuideline
import com.smartcity.citizen.model.WasteCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class GuideUiState(
    val searchQuery: String = "",
    val selectedCategory: WasteCategory? = null,
    val guidelines: List<DisposalGuideline> = emptyList()
)

/**
 * WasteGuideViewModel demonstrating SavedStateHandle and Flow operators
 * Fulfills CSE226 Unit III: SavedStateHandle & Unit II: Flow Operators (combine, stateIn)
 */
class WasteGuideViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: WasteRepository
) : ViewModel() {

    private val KEY_SEARCH_QUERY = "key_guide_search"
    private val KEY_SELECTED_CATEGORY = "key_guide_category"

    val searchQuery: StateFlow<String> = savedStateHandle.getStateFlow(KEY_SEARCH_QUERY, "")

    private val _selectedCategory = MutableStateFlow<WasteCategory?>(
        savedStateHandle.get<String>(KEY_SELECTED_CATEGORY)?.let {
            runCatching { WasteCategory.valueOf(it) }.getOrNull()
        }
    )
    val selectedCategory: StateFlow<WasteCategory?> = _selectedCategory

    val allGuidelines = repository.getAllGuidelines()

    val uiState: StateFlow<GuideUiState> = combine(
        searchQuery,
        _selectedCategory
    ) { query, category ->
        val filtered = allGuidelines.filter { guideline ->
            val matchesCategory = category == null || guideline.category == category
            val matchesQuery = query.isBlank() ||
                    guideline.category.displayName.contains(query, ignoreCase = true) ||
                    guideline.commonCampusExamples.any { it.contains(query, ignoreCase = true) } ||
                    guideline.recommendedBin.contains(query, ignoreCase = true) ||
                    guideline.preparationSteps.any { it.contains(query, ignoreCase = true) }
            matchesCategory && matchesQuery
        }
        GuideUiState(
            searchQuery = query,
            selectedCategory = category,
            guidelines = filtered
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GuideUiState(guidelines = allGuidelines)
    )

    fun onSearchQueryChanged(newQuery: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = newQuery
    }

    fun onCategorySelected(category: WasteCategory?) {
        _selectedCategory.value = category
        savedStateHandle[KEY_SELECTED_CATEGORY] = category?.name
    }
}
