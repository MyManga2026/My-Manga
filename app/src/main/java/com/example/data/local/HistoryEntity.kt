package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_history")
data class HistoryEntity(
    @PrimaryKey val mangaId: Int,
    val title: String,
    val coverImage: String,
    val chapterId: String,
    val chapterTitle: String,
    val pageNumber: Int,
    val totalPages: Int,
    val lastReadAt: Long = System.currentTimeMillis()
)
