package com.example.data.database

import androidx.room.*
import com.example.data.model.RecognizedTrack
import kotlinx.coroutines.flow.Flow

@Dao
interface RecognizedTrackDao {
    @Query("SELECT * FROM recognized_tracks ORDER BY recognizedAt DESC")
    fun getAllRecognizedTracks(): Flow<List<RecognizedTrack>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecognizedTrack(track: RecognizedTrack)

    @Delete
    suspend fun deleteRecognizedTrack(track: RecognizedTrack)

    @Query("DELETE FROM recognized_tracks")
    suspend fun clearAll()
}
