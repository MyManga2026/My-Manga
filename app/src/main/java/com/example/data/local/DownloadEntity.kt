package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_chapters")
data class DownloadEntity(
    @PrimaryKey val downloadId: String,
    val mangaId: Int,
    val mangaTitle: String,
    val coverImage: String?,
    val chapterId: String,
    val chapterTitle: String,
    val chapterNumber: String,
    val localPagesJson: String,
    val totalPages: Int,
    val downloadedAt: Long = System.currentTimeMillis()
)
