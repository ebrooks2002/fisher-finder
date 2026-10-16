package com.github.ebrooks2002.fisherfinder.data

class DatabaseManager(val messageDao: MessageDao) {

    suspend fun getAllItems(): List<MessageItem> {
        return messageDao.getAllItems()
    }

    suspend fun insertAndTrim(item: MessageItem){
        messageDao.insertItemAndTrim(item)
    }

}