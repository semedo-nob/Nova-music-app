package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.ai.GeminiManager
import com.example.ai.NovaMusicAssistant
import com.example.data.database.AppDatabase
import com.example.data.online.OnlineMusicService
import com.example.data.repository.MusicRepository
import com.example.playback.PlaybackManager
import com.example.playback.recognition.AcousticAudioRecorder
import com.example.playback.recognition.SongRecognitionService

object ServiceLocator {
    private var database: AppDatabase? = null
    private var repository: MusicRepository? = null
    private var playbackManager: PlaybackManager? = null
    private var geminiManager: GeminiManager? = null
    private var authSessionManager: com.example.data.auth.AuthSessionManager? = null
    private var songRecognitionService: SongRecognitionService? = null
    private var acousticAudioRecorder: AcousticAudioRecorder? = null
    private var onlineMusicService: OnlineMusicService? = null
    private var novaMusicAssistant: NovaMusicAssistant? = null

    fun getDatabase(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "nova_music_db"
            ).fallbackToDestructiveMigration().build()
            database = db
            db
        }
    }

    fun getRepository(context: Context): MusicRepository {
        return repository ?: synchronized(this) {
            val repo = MusicRepository(
                context.applicationContext,
                getDatabase(context).trackDao(),
                getDatabase(context).playlistDao(),
                getDatabase(context).recognizedTrackDao()
            )
            repository = repo
            repo
        }
    }

    fun getPlaybackManager(context: Context): PlaybackManager {
        return playbackManager ?: synchronized(this) {
            val pm = PlaybackManager(
                context.applicationContext,
                getRepository(context)
            )
            playbackManager = pm
            pm
        }
    }

    fun getGeminiManager(context: Context): GeminiManager {
        return geminiManager ?: synchronized(this) {
            val gm = GeminiManager(context.applicationContext)
            geminiManager = gm
            gm
        }
    }

    fun getAuthSessionManager(context: Context): com.example.data.auth.AuthSessionManager {
        return authSessionManager ?: synchronized(this) {
            val asm = com.example.data.auth.AuthSessionManager(context.applicationContext)
            authSessionManager = asm
            asm
        }
    }

    fun getSongRecognitionService(context: Context): SongRecognitionService {
        return songRecognitionService ?: synchronized(this) {
            val service = SongRecognitionService(context.applicationContext)
            songRecognitionService = service
            service
        }
    }

    fun getAcousticAudioRecorder(): AcousticAudioRecorder {
        return acousticAudioRecorder ?: synchronized(this) {
            val recorder = AcousticAudioRecorder()
            acousticAudioRecorder = recorder
            recorder
        }
    }

    fun getOnlineMusicService(): OnlineMusicService {
        return onlineMusicService ?: synchronized(this) {
            val service = OnlineMusicService()
            onlineMusicService = service
            service
        }
    }

    fun getNovaMusicAssistant(context: Context): NovaMusicAssistant {
        return novaMusicAssistant ?: synchronized(this) {
            val assistant = NovaMusicAssistant(
                context.applicationContext,
                getRepository(context),
                getPlaybackManager(context),
                getGeminiManager(context),
                getOnlineMusicService()
            )
            novaMusicAssistant = assistant
            assistant
        }
    }
}
