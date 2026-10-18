package com.example.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.model.Playlist
import com.example.data.model.PlaylistTrackCrossRef
import com.example.data.model.RecognizedTrack
import com.example.data.model.Track

@Database(
    entities = [Track::class, Playlist::class, PlaylistTrackCrossRef::class, RecognizedTrack::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun recognizedTrackDao(): RecognizedTrackDao
}
