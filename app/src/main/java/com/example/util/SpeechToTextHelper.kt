package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class SpeechToTextHelper(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onPartial: (String) -> Unit = {}
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var speechRecognizerIntent: Intent? = null

    init {
        try {
            // Android speech service must be called on main thread or has custom handles
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            Log.d("SpeechToText", "Device ready for voice")
                        }

                        override fun onBeginningOfSpeech() {
                            Log.d("SpeechToText", "Began speech")
                        }

                        override fun onRmsChanged(rmsdB: Float) {}

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            Log.d("SpeechToText", "Ended speech")
                        }

                        override fun onError(error: Int) {
                            val message = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Audio capturing error"
                                SpeechRecognizer.ERROR_CLIENT -> "Local client error"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions required"
                                SpeechRecognizer.ERROR_NETWORK -> "Network issue"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out"
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech matched"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recording service busy"
                                SpeechRecognizer.ERROR_SERVER -> "Server protocol error"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timed out"
                                else -> "Voice capture unavailable: $error"
                            }
                            Log.e("SpeechToText", "Recognizer error: $message")
                            onError(message)
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                onResult(matches[0])
                            } else {
                                onError("Speech recognizer returned no text results")
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                onPartial(matches[0])
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
                
                speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
            } else {
                Log.w("SpeechToText", "Local Speech Recognition is unavailable")
            }
        } catch (e: Exception) {
            Log.e("SpeechToText", "Failed initializing SpeechRecognizer service", e)
        }
    }

    fun startListening() {
        if (speechRecognizer != null && speechRecognizerIntent != null) {
            try {
                speechRecognizer?.startListening(speechRecognizerIntent)
            } catch (e: Exception) {
                Log.e("SpeechToText", "Error during startListening", e)
                onError("Voice engine start error: " + e.localizedMessage)
            }
        } else {
            onError("System Speech recognition unavailable on this device. Use our dreamy quick-dictation below!")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("SpeechToText", "Error stopping listening", e)
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("SpeechToText", "Error destroying speech recognizer", e)
        }
        speechRecognizer = null
    }
}
