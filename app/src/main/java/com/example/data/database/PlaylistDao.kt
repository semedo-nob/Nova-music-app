package com.example.data.database

import androidx.room.*
import com.example.data.model.Playlist
import com.example.data.model.PlaylistTrackCrossRef
import com.example.data.model.Track
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: String): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(playlistTrack: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)

    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: String)

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_track_cross_ref r ON t.id = r.trackId
        WHERE r.playlistId = :playlistId
        ORDER BY r.orderIndex ASC
    """)
    fun getTracksInPlaylist(playlistId: String): Flow<List<Track>>

    @Query("SELECT MAX(orderIndex) FROM playlist_track_cross_ref WHERE playlistId = :playlistId")
    suspend fun getMaxOrderIndex(playlistId: String): Int?

    @Transaction
    suspend fun addTrackToPlaylist(playlistId: String, trackId: String) {
        val maxIndex = getMaxOrderIndex(playlistId) ?: -1
        insertPlaylistTrack(PlaylistTrackCrossRef(playlistId, trackId, maxIndex + 1))
    }

    @Query("UPDATE playlist_track_cross_ref SET orderIndex = :newIndex WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun updateTrackOrder(playlistId: String, trackId: String, newIndex: Int)
}
