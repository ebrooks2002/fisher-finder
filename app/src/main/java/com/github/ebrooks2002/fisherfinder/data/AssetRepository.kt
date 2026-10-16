package com.github.ebrooks2002.fisherfinder.data

import android.util.Log
import com.github.ebrooks2002.fisherfinder.network.SPOTApiService

class AssetRepository(
    val retrofitService: SPOTApiService,
    val databaseManager : DatabaseManager) {

    // try to fetch data from the server and add to our database.
    // return a list of messages from our database.

    suspend fun fetchServerData() {
        val assetData = try {
            retrofitService.getData()
        } catch (e: Exception) {
            null
        }
        Log.d("assetdata", assetData.toString())
       if (assetData != null) {

           val messages = assetData.response.feedMessageResponse.messages?.list
           Log.d("messagesunsorted", messages.toString())
           messages?.sortedBy {it.unixTime}
           Log.d("messagessorted", messages.toString())
           //Log.d("messages",messages.toString())
           messages?.forEach {
               val message = MessageItem(
                   messengerName = it.messengerName,
                   date = it.formattedDate,
                   time = it.formattedTime,
                   unixTime = it.unixTime,
                   latitude = it.latitude,
                   longitude = it.longitude
               )
               insertItem(message)
           }
       }
    }

    suspend fun insertItem(item: MessageItem) {
        databaseManager.insertAndTrim(item)
    }

    suspend fun getAllItems(): List<MessageItem> {
        Log.d("Items", databaseManager.getAllItems().toString())
        return databaseManager.getAllItems()
    }

}