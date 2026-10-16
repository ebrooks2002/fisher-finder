package com.github.ebrooks2002.fisherfinder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MessageItem::class], version = 1)
abstract class MessageDatabase: RoomDatabase() {
    abstract fun messageDao(): MessageDao
    companion object {
        @Volatile
        private var Instance: MessageDatabase? = null

        fun getDatabase(context: Context): MessageDatabase {
            return Instance?: synchronized(this) {
                Room.databaseBuilder(
                    context,
                    MessageDatabase::class.java, "message_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also {
                        Instance = it
                    }
            }
        }
    }

}