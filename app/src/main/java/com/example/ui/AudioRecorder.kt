package com.example.ui

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var audioFile: File? = null

    fun startRecording(): File? {
        audioFile = File(context.cacheDir, "hw_audio_${System.currentTimeMillis()}.m4a")

        return try {
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            recorder = rec
            audioFile
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Recording failed during setup/start", e)
            try {
                recorder?.release()
            } catch (t: Throwable) {}
            recorder = null
            null
        }
    }

    fun stopRecording(): File? {
        try {
            recorder?.stop()
            recorder?.release()
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Stop failed", e)
        } finally {
            recorder = null
        }
        return audioFile
    }

    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                recorder?.pause()
            } catch (e: Exception) {
                Log.e("AudioRecorder", "Pause failed", e)
            }
        }
    }

    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                recorder?.resume()
            } catch (e: Exception) {
                Log.e("AudioRecorder", "Resume failed", e)
            }
        }
    }
}
