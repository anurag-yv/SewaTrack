package com.smartcity.citizen.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartcity.citizen.data.local.SortItDatabase
import com.smartcity.citizen.data.local.entity.WasteRecordEntity
import com.smartcity.citizen.data.repository.WasteRepository
import com.smartcity.citizen.data.repository.WasteRepositoryImpl
import com.smartcity.citizen.model.WasteCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val records: List<WasteRecordEntity> = emptyList(),
    val totalCount: Int = 0,
    val categoryCounts: Map<WasteCategory, Int> = emptyMap(),
    val diversionRatePercent: Int = 0,
    val selectedFilterCategory: WasteCategory? = null
)

/**
 * ViewModel for Personal Waste Sorting History & Analytics
 * Fulfills CSE226 Unit IV: Room with Flow & Unit III: MVVM
 */
class HistoryViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: WasteRepository = WasteRepositoryImpl(
        SortItDatabase.getDatabase(application).wasteRecordDao()
    )
) : AndroidViewModel(application) {

    private val _filterCategory = MutableStateFlow<WasteCategory?>(null)
    val filterCategory: StateFlow<WasteCategory?> = _filterCategory.asStateFlow()

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.allRecordsFlow,
        _filterCategory
    ) { allRecords, filter ->
        val counts = allRecords.groupingBy { it.confirmedCategory }.eachCount()
        val total = allRecords.size
        val recyclable = allRecords.count {
            it.confirmedCategory != WasteCategory.HAZARDOUS_SANITARY
        }
        val rate = if (total > 0) ((recyclable.toFloat() / total) * 100).toInt() else 100

        val filteredList = if (filter == null) allRecords else allRecords.filter { it.confirmedCategory == filter }

        HistoryUiState(
            records = filteredList,
            totalCount = total,
            categoryCounts = counts,
            diversionRatePercent = rate,
            selectedFilterCategory = filter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun setFilterCategory(category: WasteCategory?) {
        _filterCategory.value = category
    }

    fun deleteRecord(record: WasteRecordEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRecord(record)
        }
    }
}
