package com.github.ebrooks2002.fisherfinder.viewModel

import android.content.Context
import android.hardware.GeomagneticField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.ebrooks2002.fisherfinder.model.AssetData
import kotlinx.coroutines.launch
import android.location.Location
import android.util.Log
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.ebrooks2002.fisherfinder.FisherFinderApplication
import com.github.ebrooks2002.fisherfinder.data.AssetRepository
import com.github.ebrooks2002.fisherfinder.location.LocationFinder
import com.github.ebrooks2002.fisherfinder.location.RotationSensor
import com.github.ebrooks2002.fisherfinder.data.MessageItem
import com.github.ebrooks2002.fisherfinder.model.NavigationState
import com.github.ebrooks2002.fisherfinder.data.getCurrentSpeed
import com.github.ebrooks2002.fisherfinder.data.getFreshnessColor
import kotlin.math.round

sealed interface FisherFinderUiState {
    data class Success(val messageList: List<MessageItem>) : FisherFinderUiState
    data class Error(val messageList: List<MessageItem>) : FisherFinderUiState
    object Loading : FisherFinderUiState
}

/**
 * The view model for the Buoy Finder app.
 *
 * Tracks the state of the UI, launches coroutine to
 * asynchronously retrieve and hold user location and rotation data, and asset data.
 *
 * @author E. Brooks
 */
