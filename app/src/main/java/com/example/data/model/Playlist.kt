package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey val id: String, // UUID or preset ID for smart playlists
    val name: String,
    val description: String = "",
    val isSmart: Boolean = false,
    val smartType: String? = null, // "LIKED", "NEVER_PLAYED", "MOST_PLAYED", "RECENT_ADDED", "RECENT_PLAYED"
    val createdDate: Long = System.currentTimeMillis(),
    val artworkUrl: String? = null
)
