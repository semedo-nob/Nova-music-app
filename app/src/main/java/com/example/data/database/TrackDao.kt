package com.example.data.database

import androidx.room.*
import com.example.data.model.Track
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY title ASC")
    fun getAllTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): Track?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: Track)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<Track>)

    @Update
    suspend fun updateTrack(track: Track)

    @Delete
    suspend fun deleteTrack(track: Track)

    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteTracks(): Flow<List<Track>>

    @Query("UPDATE tracks SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFav: Boolean)

    @Query("UPDATE tracks SET playCount = playCount + 1, lastPlayed = :timestamp WHERE id = :id")
    suspend fun incrementPlayCount(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM tracks ORDER BY lastPlayed DESC LIMIT 50")
    fun getRecentlyPlayedTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks ORDER BY playCount DESC LIMIT 50")
    fun getMostPlayedTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks ORDER BY addedDate DESC")
    fun getRecentlyAddedTracks(): Flow<List<Track>>

    @Query("SELECT DISTINCT genre FROM tracks WHERE genre != '' AND genre != 'Unknown'")
    fun getAllGenres(): Flow<List<String>>

    @Query("SELECT DISTINCT artist FROM tracks WHERE artist != '' AND artist != 'Unknown'")
    fun getAllArtists(): Flow<List<String>>

    @Query("SELECT DISTINCT album FROM tracks WHERE album != '' AND album != 'Unknown'")
    fun getAllAlbums(): Flow<List<String>>

    @Query("SELECT * FROM tracks WHERE artist = :artist ORDER BY title ASC")
    fun getTracksByArtist(artist: String): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE album = :album ORDER BY title ASC")
    fun getTracksByAlbum(album: String): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE genre = :genre ORDER BY title ASC")
    fun getTracksByGenre(genre: String): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%'")
    fun searchTracks(query: String): Flow<List<Track>>

    @Query("DELETE FROM tracks WHERE isLocal = 1 AND path NOT LIKE 'content://%'")
    suspend fun deleteUnplayableLocalFileTracks()

    @Query("DELETE FROM tracks WHERE id LIKE 'nova_%' OR path LIKE '%soundhelix.com%'")
    suspend fun deletePlaceholderTracks()

    @Query("DELETE FROM tracks WHERE id IN (:ids)")
    suspend fun deleteTracksByIds(ids: List<String>)
}
