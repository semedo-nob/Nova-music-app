package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey val id: String, // filePath for local, URL or custom ID for streams
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long, // in milliseconds
    val path: String,
    val genre: String = "Unknown",
    val size: Long = 0L,
    val isFavorite: Boolean = false,
    val addedDate: Long = System.currentTimeMillis(),
    val playCount: Int = 0,
    val lastPlayed: Long = 0L,
    val isLocal: Boolean = true,
    val artworkUrl: String? = null // Optional URL or local album artURI
) : Serializable
