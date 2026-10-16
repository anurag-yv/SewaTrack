package com.smartcity.citizen.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.smartcity.citizen.data.local.SortItDatabase
import com.smartcity.citizen.data.repository.CampusBinRepository
import com.smartcity.citizen.data.repository.CampusBinRepositoryImpl
import com.smartcity.citizen.model.CampusBin
import com.smartcity.citizen.model.WasteCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BinWithDistance(
    val bin: CampusBin,
    val distanceMeters: Float? = null
) {
    val formattedDistance: String
        get() = when {
            distanceMeters == null -> "Distance unavailable"
            distanceMeters < 1000 -> "${distanceMeters.toInt()}m away"
            else -> String.format("%.1f km away", distanceMeters / 1000f)
        }
}

data class BinLocatorUiState(
    val bins: List<BinWithDistance> = emptyList(),
    val userLocation: Location? = null,
    val selectedBin: CampusBin? = null,
    val selectedCategoryFilter: WasteCategory? = null,
    val isLocating: Boolean = false,
    val isMapMode: Boolean = false
)

/**
 * ViewModel for Campus Bin Locator & Google Maps
 * Fulfills CSE226 Unit V: Location-Based Android Applications & Google Maps Compose
 */
class CampusBinLocatorViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: CampusBinRepository = CampusBinRepositoryImpl(
        SortItDatabase.getDatabase(application).campusBinDao()
    )
) : AndroidViewModel(application) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    private val _selectedBin = MutableStateFlow<CampusBin?>(null)
    val selectedBin: StateFlow<CampusBin?> = _selectedBin.asStateFlow()

    private val _selectedFilter = MutableStateFlow<WasteCategory?>(null)
    val selectedFilter: StateFlow<WasteCategory?> = _selectedFilter.asStateFlow()

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    private val _isMapMode = MutableStateFlow(false)
    val isMapMode: StateFlow<Boolean> = _isMapMode.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeSeedData()
        }
    }

    val uiState: StateFlow<BinLocatorUiState> = combine(
        repository.allBinsFlow,
        _userLocation,
        _selectedBin,
        _selectedFilter,
        combine(_isLocating, _isMapMode) { locating, mapMode -> Pair(locating, mapMode) }
    ) { bins, location, selected, filter, uiFlags ->
        val (locating, mapMode) = uiFlags
        val filteredBins = if (filter == null) {
            bins
        } else {
            bins.filter { it.acceptedCategories.contains(filter) }
        }

        val binsWithDistances = filteredBins.map { bin ->
            val distance = location?.let { loc ->
                val results = FloatArray(1)
                Location.distanceBetween(loc.latitude, loc.longitude, bin.latitude, bin.longitude, results)
                results[0]
            }
            BinWithDistance(bin = bin, distanceMeters = distance)
        }.sortedBy { it.distanceMeters ?: Float.MAX_VALUE }

        BinLocatorUiState(
            bins = binsWithDistances,
            userLocation = location,
            selectedBin = selected,
            selectedCategoryFilter = filter,
            isLocating = locating,
            isMapMode = mapMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BinLocatorUiState()
    )


    @SuppressLint("MissingPermission")
    fun fetchUserLocation() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLocating.value = true
            try {
                val cts = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cts.token
                ).addOnSuccessListener { loc ->
                    if (loc != null) {
                        _userLocation.value = loc
                    }
                    _isLocating.value = false
                }.addOnFailureListener {
                    _isLocating.value = false
                }
            } catch (e: Exception) {
                _isLocating.value = false
            }
        }
    }

    fun selectBin(bin: CampusBin?) {
        _selectedBin.value = bin
    }

    fun setCategoryFilter(category: WasteCategory?) {
        _selectedFilter.value = category
    }

    fun toggleViewMode() {
        _isMapMode.value = !_isMapMode.value
    }
}
