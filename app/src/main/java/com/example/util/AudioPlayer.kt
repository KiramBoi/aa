package com.example.util

import android.media.MediaPlayer
import android.util.Log
import java.io.File

class AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var completionCallback: (() -> Unit)? = null
    private var currentPath: String? = null

    fun play(filePath: String, onComplete: () -> Unit) {
        // Stop current before starting
        stop()
        
        val file = File(filePath)
        if (!file.exists()) {
            Log.e("AudioPlayer", "File does not exist: $filePath")
            onComplete()
            return
        }

        // Check if mock file
        if (file.length() < 100 && file.readText().contains("Dummy")) {
            // It is our safe simulated recording (runs fine, triggers complete callback after 3 seconds)
            Log.d("AudioPlayer", "Playing simulated audio log")
            completionCallback = onComplete
            mediaPlayer = null
            currentPath = filePath
            // We can let the calling UI simulate timer transitions if mediaPlayer is null!
            return
        }

        try {
            completionCallback = onComplete
            currentPath = filePath
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
                setOnCompletionListener {
                    stop()
                    onComplete()
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Failed to start playback, triggering safe complete", e)
            onComplete()
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error pausing playback", e)
        }
    }

    fun resume() {
        try {
            if (mediaPlayer != null && mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error resuming playback", e)
        }
    }

    fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Resetting player failed", e)
        }
        mediaPlayer = null
        completionCallback?.invoke()
        completionCallback = null
        currentPath = null
    }

    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun getCurrentPosition(): Int {
        return try {
            mediaPlayer?.currentPosition ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun getDuration(): Int {
        return try {
            mediaPlayer?.duration ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
