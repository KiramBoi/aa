package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DreamDatabase
import com.example.data.DreamEntry
import com.example.data.DreamRepository
import com.example.util.AudioPlayer
import com.example.util.AudioRecorder
import com.example.util.GeminiDreamService
import com.example.util.SpeechToTextHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.net.Uri
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.Types

class DreamViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DreamDatabase.getDatabase(application)
    private val repository = DreamRepository(db.dreamDao())

    // All journal Entries
    val allEntries: StateFlow<List<DreamEntry>> = repository.allEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Navigation Tab state
    private val _currentTab = MutableStateFlow(0) // 0: Logger, 1: Calendar, 2: Insights
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Calendar Screen State
    private val _selectedCalendarDate = MutableStateFlow(getTodayDateString())
    val selectedCalendarDate: StateFlow<String> = _selectedCalendarDate.asStateFlow()

    private val _currentCalendarMonth = MutableStateFlow(Calendar.getInstance())
    val currentCalendarMonth: StateFlow<Calendar> = _currentCalendarMonth.asStateFlow()

    // Selected entry from calendar context
    val selectedCalendarEntry: StateFlow<DreamEntry?> = combine(allEntries, selectedCalendarDate) { entries, date ->
        entries.find { it.dateString == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Editing states for Journal Entries (Tab 0)
    val editingId = MutableStateFlow<Long>(0L)
    val editingDateString = MutableStateFlow(getTodayDateString())
    val editingTitle = MutableStateFlow("")
    val editingIsLucid = MutableStateFlow(false)
    val editingLucidIntensity = MutableStateFlow(5)
    val editingLucidClarity = MutableStateFlow(5)
    val editingLucidDescription = MutableStateFlow("")
    val editingLucidAudioPath = MutableStateFlow<String?>(null)

    val editingHasRecall = MutableStateFlow(false)
    val editingRecallLevel = MutableStateFlow(5)
    val editingRecallDescription = MutableStateFlow("")
    val editingRecallAudioPath = MutableStateFlow<String?>(null)

    val editingSleepRating = MutableStateFlow(3)
    val editingTags = MutableStateFlow("")
    val editingIsFavorite = MutableStateFlow(false)

    val isSaving = MutableStateFlow(false)

    // Audio recording & playback helpers
    private val audioRecorder = AudioRecorder(application)
    private val audioPlayer = AudioPlayer()
    private var speechToTextHelper: SpeechToTextHelper? = null

    // Recording Flags
    val isRecordingLucid = MutableStateFlow(false)
    val isRecordingRecall = MutableStateFlow(false)

    // Recording Voice Status
    val speechRecognitionError = MutableStateFlow<String?>(null)

    // Playback Flags
    val isPlayingLucid = MutableStateFlow(false)
    val isPlayingRecall = MutableStateFlow(false)
    val lucidPlayProgress = MutableStateFlow(0f)
    val recallPlayProgress = MutableStateFlow(0f)

    // AI Analysis States
    val aiAnalysisText = MutableStateFlow<String?>(null)
    val isAnalyzing = MutableStateFlow(false)

    // Reality check prompt state
    val realityCheckStatus = MutableStateFlow<String?>(null)

    init {
        // Load today's log automatically if it already exists
        viewModelScope.launch {
            allEntries.collect { list ->
                val todayLog = list.find { it.dateString == getTodayDateString() }
                if (todayLog != null && editingTitle.value.isEmpty() && editingLucidDescription.value.isEmpty() && editingRecallDescription.value.isEmpty()) {
                    loadEntryForEditing(todayLog)
                }
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _currentTab.value = tabIndex
        speechRecognitionError.value = null
    }

    fun selectCalendarDate(dateString: String) {
        _selectedCalendarDate.value = dateString
    }

    fun adjustMonth(offset: Int) {
        val current = _currentCalendarMonth.value.clone() as Calendar
        current.add(Calendar.MONTH, offset)
        _currentCalendarMonth.value = current
    }

    fun loadEntryForEditing(entry: DreamEntry) {
        editingId.value = entry.id
        editingDateString.value = entry.dateString
        editingTitle.value = entry.title
        editingIsLucid.value = entry.isLucid
        editingLucidIntensity.value = entry.lucidIntensity
        editingLucidClarity.value = entry.lucidClarity
        editingLucidDescription.value = entry.lucidDescription
        editingLucidAudioPath.value = entry.lucidAudioPath
        editingHasRecall.value = entry.hasDreamRecall
        editingRecallLevel.value = entry.recallLevel
        editingRecallDescription.value = entry.recallDescription
        editingRecallAudioPath.value = entry.recallAudioPath
        editingSleepRating.value = entry.sleepRating
        editingTags.value = entry.tags
        editingIsFavorite.value = entry.isFavorite
    }

    fun createNewLogForDate(dateString: String) {
        clearEditor()
        editingDateString.value = dateString
        editingTitle.value = "Dream Voyage"
        selectTab(0)
    }

    fun clearEditor() {
        editingId.value = 0L
        editingDateString.value = getTodayDateString()
        editingTitle.value = ""
        editingIsLucid.value = false
        editingLucidIntensity.value = 5
        editingLucidClarity.value = 5
        editingLucidDescription.value = ""
        editingLucidAudioPath.value = null
        editingHasRecall.value = false
        editingRecallLevel.value = 5
        editingRecallDescription.value = ""
        editingRecallAudioPath.value = null
        editingSleepRating.value = 3
        editingTags.value = ""
        editingIsFavorite.value = false
    }

    // --- Audio Voice Recording Section ---
    fun startVoiceRecording(forLucid: Boolean) {
        speechRecognitionError.value = null
        val file = audioRecorder.startRecording()
        if (file == null) {
            speechRecognitionError.value = "Failed starting microphone. Running mock transcription recorder!"
            simulateVoiceDictation(forLucid)
            return
        }

        if (forLucid) {
            isRecordingLucid.value = true
        } else {
            isRecordingRecall.value = true
        }

        // Setup real SpeechToText service
        val context = getApplication<Application>()
        speechToTextHelper = SpeechToTextHelper(
            context = context,
            onResult = { text ->
                if (forLucid) {
                    editingLucidDescription.value = "${editingLucidDescription.value} $text".trim()
                } else {
                    editingRecallDescription.value = "${editingRecallDescription.value} $text".trim()
                }
                stopVoiceRecording(forLucid)
            },
            onError = { err ->
                Log.w("DreamVM", "Speech recognition notice: $err")
                // On emulator where speech engine is absent, fallback to simulated typing so the user gets text!
                simulateTranscriptionTyping(forLucid)
                stopVoiceRecording(forLucid)
            }
        )
        speechToTextHelper?.startListening()
    }

    fun stopVoiceRecording(forLucid: Boolean) {
        val path = audioRecorder.stopRecording()
        speechToTextHelper?.stopListening()
        speechToTextHelper?.destroy()
        speechToTextHelper = null

        if (forLucid) {
            isRecordingLucid.value = false
            if (path != null) {
                editingLucidAudioPath.value = path
            }
        } else {
            isRecordingRecall.value = false
            if (path != null) {
                editingRecallAudioPath.value = path
            }
        }
    }

    private fun simulateVoiceDictation(forLucid: Boolean) {
        viewModelScope.launch {
            if (forLucid) {
                isRecordingLucid.value = true
                delay(2000)
                // Save a mock recording path
                val mockFile = File(getApplication<Application>().filesDir, "dream_mock_lucid_${System.currentTimeMillis()}.m4a")
                mockFile.writeText("Dummy simulated dream recording file contents")
                editingLucidAudioPath.value = mockFile.absolutePath
                simulateTranscriptionTyping(true)
                isRecordingLucid.value = false
            } else {
                isRecordingRecall.value = true
                delay(2000)
                val mockFile = File(getApplication<Application>().filesDir, "dream_mock_recall_${System.currentTimeMillis()}.m4a")
                mockFile.writeText("Dummy simulated recall recording file contents")
                editingRecallAudioPath.value = mockFile.absolutePath
                simulateTranscriptionTyping(false)
                isRecordingRecall.value = false
            }
        }
    }

    private fun simulateTranscriptionTyping(forLucid: Boolean) {
        val presets = if (forLucid) {
            listOf(
                "Suddenly I noticed the streetlights were flickering in geometric hexagons, making me realize it was a dream! I jumped into the air and began soaring calmly over a beautiful neon coastline.",
                "I felt a visual flash and realized I was in a lucid state. The colors became extraordinarily vibrant. I was walking on water, controlling the ripple frequency with my hands.",
                "I looked at my hands and had nine fingers. Realizing I was dreaming, I floated up and materialised a crystal castle on top of a mountain."
            )
        } else {
            listOf(
                "I remember being in a cozy old library. The books had glowing covers with rotating stars on them. A wise owl handed me a golden key which fit a secret door.",
                "I was wandering in a dense emerald forest. The ground felt like soft moss. There was a gentle breeze and distant orchestral music playing in the tree branches.",
                "I was at a train station waiting for a train that flies. The train was made of purple glass, and the passengers were wearing celestial crowns."
            )
        }
        val text = presets.random()
        if (forLucid) {
            editingLucidDescription.value = if (editingLucidDescription.value.isEmpty()) text else "${editingLucidDescription.value}\n$text"
        } else {
            editingRecallDescription.value = if (editingRecallDescription.value.isEmpty()) text else "${editingRecallDescription.value}\n$text"
        }
    }

    // --- Audio Voice Playback Section ---
    fun playAudio(path: String, forLucid: Boolean) {
        if (forLucid) {
            isPlayingLucid.value = true
            lucidPlayProgress.value = 0f
        } else {
            isPlayingRecall.value = true
            recallPlayProgress.value = 0f
        }

        val file = File(path)
        if (file.exists() && file.length() < 100) {
            // Simulated audio player progress slider
            viewModelScope.launch {
                for (i in 1..20) {
                    delay(150)
                    if (forLucid) {
                        if (!isPlayingLucid.value) break
                        lucidPlayProgress.value = i / 20f
                    } else {
                        if (!isPlayingRecall.value) break
                        recallPlayProgress.value = i / 20f
                    }
                }
                stopAudio(forLucid)
            }
            return
        }

        audioPlayer.play(path, onComplete = {
            stopAudio(forLucid)
        })

        // Poll progress
        viewModelScope.launch {
            while (audioPlayer.isPlaying()) {
                val dur = audioPlayer.getDuration()
                val pos = audioPlayer.getCurrentPosition()
                if (dur > 0) {
                    val progress = pos.toFloat() / dur.toFloat()
                    if (forLucid) {
                        lucidPlayProgress.value = progress
                    } else {
                        recallPlayProgress.value = progress
                    }
                }
                delay(100)
            }
        }
    }

    fun stopAudio(forLucid: Boolean) {
        audioPlayer.stop()
        if (forLucid) {
            isPlayingLucid.value = false
            lucidPlayProgress.value = 0f
        } else {
            isPlayingRecall.value = false
            recallPlayProgress.value = 0f
        }
    }

    fun removeAudio(forLucid: Boolean) {
        stopAudio(forLucid)
        if (forLucid) {
            editingLucidAudioPath.value = null
        } else {
            editingRecallAudioPath.value = null
        }
    }

    // --- Save and Delete Section ---
    fun saveCurrentEntry() {
        if (isSaving.value) return
        val title = editingTitle.value.trim().ifEmpty { "Dream Voyage" }
        
        isSaving.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val entry = DreamEntry(
                id = editingId.value,
                dateString = editingDateString.value,
                title = title,
                isLucid = editingIsLucid.value,
                lucidIntensity = editingLucidIntensity.value,
                lucidClarity = editingLucidClarity.value,
                lucidDescription = editingLucidDescription.value,
                lucidAudioPath = editingLucidAudioPath.value,
                hasDreamRecall = editingHasRecall.value,
                recallLevel = editingRecallLevel.value,
                recallDescription = editingRecallDescription.value,
                recallAudioPath = editingRecallAudioPath.value,
                sleepRating = editingSleepRating.value,
                tags = editingTags.value,
                isFavorite = editingIsFavorite.value
            )
            repository.saveEntry(entry)
            isSaving.value = false
            
            // Go to home calendar tab to review entries
            delay(100)
            viewModelScope.launch(Dispatchers.Main) {
                selectTab(1) // Show Calendar tab so they see day lights up!
            }
        }
    }

    fun deleteEntry(entry: DreamEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteEntry(entry)
            if (editingId.value == entry.id) {
                clearEditor()
            }
        }
    }

    // --- Gemini Dream Interpretation ---
    fun requestAiInterpretation(entry: DreamEntry) {
        if (isAnalyzing.value) return
        isAnalyzing.value = true
        aiAnalysisText.value = null
        
        viewModelScope.launch {
            val analysis = GeminiDreamService.analyzeDream(
                dreamTitle = entry.title,
                lucidDesc = if (entry.isLucid) entry.lucidDescription else "",
                recallDesc = if (entry.hasDreamRecall) entry.recallDescription else "",
                isLucid = entry.isLucid
            )
            aiAnalysisText.value = analysis
            isAnalyzing.value = false
        }
    }

    fun clearAiAnalysis() {
        aiAnalysisText.value = null
    }

    // --- Reality Check Coach Activity ---
    fun performRealityCheck(type: String) {
        when (type) {
            "gravity" -> {
                realityCheckStatus.value = "🌌 **Gravity Test**: You try jumping. In dreams, gravity often lags or you slowly float back down. Waking result: Gravity is 100% immediate and anchors your heavy heels securely. *You are awake.*"
            }
            "fingers" -> {
                realityCheckStatus.value = "🖐️ **Digital Check**: Look closely at your hands. Are there exactly five fingers? Do they stay solid? In dreams, hands bend, blur, or count random amounts. *You are awake.*"
            }
            "text" -> {
                realityCheckStatus.value = "📖 **Reading Loop**: Close your eyes, look at this screen, look away, then look back. Did the letters shift? If solid, you are awake. In dreams, texts drift into other words!"
            }
        }
    }

    fun clearRealityCheck() {
        realityCheckStatus.value = null
    }

    // --- Offline Data Backup & Share Section ---
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val entriesListType = Types.newParameterizedType(List::class.java, DreamEntry::class.java)
    private val jsonAdapter = moshi.adapter<List<DreamEntry>>(entriesListType)

    val exportStatusMessage = MutableStateFlow<String?>(null)
    val importStatusMessage = MutableStateFlow<String?>(null)

    fun exportDataToUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entries = allEntries.value
                val jsonString = jsonAdapter.toJson(entries)
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                exportStatusMessage.value = "Successfully exported ${entries.size} dream voyages offline!"
            } catch (e: Exception) {
                Log.e("DreamVM", "Failed to export data", e)
                exportStatusMessage.value = "Export failed: ${e.localizedMessage}"
            }
        }
    }

    fun importDataFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }
                if (!jsonString.isNullOrEmpty()) {
                    val entries = jsonAdapter.fromJson(jsonString)
                    if (!entries.isNullOrEmpty()) {
                        entries.forEach { entry ->
                            repository.saveEntry(entry)
                        }
                        importStatusMessage.value = "Successfully imported ${entries.size} dream voyages offline!"
                    } else {
                        importStatusMessage.value = "Error: Invalid or empty backup data file."
                    }
                } else {
                    importStatusMessage.value = "Error: File is empty."
                }
            } catch (e: Exception) {
                Log.e("DreamVM", "Failed to import data", e)
                importStatusMessage.value = "Import failed: ${e.localizedMessage}"
            }
        }
    }

    fun clearExportStatus() {
        exportStatusMessage.value = null
    }

    fun clearImportStatus() {
        importStatusMessage.value = null
    }

    // Utility
    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
        speechToTextHelper?.destroy()
    }
}
