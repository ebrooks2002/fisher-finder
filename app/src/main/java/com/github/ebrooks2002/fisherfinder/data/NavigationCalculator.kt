package com.github.ebrooks2002.fisherfinder.data

import android.location.Location
import android.util.Log
import com.github.ebrooks2002.fisherfinder.model.Message

fun getCurrentSpeed(latest: MessageItem, previous: MessageItem): Double {
    val time1 = latest.unixTime
    val time2 = previous.unixTime
    val timeDiffSec = time1 - time2

    if (timeDiffSec > 0) {
        val loc1 = Location("PointA").apply {
            latitude = latest.latitude
            longitude = latest.longitude
        }
        val loc2 = Location("PointB").apply {
            latitude = previous.latitude
            longitude = previous.longitude
        }
        val distanceMeters = loc1.distanceTo(loc2)
        val distanceKm = distanceMeters / 1000.0
        val timeHours = timeDiffSec / 60 / 60
        Log.d("hours", timeHours.toString())
        val speedKmh = distanceKm / timeHours
        val speedKnots = speedKmh * 0.539957
        return speedKnots
    }
    return 0.0
}

fun getFreshnessColor(diffMinutes: Long): String {
    return when {
        diffMinutes <= 15 -> "#00A86B"
        diffMinutes <= 30 -> "#ccae16"
        else -> "#FF0000"
    }
}


