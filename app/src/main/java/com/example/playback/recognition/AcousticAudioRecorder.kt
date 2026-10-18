package com.example.playback.recognition

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import kotlin.math.abs
import kotlin.math.max

class AcousticAudioRecorder {

    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isRecording = false

    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    @SuppressLint("MissingPermission")
    suspend fun recordAudioSample(
        durationMs: Long = 5000L,
        onAmplitude: (Float) -> Unit
    ): ByteArray? = withContext(Dispatchers.IO) {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufferSize <= 0) {
            Log.e("AcousticAudioRecorder", "Invalid buffer size: $minBufferSize")
            return@withContext null
        }

        val bufferSize = max(minBufferSize, 4096)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AcousticAudioRecorder", "AudioRecord initialization failed")
                audioRecord?.release()
                audioRecord = null
                return@withContext null
            }

            audioRecord?.startRecording()
            isRecording = true

            val pcmOutputStream = ByteArrayOutputStream()
            val buffer = ShortArray(bufferSize / 2)
            val startTime = System.currentTimeMillis()

            while (isRecording && (System.currentTimeMillis() - startTime < durationMs)) {
                val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readCount > 0) {
                    var maxAmplitude = 0
                    for (i in 0 until readCount) {
                        val sample = buffer[i]
                        val absVal = abs(sample.toInt())
                        if (absVal > maxAmplitude) {
                            maxAmplitude = absVal
                        }
                        // Write 16-bit PCM (little endian)
                        pcmOutputStream.write((sample.toInt() and 0xFF))
                        pcmOutputStream.write(((sample.toInt() shr 8) and 0xFF))
                    }

                    // Normalize amplitude to 0.0 .. 1.0
                    val normalized = (maxAmplitude / 32767.0f).coerceIn(0.0f, 1.0f)
                    withContext(Dispatchers.Main) {
                        onAmplitude(normalized)
                    }
                }
            }

            stop()
            val pcmData = pcmOutputStream.toByteArray()
            if (pcmData.isEmpty()) return@withContext null

            // Wrap PCM data with a 44-byte standard RIFF/WAVE header
            return@withContext createWavHeader(pcmData, SAMPLE_RATE, 1, 16)
        } catch (e: Exception) {
            Log.e("AcousticAudioRecorder", "Error during microphone recording", e)
            stop()
            return@withContext null
        }
    }

    fun stop() {
        isRecording = false
        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.w("AcousticAudioRecorder", "Error stopping recorder", e)
        } finally {
            audioRecord = null
        }
    }

    private fun createWavHeader(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray {
        val totalAudioLen = pcmData.size.toLong()
        val totalDataLen = totalAudioLen + 36
        val byteRate = (sampleRate * channels * bitsPerSample / 8).toLong()

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 16 for PCM
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // PCM format = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte() // block align
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        return header + pcmData
    }
}
