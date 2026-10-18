package com.example.playback

import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.di.ServiceLocator

class NovaMediaPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        try {
            val playbackManager = ServiceLocator.getPlaybackManager(applicationContext)
            mediaSession = playbackManager.getOrCreateMediaSession(this)
            mediaSession?.let { addSession(it) }
            Log.d("NovaMediaPlaybackService", "MediaSessionService created with session: ${mediaSession?.id}")
        } catch (e: Exception) {
            Log.e("NovaMediaPlaybackService", "Error initializing MediaSessionService", e)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        Log.d("NovaMediaPlaybackService", "MediaSessionService onDestroy")
        // Drop the session binding only. The player stays alive in PlaybackManager.
        ServiceLocator.getPlaybackManager(applicationContext).detachMediaSession()
        mediaSession = null
        super.onDestroy()
    }
}
