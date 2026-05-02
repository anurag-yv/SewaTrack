package com.smartcity.citizen.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

@Composable
fun LocationPicker(
    onLocationSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var locationText by remember { mutableStateOf("") }
    var isGettingLocation by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Helper function to get address from coordinates
    fun getAddressFromLocation(location: Location, context: Context, onResult: (String) -> Unit) {
        val geocoder = android.location.Geocoder(context)
        try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val addressLine = address.getAddressLine(0)
                    ?: "${address.locality ?: ""}, ${address.countryName ?: ""}".trim(',')
                onResult(if (addressLine.isNotEmpty()) addressLine else "${location.latitude}, ${location.longitude}")
            } else {
                onResult("${location.latitude}, ${location.longitude}")
            }
        } catch (e: Exception) {
            onResult("${location.latitude}, ${location.longitude}")
        }
    }

    // Function to get current location
    fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        scope.launch {
            isGettingLocation = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    getAddressFromLocation(location, context) { address ->
                        locationText = address
                        onLocationSelected(address)
                        isGettingLocation = false
                    }
                } else {
                    Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
                    isGettingLocation = false
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to get location: ${it.message}", Toast.LENGTH_SHORT).show()
                isGettingLocation = false
            }
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = locationText,
            onValueChange = { newText ->
                locationText = newText
                onLocationSelected(newText)
            },
            label = { Text("Location *") },
            placeholder = { Text("Enter address, landmark, or coordinates") },
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            trailingIcon = {
                IconButton(
                    onClick = { getCurrentLocation() },
                    enabled = !isGettingLocation
                ) {
                    if (isGettingLocation) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.MyLocation, contentDescription = "Use current location")
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = androidx.compose.ui.graphics.Color(0xFF424242),
                unfocusedBorderColor = androidx.compose.ui.graphics.Color(0xFFBDBDBD)
            )
        )
        Text(
            text = "You can type an address, landmark, or coordinates (e.g., 28.6139,77.2090)",
            style = MaterialTheme.typography.bodySmall,
            color = androidx.compose.ui.graphics.Color.Gray,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )
    }
}