package com.github.ebrooks2002.fisherfinder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("messageItems")
data class MessageItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val messengerName: String,
    val date: String,
    val time: String,
    val unixTime: Long,
    val latitude: Double,
    val longitude: Double
)


