package com.example.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import com.example.data.model.Track

object SongActions {
    fun share(context: Context, tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val audioUris = tracks.mapNotNull { track ->
            track.path.takeIf { it.startsWith("content://") }?.let(Uri::parse)
        }
        val send = if (audioUris.size == tracks.size && audioUris.isNotEmpty()) {
            if (audioUris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "audio/*"
                    putExtra(Intent.EXTRA_STREAM, audioUris.first())
                    putExtra(Intent.EXTRA_TEXT, "${tracks.first().title} — ${tracks.first().artist}")
                    clipData = ClipData.newUri(context.contentResolver, tracks.first().title, audioUris.first())
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "audio/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(audioUris))
                    val clip = ClipData.newUri(context.contentResolver, "Songs", audioUris.first())
                    audioUris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
                    clipData = clip
                }
            }.apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, tracks.joinToString("\n") { "${it.title} — ${it.artist}" })
            }
        }
        context.startActivity(Intent.createChooser(send, if (tracks.size == 1) "Share song" else "Share songs"))
    }

    /**
     * Deletes what it can immediately and returns the on-device ids still waiting
     * for the system confirmation dialog on Android 11+.
     */
    fun prepareDeviceDelete(
        context: Context,
        tracks: List<Track>,
        launchConsent: (IntentSenderRequest) -> Unit,
        removeNow: (List<String>) -> Unit
    ): List<String> {
        val onDevice = tracks.filter { it.path.startsWith("content://") }
        val libraryOnly = tracks.filter { !it.path.startsWith("content://") }
        if (libraryOnly.isNotEmpty()) removeNow(libraryOnly.map { it.id })
        if (onDevice.isEmpty()) return emptyList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val request = MediaStore.createDeleteRequest(
                context.contentResolver,
                onDevice.map { Uri.parse(it.path) }
            )
            launchConsent(IntentSenderRequest.Builder(request).build())
            return onDevice.map { it.id }
        }
        val deleted = onDevice.mapNotNull { track ->
            val rows = runCatching {
                context.contentResolver.delete(Uri.parse(track.path), null, null)
            }.getOrDefault(0)
            track.id.takeIf { rows > 0 }
        }
        if (deleted.isNotEmpty()) removeNow(deleted)
        return emptyList()
    }
}
