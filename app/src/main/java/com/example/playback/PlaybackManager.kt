package com.example.playback

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.example.MainActivity
import com.example.data.model.Track
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

@OptIn(UnstableApi::class)
class PlaybackManager(
    private val context: Context,
    private val repository: MusicRepository
) {
    private val mainScope = CoroutineScope(Dispatchers.Main + Job())
    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    // Audio effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    // UI States
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF) // 0 = OFF, 1 = ONE, 2 = ALL
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private var outputVolume = 1f
    private var crossfadeEnabled = false
    private var fadeRunnable: Runnable? = null

    // Original unshuffled queue to restore order when disabling shuffle
    private var originalQueue: List<Track> = emptyList()

    // Progress update runnable
    private val handler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    _currentPosition.value = player.currentPosition
                    _duration.value = player.duration.coerceAtLeast(0L)
                }
            }
            handler.postDelayed(this, 250)
        }
    }

    // Sleep Timer
    private var sleepTimerJob: Job? = null
    private val _sleepTimeRemaining = MutableStateFlow<Long?>(null) // null = off, otherwise ms remaining
    val sleepTimeRemaining: StateFlow<Long?> = _sleepTimeRemaining.asStateFlow()

    // Equalizer Preset names and configs
    val eqPresets = listOf("Custom", "Flat", "Rock", "Pop", "Jazz", "Dance", "Hip Hop", "Classical", "Electronic")
    private val _eqPreset = MutableStateFlow("Flat")
    val eqPreset: StateFlow<String> = _eqPreset.asStateFlow()

    private val _bassLevel = MutableStateFlow(0) // 0 to 1000
    val bassLevel: StateFlow<Int> = _bassLevel.asStateFlow()

    private val _virtualizerLevel = MutableStateFlow(0) // 0 to 1000
    val virtualizerLevel: StateFlow<Int> = _virtualizerLevel.asStateFlow()

    private val _eqBands = MutableStateFlow<List<Int>>(emptyList()) // current gains of bands
    val eqBands: StateFlow<List<Int>> = _eqBands.asStateFlow()

    init {
        mainScope.launch {
            setupExoPlayer()
        }
    }

    private fun setupExoPlayer(): ExoPlayer {
        val existing = exoPlayer
        if (existing != null) return existing

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true) // Handles Audio Focus automatically!
            .setHandleAudioBecomingNoisy(true) // Pauses automatically when headphones disconnected!
            // NETWORK keeps the CPU and Wi-Fi awake. LOCAL lets the radio sleep and the stream dies.
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    _playbackError.value = null
                    lastFailedIndex = -1
                    handler.post(progressRunnable)
                    startPlaybackService()
                } else {
                    handler.removeCallbacks(progressRunnable)
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        _duration.value = player.duration.coerceAtLeast(0L)
                        _playbackError.value = null
                    }
                    Player.STATE_ENDED -> {
                        _isPlaying.value = false
                    }
                    Player.STATE_BUFFERING -> {
                        _duration.value = player.duration.coerceAtLeast(0L)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("PlaybackManager", "Playback failed: ${error.errorCodeName}", error)
                releaseAudioEffects()
                val failedIndex = player.currentMediaItemIndex
                if (failedIndex != lastFailedIndex) {
                    // One retry covers a stream that stalled when the radio slept.
                    lastFailedIndex = failedIndex
                    player.prepare()
                    player.play()
                    return
                }
                if (player.hasNextMediaItem()) {
                    _playbackError.value = "Skipped a track that wouldn't play"
                    player.seekToNextMediaItem()
                    player.prepare()
                    player.play()
                } else {
                    _isPlaying.value = false
                    _playbackError.value = playbackErrorMessage(error)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = player.currentMediaItemIndex
                if (index in queue.value.indices) {
                    _currentIndex.value = index
                    val track = queue.value[index]
                    _currentTrack.value = track
                    mainScope.launch {
                        repository.recordPlay(track.id)
                    }
                    if (crossfadeEnabled && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                        fadeIn()
                    }
                }
            }
        })

        exoPlayer = player
        return player
    }

    private var sessionOwner: Context? = null
    private var lastFailedIndex = -1

    fun getOrCreateMediaSession(serviceContext: Context): MediaSession {
        val existing = mediaSession
        if (existing != null && sessionOwner === serviceContext) {
            return existing
        }
        // A recreated service cannot reuse a session bound to the dead instance.
        // Releasing the session leaves the player running.
        existing?.release()
        mediaSession = null

        val player = exoPlayer ?: setupExoPlayer()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val session = MediaSession.Builder(serviceContext, player)
            .setId("nova_music")
            .setSessionActivity(pendingIntent)
            .build()

        mediaSession = session
        sessionOwner = serviceContext
        return session
    }

    fun detachMediaSession() {
        mediaSession?.release()
        mediaSession = null
        sessionOwner = null
    }

    fun startPlaybackService() {
        val player = exoPlayer
        if (player == null || player.mediaItemCount == 0 || !player.playWhenReady) return
        try {
            val serviceIntent = Intent(context, NovaMediaPlaybackService::class.java)
            // The service promotes itself to the foreground once the player is already playing.
            context.startService(serviceIntent)
        } catch (e: Exception) {
            Log.w("PlaybackManager", "Could not start NovaMediaPlaybackService", e)
        }
    }

    private fun trackToMediaItem(track: Track): MediaItem {
        val uri = resolvePlaybackUri(track.path)

        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)

        track.artworkUrl?.let { art ->
            try {
                val artUri = if (art.startsWith("/") && !art.startsWith("content://") && !art.startsWith("http")) {
                    Uri.fromFile(File(art))
                } else {
                    Uri.parse(art)
                }
                metadataBuilder.setArtworkUri(artUri)
            } catch (e: Exception) {
                Log.w("PlaybackManager", "Failed to parse artworkUri: $art", e)
            }
        }

        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(uri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun resolvePlaybackUri(path: String): Uri {
        if (path.startsWith("content://") || path.startsWith("http://") || path.startsWith("https://")) {
            return Uri.parse(path)
        }
        if (path.startsWith("/")) {
            contentUriForFilePath(path)?.let { return it }
            return Uri.fromFile(File(path))
        }
        return Uri.parse(path)
    }

    private fun contentUriForFilePath(path: String): Uri? {
        return try {
            val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            context.contentResolver.query(
                collection,
                arrayOf(MediaStore.Audio.Media._ID),
                "${MediaStore.Audio.Media.DATA} = ?",
                arrayOf(path),
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                ContentUris.withAppendedId(collection, id)
            }
        } catch (e: Exception) {
            Log.w("PlaybackManager", "MediaStore lookup failed for $path", e)
            null
        }
    }

    private var activeAudioSessionId: Int? = null

    private fun releaseAudioEffects() {
        try {
            equalizer?.release()
        } catch (e: Exception) {
            Log.w("PlaybackManager", "Error releasing equalizer", e)
        }
        try {
            bassBoost?.release()
        } catch (e: Exception) {
            Log.w("PlaybackManager", "Error releasing bass boost", e)
        }
        try {
            virtualizer?.release()
        } catch (e: Exception) {
            Log.w("PlaybackManager", "Error releasing virtualizer", e)
        }
        equalizer = null
        bassBoost = null
        virtualizer = null
        activeAudioSessionId = null
    }

    private fun ensureAudioEffects(): Boolean {
        val sessionId = exoPlayer?.audioSessionId ?: return false
        if (sessionId == 0 || sessionId == C.AUDIO_SESSION_ID_UNSET) return false
        if (equalizer == null || activeAudioSessionId != sessionId) {
            setupAudioEffects(sessionId)
        }
        return equalizer != null || bassBoost != null || virtualizer != null
    }

    private fun setupAudioEffects(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
        if (activeAudioSessionId == audioSessionId && equalizer != null) return
        releaseAudioEffects()
        activeAudioSessionId = audioSessionId
        try {
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
            equalizer?.let { eq ->
                val bands = mutableListOf<Int>()
                for (i in 0 until eq.numberOfBands.toInt()) {
                    bands.add(eq.getBandLevel(i.toShort()).toInt())
                }
                _eqBands.value = bands
            }
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Equalizer unavailable; continuing without it", e)
            equalizer = null
        }
        try {
            bassBoost = BassBoost(0, audioSessionId).apply { enabled = true }
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Bass boost unavailable", e)
            bassBoost = null
        }
        try {
            virtualizer = Virtualizer(0, audioSessionId).apply { enabled = true }
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Virtualizer unavailable", e)
            virtualizer = null
        }
    }

    private fun playbackErrorMessage(error: PlaybackException): String {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE ->
                "This file can't be opened. Scan the library again."
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                "The stream stopped. Check your connection and try again."
            else -> "Playback stopped: ${error.errorCodeName}"
        }
    }

    // Set play queue and start playing from specific index
    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        originalQueue = tracks.toList()
        _queue.value = tracks

        val index = startIndex.coerceIn(0, tracks.lastIndex)
        _currentIndex.value = index
        _currentTrack.value = tracks[index]
        _playbackError.value = null
        lastFailedIndex = -1

        try {
            releaseAudioEffects()
            val player = exoPlayer ?: setupExoPlayer()
            // Replace the playlist in one step. stop() forces playWhenReady off and the
            // session service treats that idle gap as "nothing to play" and shuts down.
            player.setMediaItems(tracks.map { trackToMediaItem(it) }, index, 0L)
            player.repeatMode = repeatMode.value
            player.shuffleModeEnabled = false
            player.setPlaybackSpeed(playbackSpeed.value)
            player.volume = outputVolume
            player.prepare()
            player.play()
            startPlaybackService()
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Couldn't start playback", e)
            _isPlaying.value = false
            _playbackError.value = e.message ?: "Couldn't start playback"
        }
    }

    fun play() {
        val player = exoPlayer ?: setupExoPlayer()
        player.volume = outputVolume
        player.play()
        startPlaybackService()
    }

    fun setOutputVolume(volume: Float) {
        outputVolume = volume.coerceIn(0f, 1f)
        exoPlayer?.volume = outputVolume
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        crossfadeEnabled = enabled
    }

    private fun fadeIn() {
        fadeRunnable?.let { handler.removeCallbacks(it) }
        val steps = 10
        var step = 0
        val job = object : Runnable {
            override fun run() {
                val deck = exoPlayer ?: return
                step++
                deck.volume = outputVolume * (step / steps.toFloat())
                if (step < steps) handler.postDelayed(this, 45L) else deck.volume = outputVolume
            }
        }
        fadeRunnable = job
        exoPlayer?.volume = 0f
        handler.post(job)
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun next() {
        exoPlayer?.let { player ->
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
            } else {
                if (repeatMode.value == Player.REPEAT_MODE_ALL && queue.value.isNotEmpty()) {
                    player.seekTo(0, 0L)
                }
            }
        }
    }

    fun previous() {
        exoPlayer?.let { player ->
            if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
            } else {
                player.seekTo(0L)
            }
        }
    }

    // Queue management functions
    fun removeTracks(ids: Set<String>) {
        if (ids.isEmpty()) return
        val old = _queue.value
        if (old.none { it.id in ids }) return
        val updated = old.filter { it.id !in ids }
        if (updated.isEmpty()) {
            clearQueue()
            return
        }
        val playingId = _currentTrack.value?.id
        val removeIndices = old.mapIndexedNotNull { index, track ->
            index.takeIf { track.id in ids }
        }.sortedDescending()
        _queue.value = updated
        originalQueue = originalQueue.filter { it.id !in ids }
        removeIndices.forEach { index -> exoPlayer?.removeMediaItem(index) }
        if (playingId != null && playingId !in ids) {
            _currentIndex.value = updated.indexOfFirst { it.id == playingId }.coerceAtLeast(0)
        }
    }

    fun addToQueue(track: Track) {
        val currentQueue = _queue.value.toMutableList()
        currentQueue.add(track)
        _queue.value = currentQueue
        originalQueue = originalQueue + track
        exoPlayer?.addMediaItem(trackToMediaItem(track))
    }

    fun playNext(track: Track) {
        val player = exoPlayer ?: setupExoPlayer()
        val currentIdx = _currentIndex.value
        val insertIndex = (currentIdx + 1).coerceAtMost(_queue.value.size)
        val currentQueue = _queue.value.toMutableList()
        currentQueue.add(insertIndex, track)
        _queue.value = currentQueue
        player.addMediaItem(insertIndex, trackToMediaItem(track))
    }

    fun removeFromQueue(index: Int) {
        if (index !in _queue.value.indices) return
        val currentQueue = _queue.value.toMutableList()
        val removed = currentQueue.removeAt(index)
        _queue.value = currentQueue
        originalQueue = originalQueue.filter { it.id != removed.id }
        exoPlayer?.removeMediaItem(index)
        if (index < _currentIndex.value) {
            _currentIndex.value = (_currentIndex.value - 1).coerceAtLeast(0)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in _queue.value.indices || toIndex !in _queue.value.indices) return
        val currentQueue = _queue.value.toMutableList()
        val item = currentQueue.removeAt(fromIndex)
        currentQueue.add(toIndex, item)
        _queue.value = currentQueue
        exoPlayer?.moveMediaItem(fromIndex, toIndex)
        val currentIdx = _currentIndex.value
        if (currentIdx == fromIndex) {
            _currentIndex.value = toIndex
        } else if (fromIndex < currentIdx && toIndex >= currentIdx) {
            _currentIndex.value = currentIdx - 1
        } else if (fromIndex > currentIdx && toIndex <= currentIdx) {
            _currentIndex.value = currentIdx + 1
        }
    }

    fun clearQueue() {
        exoPlayer?.let { player ->
            releaseAudioEffects()
            player.stop()
            player.clearMediaItems()
        }
        _queue.value = emptyList()
        originalQueue = emptyList()
        _currentIndex.value = -1
        _currentTrack.value = null
        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
    }

    fun toggleShuffle() {
        val enabled = !shuffleEnabled.value
        _shuffleEnabled.value = enabled
        // Shuffle is applied by reordering the queue. Leaving ExoPlayer's own shuffle
        // on top of that jumps away from the current song.

        if (enabled) {
            val current = currentTrack.value
            val currentIdx = currentIndex.value
            val listToShuffle = queue.value.toMutableList()
            if (current != null && currentIdx in listToShuffle.indices) {
                listToShuffle.removeAt(currentIdx)
                listToShuffle.shuffle()
                listToShuffle.add(0, current)
                _queue.value = listToShuffle
                _currentIndex.value = 0

                exoPlayer?.let { player ->
                    val progress = player.currentPosition
                    val wasPlaying = player.playWhenReady
                    releaseAudioEffects()
                    player.setMediaItems(listToShuffle.map { trackToMediaItem(it) }, 0, progress)
                    player.shuffleModeEnabled = false
                    player.prepare()
                    player.playWhenReady = wasPlaying
                }
            }
        } else {
            val current = currentTrack.value
            val originalIndex = originalQueue.indexOf(current)
            _queue.value = originalQueue
            _currentIndex.value = if (originalIndex != -1) originalIndex else 0

            exoPlayer?.let { player ->
                val progress = player.currentPosition
                val idx = if (originalIndex != -1) originalIndex else 0
                val wasPlaying = player.playWhenReady
                releaseAudioEffects()
                player.setMediaItems(originalQueue.map { trackToMediaItem(it) }, idx, progress)
                player.shuffleModeEnabled = false
                player.prepare()
                player.playWhenReady = wasPlaying
            }
        }
    }

    fun toggleRepeatMode() {
        val nextMode = when (repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = nextMode
        exoPlayer?.repeatMode = nextMode
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer?.setPlaybackSpeed(speed)
    }

    // Equalizer presets configuration
    fun selectEqPreset(presetName: String) {
        _eqPreset.value = presetName
        if (!ensureAudioEffects()) return
        val eq = equalizer ?: return
        try {
            when (presetName) {
                "Flat" -> {
                    for (i in 0 until eq.numberOfBands.toInt()) {
                        eq.setBandLevel(i.toShort(), 0)
                    }
                }
                "Rock" -> {
                    val gains = listOf(4, 3, -1, 3, 5)
                    applyEqGains(gains)
                }
                "Pop" -> {
                    val gains = listOf(-2, -1, 3, 2, -1)
                    applyEqGains(gains)
                }
                "Jazz" -> {
                    val gains = listOf(3, 2, -2, 2, 4)
                    applyEqGains(gains)
                }
                "Dance" -> {
                    val gains = listOf(5, 0, 2, 4, 1)
                    applyEqGains(gains)
                }
                "Hip Hop" -> {
                    val gains = listOf(5, 3, 0, 1, 3)
                    applyEqGains(gains)
                }
                "Classical" -> {
                    val gains = listOf(3, 2, -1, 2, 3)
                    applyEqGains(gains)
                }
                "Electronic" -> {
                    val gains = listOf(4, 2, -1, 2, 4)
                    applyEqGains(gains)
                }
                "Bass Boost" -> applyEqGains(listOf(5, 3, 0, -1, -1))
                "Cyberpunk" -> applyEqGains(listOf(4, -1, 1, 4, 5))
                "Vocal" -> applyEqGains(listOf(-2, 1, 4, 2, -1))
            }
            val bands = mutableListOf<Int>()
            for (i in 0 until eq.numberOfBands.toInt()) {
                bands.add(eq.getBandLevel(i.toShort()).toInt())
            }
            _eqBands.value = bands
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Error applying preset: $presetName", e)
        }
    }

    private fun applyEqGains(gains: List<Int>) {
        val eq = equalizer ?: return
        for (i in 0 until eq.numberOfBands.toInt().coerceAtMost(gains.size)) {
            val bandRange = eq.bandLevelRange
            val min = bandRange[0]
            val max = bandRange[1]
            val value = (gains[i] * 100).toShort().coerceIn(min, max)
            eq.setBandLevel(i.toShort(), value)
        }
    }

    fun updateEqBand(bandId: Short, levelMillibels: Short) {
        if (!ensureAudioEffects()) return
        val eq = equalizer ?: return
        try {
            val bandRange = eq.bandLevelRange
            val safeLevel = levelMillibels.coerceIn(bandRange[0], bandRange[1])
            eq.setBandLevel(bandId, safeLevel)
            _eqPreset.value = "Custom"

            val bands = _eqBands.value.toMutableList()
            if (bandId.toInt() in bands.indices) {
                bands[bandId.toInt()] = safeLevel.toInt()
                _eqBands.value = bands
            }
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Error updating EQ band $bandId", e)
        }
    }

    fun setBassBoost(level: Int) {
        _bassLevel.value = level
        if (!ensureAudioEffects()) return
        try {
            bassBoost?.let { boost ->
                if (boost.strengthSupported) {
                    boost.setStrength(level.toShort())
                }
            }
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Error updating bass boost strength", e)
        }
    }

    fun setVirtualizer(level: Int) {
        _virtualizerLevel.value = level
        if (!ensureAudioEffects()) return
        try {
            virtualizer?.let { virt ->
                if (virt.strengthSupported) {
                    virt.setStrength(level.toShort())
                }
            }
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Error updating virtualizer strength", e)
        }
    }

    // Sleep Timer actions
    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimeRemaining.value = null
            return
        }

        _sleepTimeRemaining.value = minutes * 60 * 1000L
        sleepTimerJob = mainScope.launch {
            var remaining = minutes * 60 * 1000L
            while (remaining > 0) {
                delay(1000L)
                remaining -= 1000L
                _sleepTimeRemaining.value = remaining
            }
            pause()
            _sleepTimeRemaining.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimeRemaining.value = null
    }

    fun updateTrackFavoriteStatus(trackId: String, isFavorite: Boolean) {
        val current = _currentTrack.value
        if (current != null && current.id == trackId) {
            _currentTrack.value = current.copy(isFavorite = isFavorite)
        }
        _queue.value = _queue.value.map {
            if (it.id == trackId) it.copy(isFavorite = isFavorite) else it
        }
        originalQueue = originalQueue.map {
            if (it.id == trackId) it.copy(isFavorite = isFavorite) else it
        }
    }

    fun release() {
        handler.removeCallbacks(progressRunnable)
        sleepTimerJob?.cancel()
        releaseAudioEffects()
        mediaSession?.release()
        mediaSession = null
        sessionOwner = null
        exoPlayer?.release()
        exoPlayer = null
    }
}
