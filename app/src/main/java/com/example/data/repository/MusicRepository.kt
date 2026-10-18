package com.example.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.data.database.PlaylistDao
import com.example.data.database.TrackDao
import com.example.data.model.Playlist
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID

class MusicRepository(
    private val context: Context,
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
    private val recognizedTrackDao: com.example.data.database.RecognizedTrackDao
) {
    // Flows exposed to the UI
    val allTracks: Flow<List<Track>> = trackDao.getAllTracks()
    val recognizedTracks: Flow<List<com.example.data.model.RecognizedTrack>> = recognizedTrackDao.getAllRecognizedTracks()
    val favoriteTracks: Flow<List<Track>> = trackDao.getFavoriteTracks()
    val recentlyPlayed: Flow<List<Track>> = trackDao.getRecentlyPlayedTracks()
    val mostPlayed: Flow<List<Track>> = trackDao.getMostPlayedTracks()
    val recentlyAdded: Flow<List<Track>> = trackDao.getRecentlyAddedTracks()
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()
    val allGenres: Flow<List<String>> = trackDao.getAllGenres()
    val allArtists: Flow<List<String>> = trackDao.getAllArtists()
    val allAlbums: Flow<List<String>> = trackDao.getAllAlbums()

    // Map of smart playlists built reactively or calculated
    fun getTracksInPlaylist(playlistId: String): Flow<List<Track>> {
        return when (playlistId) {
            "smart_favorites" -> favoriteTracks
            "smart_recently_added" -> recentlyAdded
            "smart_recently_played" -> recentlyPlayed
            "smart_most_played" -> mostPlayed
            "smart_never_played" -> {
                // Filter out where playCount == 0 (we can customize or use standard filter)
                trackDao.getAllTracks() // We can filter this in ViewModel
            }
            else -> playlistDao.getTracksInPlaylist(playlistId)
        }
    }

    suspend fun getTrackById(id: String): Track? = trackDao.getTrackById(id)

    suspend fun toggleFavorite(trackId: String) = withContext(Dispatchers.IO) {
        val track = trackDao.getTrackById(trackId)
        if (track != null) {
            trackDao.updateFavoriteStatus(trackId, !track.isFavorite)
        }
    }

    suspend fun recordPlay(trackId: String) = withContext(Dispatchers.IO) {
        trackDao.incrementPlayCount(trackId)
    }

    suspend fun checkAndPrepopulateOriginals() = withContext(Dispatchers.IO) {
        trackDao.deletePlaceholderTracks()
        val smartPlaylists = listOf(
            Playlist("smart_favorites", "Liked Songs", "Your absolute favorites", isSmart = true, smartType = "LIKED"),
            Playlist("smart_recently_played", "Recently Played", "Songs you listened to lately", isSmart = true, smartType = "RECENT_PLAYED"),
            Playlist("smart_most_played", "Most Played", "Your top heavy-rotation beats", isSmart = true, smartType = "MOST_PLAYED"),
            Playlist("smart_recently_added", "Recently Added", "Newly imported or discovered tracks", isSmart = true, smartType = "RECENT_ADDED"),
            Playlist("smart_never_played", "Never Played", "Hidden gems you haven't opened yet", isSmart = true, smartType = "NEVER_PLAYED")
        )
        for (playlist in smartPlaylists) {
            if (playlistDao.getPlaylistById(playlist.id) == null) {
                playlistDao.insertPlaylist(playlist)
            }
        }
    }

    // Scan Android Local Storage for music files
    suspend fun scanLocalStorage(): Int = withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DISPLAY_NAME
        )

        // Only query files that are music
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        var count = 0

        try {
            val cursor = resolver.query(uri, projection, selection, null, null)
            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

                val tracksToInsert = mutableListOf<Track>()

                while (c.moveToNext()) {
                    val mediaId = c.getLong(idCol)
                    val displayName = c.getString(nameCol).orEmpty()
                    if (displayName.startsWith(".")) continue

                    // content:// URIs stay readable under scoped storage. Raw file paths do not.
                    val contentUri = ContentUris.withAppendedId(uri, mediaId).toString()
                    val title = c.getString(titleCol)?.takeIf { it.isNotBlank() }
                        ?: displayName.substringBeforeLast('.')
                    val artist = c.getString(artistCol)?.takeIf { it.isNotBlank() } ?: "Unknown Artist"
                    val album = c.getString(albumCol)?.takeIf { it.isNotBlank() } ?: "Unknown Album"
                    val duration = c.getLong(durationCol)
                    val size = c.getLong(sizeCol)

                    val track = Track(
                        id = contentUri,
                        title = title.ifBlank { "Unknown Title" },
                        artist = artist,
                        album = album,
                        duration = if (duration > 0) duration else 180000L,
                        path = contentUri,
                        genre = "Unknown",
                        size = size,
                        isLocal = true,
                        artworkUrl = null
                    )
                    tracksToInsert.add(track)
                }

                if (tracksToInsert.isNotEmpty()) {
                    trackDao.insertTracks(tracksToInsert)
                    trackDao.deleteUnplayableLocalFileTracks()
                    count = tracksToInsert.size
                }
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error scanning local media store", e)
        }

        return@withContext count
    }

    // Playlist CRUD operations
    suspend fun createPlaylist(name: String, description: String = ""): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val playlist = Playlist(
            id = id,
            name = name,
            description = description,
            isSmart = false
        )
        playlistDao.insertPlaylist(playlist)
        return@withContext id
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        val p = playlistDao.getPlaylistById(playlistId)
        if (p != null && !p.isSmart) {
            playlistDao.deletePlaylist(p)
        }
    }

    suspend fun addTrackToPlaylist(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        playlistDao.addTrackToPlaylist(playlistId, trackId)
    }

    suspend fun addTracksToPlaylist(playlistId: String, trackIds: List<String>) = withContext(Dispatchers.IO) {
        trackIds.forEach { trackId -> playlistDao.addTrackToPlaylist(playlistId, trackId) }
    }

    suspend fun deleteTracks(ids: List<String>) = withContext(Dispatchers.IO) {
        if (ids.isNotEmpty()) trackDao.deleteTracksByIds(ids)
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun duplicatePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        val source = playlistDao.getPlaylistById(playlistId) ?: return@withContext
        val newId = createPlaylist("${source.name} (Copy)", source.description)
        playlistDao.getTracksInPlaylist(playlistId).firstOrNull()?.forEach { track ->
            playlistDao.addTrackToPlaylist(newId, track.id)
        }
    }

    suspend fun reorderPlaylistTracks(playlistId: String, tracks: List<Track>) = withContext(Dispatchers.IO) {
        tracks.forEachIndexed { index, track ->
            playlistDao.updateTrackOrder(playlistId, track.id, index)
        }
    }

    fun getTracksByArtist(artist: String): Flow<List<Track>> = trackDao.getTracksByArtist(artist)
    fun getTracksByAlbum(album: String): Flow<List<Track>> = trackDao.getTracksByAlbum(album)
    fun getTracksByGenre(genre: String): Flow<List<Track>> = trackDao.getTracksByGenre(genre)
    fun searchTracks(query: String): Flow<List<Track>> = trackDao.searchTracks(query)

    suspend fun saveRecognizedTrack(track: com.example.data.model.RecognizedTrack) = withContext(Dispatchers.IO) {
        recognizedTrackDao.insertRecognizedTrack(track)
    }

    suspend fun deleteRecognizedTrack(track: com.example.data.model.RecognizedTrack) = withContext(Dispatchers.IO) {
        recognizedTrackDao.deleteRecognizedTrack(track)
    }

    suspend fun clearRecognizedHistory() = withContext(Dispatchers.IO) {
        recognizedTrackDao.clearAll()
    }
}
