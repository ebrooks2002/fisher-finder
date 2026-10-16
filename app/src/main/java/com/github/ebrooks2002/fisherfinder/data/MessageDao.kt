package com.github.ebrooks2002.fisherfinder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MessageDao {

    @Query("""
        DELETE FROM messageItems 
        WHERE id NOT IN (
            SELECT id FROM messageItems 
            ORDER BY unixTime DESC 
            LIMIT 500
        )
    """)
    suspend fun deleteExcessMessages()

    @Transaction
    suspend fun insertItemAndTrim(item: MessageItem) {
        deleteExcessMessages()
        insertItem(item)
    }

    @Query("SELECT * FROM messageItems ORDER BY unixTime DESC")
    suspend fun getAllItems(): List<MessageItem>

    @Insert
    suspend fun insertItem(item: MessageItem)

}