class FisherFinderViewModel(
    val assetRepository: AssetRepository,
    val locationFinder: LocationFinder,
    val rotationSensor: RotationSensor,
) : ViewModel() {
    var fisherFinderUiState: FisherFinderUiState by mutableStateOf(FisherFinderUiState.Loading)
        private set
    var userLocation: Location? by mutableStateOf(null)
        private set
    var userRotation: Float? by mutableStateOf(null)
        private set
    var selectedAssetName by mutableStateOf<String?>(null)
        private set
    private var lastRequestTime: Long = 0
    private val locUpdateInterval = 3000L // user location updates every 3 seconds.
    val headingDirection: String
        get() {
            val rot = userRotation ?: return "No Magnetometer"
            val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
            val index = round(rot / 45f).toInt() % 8
            val safeIndex = (index + 8) % 8
            return directions[safeIndex]
        }

    /**
     * Collects the flow from getRotationUpdates and updates userRotation.
     */
    fun startRotationTracking() {
        viewModelScope.launch {
            rotationSensor.getRotationUpdates().collect { rotation ->
                val loc = userLocation
                if (loc != null) {
                    val field = GeomagneticField(
                        loc.latitude.toFloat(),
                        loc.longitude.toFloat(),
                        loc.altitude.toFloat(),
                        System.currentTimeMillis()
                    )
                    val adjustedRotation = (rotation + field.declination + 360f) % 360f
                    userRotation = adjustedRotation
                } else {
                    userRotation = rotation
                }
            }
        }
    }

    /**
     * Collects the flow from getLocationUpdates and updates userLocation.
     */
    fun startLocationTracking() {
        viewModelScope.launch {
            locationFinder.getLocationUpdates(locUpdateInterval).collect { location ->
                userLocation = location
            }
        }
    }

    /**
     * Checks if the time since the last request exceeds the 5-minute threshold.
     */
    private fun shouldFetchData(): Boolean {
        val FIVE_MINUTES_MS = 1000
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastRequestTime >= FIVE_MINUTES_MS)
    }

    /**
     * Launches a coroutine to asynchronously retrieve and hold asset data, while tracking UI State.
     */
    fun getAssetData() {
        if (!shouldFetchData()) {
            return
        }
        lastRequestTime = System.currentTimeMillis()
        viewModelScope.launch {
            fisherFinderUiState = FisherFinderUiState.Loading
            assetRepository.fetchServerData() // first try to fetch server data and insert to db
            val listResult = assetRepository.getAllItems() // get data from db
            Log.d("listresult", listResult.toString())
            fisherFinderUiState = FisherFinderUiState.Success(listResult)
        }

    }
    fun selectAsset(name: String) {
        selectedAssetName = name
    }

    /**
     * Processes raw AssetData and returns a NavigationState data object
     */
    fun processAssetData(messages: List<MessageItem>): NavigationState {
        var assetSpeedDisplay = "0.00 Knots"
        Log.d("messages1", messages.toString())
        val assetHistory = messages
            .filter {it.messengerName == selectedAssetName }
            .sortedByDescending { it.unixTime?: 0L }
        Log.d("assethistory", assetHistory.toString())
        Log.d("selected asset", selectedAssetName.toString())
        if (assetHistory.size >= 2) {
            val latest = assetHistory[0]
            val previous = assetHistory[1]
            val currentSpeed = getCurrentSpeed(latest, previous)
            Log.d("currspeed", currentSpeed.toString())
            if (currentSpeed > 0.0) {
                assetSpeedDisplay = "%.2f knots".format(currentSpeed)
            }
        } else {
            assetSpeedDisplay = "Calculating..."
        }
        val latestMessagesPerAsset = assetHistory.distinctBy { it.messengerName }
        val uniqueAssets = messages.mapNotNull { it.messengerName }.distinct().sorted()
        if (selectedAssetName == null && uniqueAssets.isNotEmpty()) {
            selectedAssetName = uniqueAssets.first()
        }
        val selectedMessage = assetHistory.find { it.messengerName == selectedAssetName }
        val time = selectedMessage?.unixTime
        val now = System.currentTimeMillis() / 1000 // current time in seconds
        var myHeading: Float? by mutableStateOf(null)
        var bearingToBuoy = 0f
        val diffMinutes = if (time != null) {
            (now - time) / (60)
        } else {
            Long.MAX_VALUE // If no date, treat as "very old"
        }
        Log.d("diff minutes", diffMinutes.toString())
        val color = getFreshnessColor(diffMinutes)
        val displayName =
            selectedMessage?.messengerName?.substringAfterLast("_") ?: "Select an Asset"
        val position = selectedMessage?.let { "Lat: ${it.latitude},\nLong: ${it.longitude}" }
            ?: "Position unavailable"
        var temaToAsset = 0f
        var assetPosition = Location("Asset")

        var userToAsset = 0f
        val gpsInfo: AnnotatedString = buildAnnotatedString { append("Waiting for GPS...") }
        if (userLocation != null && selectedMessage != null) {
            assetPosition = Location("Asset").apply {
                latitude = selectedMessage.latitude; longitude = selectedMessage.longitude
            }
            val temaPortCoords =
                Location("Tema Harbour").apply { latitude = 5.63438; longitude = 0.01674 }
            userToAsset = userLocation!!.distanceTo(assetPosition) / 1000
            temaToAsset = temaPortCoords.distanceTo(assetPosition) / 1000
            bearingToBuoy = userLocation!!.bearingTo(assetPosition)
            if (userLocation!!.hasSpeed() && userLocation!!.speed > 0.5f) {
                myHeading = userLocation!!.bearing
            }
        }
            return NavigationState(
                allMessages = messages,
                messages = latestMessagesPerAsset,
                selectedAssetName = selectedAssetName,
                displayName = displayName,
                position = position,
                gpsInfo = gpsInfo,
                uniqueAssets = uniqueAssets,
                movingHeading = myHeading,
                formattedDate = selectedMessage?.time ?: "Date not available",
                formattedTime = selectedMessage?.date ?: "Time not available",
                diffMinutes = diffMinutes.toString(),
                userRotation = userRotation,
                bearingToBuoy = bearingToBuoy,
                assetSpeedDisplay = assetSpeedDisplay,
                color = color,
                temaToAsset = temaToAsset,
                userLocation = userLocation,
                userToAsset = userToAsset,
                assetPosition = assetPosition
            )
        }


    companion object {
        val Factory: Factory
            get() = viewModelFactory {
                initializer {
                    val application = (this[APPLICATION_KEY] as FisherFinderApplication)
                    val fisherFinderRepository = application.container.assetDataRepository
                    val locationFinder = application.container.locationFinder
                    val rotationSensor = application.container.rotationSensor
                    FisherFinderViewModel(
                        assetRepository = fisherFinderRepository,
                        locationFinder = locationFinder,
                        rotationSensor = rotationSensor
                    )
                }
            }
    }
}


