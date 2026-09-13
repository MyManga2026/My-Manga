package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val mangaId: Int,
    val title: String,
    val coverImage: String,
    val genres: String, // Comma separated
    val status: String,
    val score: Int,
    val addedAt: Long = System.currentTimeMillis()
)
