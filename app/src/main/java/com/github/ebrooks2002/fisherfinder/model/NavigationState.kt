package com.github.ebrooks2002.fisherfinder.model

import android.location.Location
import androidx.compose.ui.text.AnnotatedString
import com.github.ebrooks2002.fisherfinder.data.MessageItem

data class NavigationState(
    val allMessages: List<MessageItem>,
    val messages : List<MessageItem>,
    val selectedAssetName: String?,
    val displayName: String,
    val position: String,
    val gpsInfo: AnnotatedString,
    val uniqueAssets: List<String>,
    val formattedDate: String,
    val formattedTime: String,
    val diffMinutes: String,
    val movingHeading: Float?,
    val userRotation: Float?,
    val bearingToBuoy: Float,
    val assetSpeedDisplay: String,
    val color: String,
    val temaToAsset: Float,
    val userLocation: Location?,
    val userToAsset: Float,
    val assetPosition: Location
)