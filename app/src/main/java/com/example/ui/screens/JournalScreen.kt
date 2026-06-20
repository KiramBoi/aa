package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.DreamViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalScreen(viewModel: DreamViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Observe ViewModel editing states
    val editingId by viewModel.editingId.collectAsState()
    val dateString by viewModel.editingDateString.collectAsState()
    val title by viewModel.editingTitle.collectAsState()
    
    val isLucid by viewModel.editingIsLucid.collectAsState()
    val lucidIntensity by viewModel.editingLucidIntensity.collectAsState()
    val lucidClarity by viewModel.editingLucidClarity.collectAsState()
    val lucidDescription by viewModel.editingLucidDescription.collectAsState()
    val lucidAudioPath by viewModel.editingLucidAudioPath.collectAsState()

    val hasRecall by viewModel.editingHasRecall.collectAsState()
    val recallLevel by viewModel.editingRecallLevel.collectAsState()
    val recallDescription by viewModel.editingRecallDescription.collectAsState()
    val recallAudioPath by viewModel.editingRecallAudioPath.collectAsState()

    val sleepRating by viewModel.editingSleepRating.collectAsState()
    val tags by viewModel.editingTags.collectAsState()
    val isFavorite by viewModel.editingIsFavorite.collectAsState()
    
    val isSaving by viewModel.isSaving.collectAsState()

    val isRecordingLucid by viewModel.isRecordingLucid.collectAsState()
    val isRecordingRecall by viewModel.isRecordingRecall.collectAsState()
    
    val isPlayingLucid by viewModel.isPlayingLucid.collectAsState()
    val isPlayingRecall by viewModel.isPlayingRecall.collectAsState()
    val lucidPlayProgress by viewModel.lucidPlayProgress.collectAsState()
    val recallPlayProgress by viewModel.recallPlayProgress.collectAsState()

    val voiceError by viewModel.speechRecognitionError.collectAsState()

    // Audio permission requester
    var targetRecordingForLucid by remember { mutableStateOf(true) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.startVoiceRecording(targetRecordingForLucid)
            } else {
                Toast.makeText(context, "Microphone permission is required for voice logs.", Toast.LENGTH_SHORT).show()
                // Fallback to simulated dictation
                viewModel.startVoiceRecording(targetRecordingForLucid)
            }
        }
    )

    fun startRecordingWithPermission(forLucid: Boolean) {
        targetRecordingForLucid = forLucid
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.startVoiceRecording(forLucid)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Show toast on speech recognition errors/alerts
    LaunchedEffect(voiceError) {
        voiceError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    val isNewEntry = editingId == 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 80.dp) // space for bottom navigation bar
    ) {
        // App / Screen Header
        Text(
            text = if (isNewEntry) "Record Subconscious Voyage" else "Edit Subconscious Voyage",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = CosmicPrimary
            ),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Date Focus: $dateString",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray.copy(alpha = 0.8f)),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // General Voyage Information Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("general_info_card"),
            colors = CardDefaults.cardColors(containerColor = CosmicCardBg),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Core Metrics",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CosmicSecondary
                    ),
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { viewModel.editingTitle.value = it },
                    label = { Text("Voyage Title") },
                    placeholder = { Text("e.g. Floating in the starry clocktower") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CosmicPrimary,
                        unfocusedBorderColor = BorderColor,
                        focusedLabelColor = CosmicPrimary,
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = CosmicPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("dream_title_input")
                )

                // Sleep Star Rating Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sleep Quality:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Row {
                        for (star in 1..5) {
                            Icon(
                                imageVector = if (star <= sleepRating) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = "Star $star",
                                tint = if (star <= sleepRating) StarActive else Color.Gray,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { viewModel.editingSleepRating.value = star }
                                    .testTag("sleep_rating_star_$star")
                            )
                        }
                    }
                }

                // Tags Input
                OutlinedTextField(
                    value = tags,
                    onValueChange = { viewModel.editingTags.value = it },
                    label = { Text("Dream Tag Keywords (comma separated)") },
                    placeholder = { Text("flying, space, oceanic, neon, owl") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = "Tags", tint = CosmicSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CosmicSecondary,
                        unfocusedBorderColor = BorderColor,
                        unfocusedLabelColor = Color.Gray,
                        focusedLabelColor = CosmicSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }

        // Checklist 1: Lucid Dreaming Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicCardBg),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header Row with Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isLucid,
                        onCheckedChange = { viewModel.editingIsLucid.value = it },
                        colors = CheckboxDefaults.colors(checkedColor = LucidTeal),
                        modifier = Modifier.testTag("lucid_dream_checkbox")
                    )
                    Column(modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.editingIsLucid.value = !isLucid }
                    ) {
                        Text(
                            text = "Had Lucid Dream",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isLucid) LucidTeal else Color.LightGray
                            )
                        )
                        Text(
                            text = "Achieved active waking awareness in sleep",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                    }
                    if (isLucid) {
                        Icon(
                            imageVector = Icons.Default.Brightness4,
                            contentDescription = "Lucid Active",
                            tint = LucidTeal,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Expandable sliders & description box if checked
                AnimatedVisibility(
                    visible = isLucid,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 12.dp))

                        // Lucid Intensity Slider
                        Text(
                            text = "Intensity level: $lucidIntensity / 10",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Slider(
                            value = lucidIntensity.toFloat(),
                            onValueChange = { viewModel.editingLucidIntensity.value = it.toInt() },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = LucidTeal,
                                activeTrackColor = LucidTeal,
                                inactiveTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Lucid Clarity Slider
                        Text(
                            text = "Clarity level: $lucidClarity / 10",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Slider(
                            value = lucidClarity.toFloat(),
                            onValueChange = { viewModel.editingLucidClarity.value = it.toInt() },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = LucidTeal,
                                activeTrackColor = LucidTeal,
                                inactiveTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Lucid Description Box with Voice to Text Integration
                        OutlinedTextField(
                            value = lucidDescription,
                            onValueChange = { viewModel.editingLucidDescription.value = it },
                            label = { Text("Lucid Actions & Control Logs") },
                            placeholder = { Text("What did you do? Did you fly, spin, test gravity?") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("lucid_description_input"),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LucidTeal,
                                unfocusedBorderColor = BorderColor,
                                unfocusedLabelColor = Color.Gray,
                                focusedLabelColor = LucidTeal
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Voice and Playback block
                        VoiceInputBar(
                            isRecording = isRecordingLucid,
                            audioPath = lucidAudioPath,
                            isPlaying = isPlayingLucid,
                            playProgress = lucidPlayProgress,
                            onRecordToggle = {
                                if (isRecordingLucid) {
                                    viewModel.stopVoiceRecording(true)
                                } else {
                                    startRecordingWithPermission(true)
                                }
                            },
                            onPlayToggle = {
                                if (isPlayingLucid) {
                                    viewModel.stopAudio(true)
                                } else {
                                    lucidAudioPath?.let { viewModel.playAudio(it, true) }
                                }
                            },
                            onDeleteAudio = {
                                viewModel.removeAudio(true)
                            },
                            onSimulateDictation = {
                                viewModel.editingLucidDescription.value = "I was floating mid-air overlooking a brilliant purple valley. I rubbed my hands together and stabilized the scene, creating amazing water waves with my thoughts!"
                            }
                        )
                    }
                }
            }
        }

        // Checklist 2: Dream Recall Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicCardBg),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header Row with Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasRecall,
                        onCheckedChange = { viewModel.editingHasRecall.value = it },
                        colors = CheckboxDefaults.colors(checkedColor = RecallGold),
                        modifier = Modifier.testTag("recall_dream_checkbox")
                    )
                    Column(modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.editingHasRecall.value = !hasRecall }
                    ) {
                        Text(
                            text = "Remembered Dreams",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (hasRecall) RecallGold else Color.LightGray
                            )
                        )
                        Text(
                            text = "Non-lucid dreams recalled with detailed patterns",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                    }
                    if (hasRecall) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Recall Active",
                            tint = RecallGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Expandable sliders & description box if checked
                AnimatedVisibility(
                    visible = hasRecall,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 12.dp))

                        // Recall Strength Slider
                        Text(
                            text = "Vibrancy & Detail Recall Level: $recallLevel / 10",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Slider(
                            value = recallLevel.toFloat(),
                            onValueChange = { viewModel.editingRecallLevel.value = it.toInt() },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = RecallGold,
                                activeTrackColor = RecallGold,
                                inactiveTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Recall Description Box
                        OutlinedTextField(
                            value = recallDescription,
                            onValueChange = { viewModel.editingRecallDescription.value = it },
                            label = { Text("Dream Recall Description") },
                            placeholder = { Text("Where were you? Describe characters, items, environment details...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("recall_description_input"),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RecallGold,
                                unfocusedBorderColor = BorderColor,
                                unfocusedLabelColor = Color.Gray,
                                focusedLabelColor = RecallGold
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Voice and Playback block
                        VoiceInputBar(
                            isRecording = isRecordingRecall,
                            audioPath = recallAudioPath,
                            isPlaying = isPlayingRecall,
                            playProgress = recallPlayProgress,
                            onRecordToggle = {
                                if (isRecordingRecall) {
                                    viewModel.stopVoiceRecording(false)
                                } else {
                                    startRecordingWithPermission(false)
                                }
                            },
                            onPlayToggle = {
                                if (isPlayingRecall) {
                                    viewModel.stopAudio(false)
                                } else {
                                    recallAudioPath?.let { viewModel.playAudio(it, false) }
                                }
                            },
                            onDeleteAudio = {
                                viewModel.removeAudio(false)
                            },
                            onSimulateDictation = {
                                viewModel.editingRecallDescription.value = "I was at a whimsical antique market with glowing lamps. A shopkeeper was selling crystal flasks that contained swirling stardust. It played soft chimes upon touching."
                            }
                        )
                    }
                }
            }
        }

        // Submit & Cancel Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.clearEditor() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp, brush = Brush.linearGradient(listOf(Color.Gray, Color.DarkGray))),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Editor", fontSize = 14.sp)
            }

            Button(
                onClick = { viewModel.saveCurrentEntry() },
                enabled = !isSaving && (isLucid || hasRecall),
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .testTag("save_dream_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicPrimary,
                    contentColor = CosmicBackground
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CosmicBackground)
                } else {
                    Icon(Icons.Default.Check, contentDescription = "Save", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Seal Dream Scroll", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun VoiceInputBar(
    isRecording: Boolean,
    audioPath: String?,
    isPlaying: Boolean,
    playProgress: Float,
    onRecordToggle: () -> Unit,
    onPlayToggle: () -> Unit,
    onDeleteAudio: () -> Unit,
    onSimulateDictation: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Record Button
                Button(
                    onClick = onRecordToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) CosmicAccent else Color.DarkGray,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = "Voice dictation",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRecording) "Recording..." else "Voice to Text",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Short helper button for virtual emulator environments
                TextButton(
                    onClick = onSimulateDictation,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Simulate", tint = CosmicSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dictation Assist", fontSize = 11.sp, color = CosmicSecondary)
                }
            }

            // Audio Preservation Status Bar
            if (audioPath != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = Color.Gray.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPlayToggle,
                        modifier = Modifier
                            .size(36.dp)
                            .background(CosmicSecondary, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play voice log",
                            tint = CosmicBackground,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Preserved Voice Note.m4a",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmicSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { playProgress },
                            color = CosmicSecondary,
                            trackColor = Color.DarkGray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = onDeleteAudio,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete recording",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
