package com.example.playback

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.cos
import kotlin.math.sin

/**
 * Deck B of the mixer. Deck A is the main player. The crossfader is equal-power:
 * full left is deck A, full right is deck B, and the center keeps both audible.
 * Deck B does not take audio focus, so both decks can play together.
 */
class DjMixer(context: Context) {
    private val appContext = context.applicationContext
    private var player: ExoPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    private val _deckBTrack = MutableStateFlow<Track?>(null)
    val deckBTrack: StateFlow<Track?> = _deckBTrack.asStateFlow()

    private val _deckBPlaying = MutableStateFlow(false)
    val deckBPlaying: StateFlow<Boolean> = _deckBPlaying.asStateFlow()

    private val _deckBPosition = MutableStateFlow(0L)
    val deckBPosition: StateFlow<Long> = _deckBPosition.asStateFlow()

    private val _deckBDuration = MutableStateFlow(0L)
    val deckBDuration: StateFlow<Long> = _deckBDuration.asStateFlow()

    private val _crossfader = MutableStateFlow(0f)
    val crossfader: StateFlow<Float> = _crossfader.asStateFlow()

    private val _deckAGain = MutableStateFlow(1f)
    val deckAGain: StateFlow<Float> = _deckAGain.asStateFlow()

    private val _deckBGain = MutableStateFlow(1f)
    val deckBGain: StateFlow<Float> = _deckBGain.asStateFlow()

    private val progress = object : Runnable {
        override fun run() {
            player?.let { deck ->
                _deckBPosition.value = deck.currentPosition.coerceAtLeast(0L)
                if (deck.isPlaying) handler.postDelayed(this, 250L)
            }
        }
    }

    fun load(track: Track) {
        val deck = player()
        val item = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(Uri.parse(track.path))
            .build()
        deck.setMediaItem(item)
        deck.prepare()
        deck.playWhenReady = true
        deck.play()
        _deckBTrack.value = track
        applyDeckBVolume()
    }

    fun toggleDeckB() {
        val deck = player ?: return
        if (deck.isPlaying) deck.pause() else deck.play()
    }

    fun cueDeckB() {
        player?.seekTo(0L)
        _deckBPosition.value = 0L
    }

    fun setCrossfader(position: Float) {
        _crossfader.value = position.coerceIn(0f, 1f)
        applyDeckBVolume()
    }

    fun setDeckAGain(gain: Float) {
        _deckAGain.value = gain.coerceIn(0f, 1f)
    }

    fun setDeckBGain(gain: Float) {
        _deckBGain.value = gain.coerceIn(0f, 1f)
        applyDeckBVolume()
    }

    fun deckAVolume(): Float = equalPower(left = true)

    fun stopDeckB() {
        handler.removeCallbacks(progress)
        player?.let { deck ->
            deck.pause()
            deck.clearMediaItems()
        }
        _deckBTrack.value = null
        _deckBPlaying.value = false
        _deckBPosition.value = 0L
        _deckBDuration.value = 0L
        _crossfader.value = 0f
        _deckAGain.value = 1f
        _deckBGain.value = 1f
    }

    fun release() {
        stopDeckB()
        player?.release()
        player = null
    }

    private fun applyDeckBVolume() {
        player?.volume = equalPower(left = false)
    }

    private fun equalPower(left: Boolean): Float {
        val angle = _crossfader.value * (Math.PI / 2.0)
        val curve = if (left) cos(angle) else sin(angle)
        val gain = if (left) _deckAGain.value else _deckBGain.value
        return (curve * gain).toFloat().coerceIn(0f, 1f)
    }

    private fun player(): ExoPlayer {
        player?.let { return it }
        val attributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        val deck = ExoPlayer.Builder(appContext)
            .setAudioAttributes(attributes, false)
            .setHandleAudioBecomingNoisy(false)
            .build()
        deck.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _deckBPlaying.value = isPlaying
                if (isPlaying) handler.post(progress) else handler.removeCallbacks(progress)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _deckBDuration.value = deck.duration.coerceAtLeast(0L)
                }
            }
        })
        player = deck
        return deck
    }
}
