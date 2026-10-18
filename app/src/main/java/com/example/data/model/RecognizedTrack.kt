package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "recognized_tracks")
data class RecognizedTrack(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val releaseDate: String? = null,
    val artworkUrl: String? = null,
    val previewUrl: String? = null,
    val songLink: String? = null,
    val recognizedAt: Long = System.currentTimeMillis(),
    val source: String = "Acoustic Recognition"
) : Serializable
