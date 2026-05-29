package com.example.ui

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import java.io.File

class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun playFile(file: File, onCompletion: () -> Unit) {
        stop() // Stop anything currently playing
        try {
            val player = MediaPlayer.create(context, Uri.fromFile(file))
            if (player != null) {
                mediaPlayer = player
                player.setOnCompletionListener { 
                    onCompletion() 
                }
                player.start()
            } else {
                Log.w("AudioPlayer", "MediaPlayer.create returned null")
                onCompletion()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Playback failed", e)
            onCompletion()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
            // Safe catch
        }
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Safe catch
        }
        mediaPlayer = null
    }
}
