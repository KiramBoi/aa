package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DreamEntry
import com.example.ui.DreamViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(viewModel: DreamViewModel) {
    val entries by viewModel.allEntries.collectAsState()
    val selectedDate by viewModel.selectedCalendarDate.collectAsState()
    val selectedEntry by viewModel.selectedCalendarEntry.collectAsState()
    val currentMonthCal by viewModel.currentCalendarMonth.collectAsState()

    val aiText by viewModel.aiAnalysisText.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    val scrollState = rememberScrollState()

    // Format headers
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthTitle = monthYearFormat.format(currentMonthCal.time)

    // Calculate calendar grid cells
    val monthCal = currentMonthCal.clone() as Calendar
    monthCal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK) // 1: Sunday, 2: Monday...
    val maxDaysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val gridCells = remember(currentMonthCal, maxDaysInMonth, firstDayOfWeek) {
        val list = mutableListOf<DateCellInfo>()
        
        // Offset shift for blank grid slots at start of month
        for (i in 1 until firstDayOfWeek) {
            list.add(DateCellInfo(dateString = "", dayValue = 0, isDummy = true))
        }

        val cellSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val workingCal = currentMonthCal.clone() as Calendar

        for (day in 1..maxDaysInMonth) {
            workingCal.set(Calendar.DAY_OF_MONTH, day)
            val dateStr = cellSdf.format(workingCal.time)
            list.add(DateCellInfo(dateString = dateStr, dayValue = day, isDummy = false))
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 80.dp) // Leave screen space for navigation layout
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.NightsStay,
                contentDescription = "Sleep Scroll",
                tint = CosmicPrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Subconscious Calendar",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        // Calendar Shell Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicCardBg),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header navigation row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.adjustMonth(-1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = CosmicSecondary)
                    }
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CosmicPrimary,
                            fontSize = 18.sp
                        )
                    )
                    IconButton(onClick = { viewModel.adjustMonth(1) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = CosmicSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sunday - Saturday columns text
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    val daysHeader = listOf("S", "M", "T", "W", "T", "F", "S")
                    daysHeader.forEach { dayLetter ->
                        Text(
                            text = dayLetter,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Custom calendar Grid drawing
                val rows = gridCells.chunked(7)
                Column {
                    rows.forEach { rowCells ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            rowCells.forEach { cell ->
                                if (cell.isDummy) {
                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    val entryForCell = entries.find { it.dateString == cell.dateString }
                                    val isCellSelected = cell.dateString == selectedDate

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                width = if (isCellSelected) 2.dp else 0.dp,
                                                color = if (isCellSelected) Color.White else Color.Transparent,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { viewModel.selectCalendarDate(cell.dateString) }
                                            .drawBehind {
                                                // Innovative double indicator painting
                                                if (entryForCell != null) {
                                                    val isLuc = entryForCell.isLucid
                                                    val isRec = entryForCell.hasDreamRecall

                                                    when {
                                                        isLuc && isRec -> {
                                                            // Split hemisphere double indicators!
                                                            val lucAlpha = 0.25f + (entryForCell.lucidIntensity / 10f) * 0.75f
                                                            val recAlpha = 0.25f + (entryForCell.recallLevel / 10f) * 0.75f
                                                            
                                                            // Paint left side Purple/Teal
                                                            drawRect(
                                                                color = LucidPurple.copy(alpha = lucAlpha),
                                                                topLeft = Offset(0f, 0f),
                                                                size = Size(size.width / 2f, size.height)
                                                            )
                                                            // Paint right side Amber/Gold
                                                            drawRect(
                                                                color = RecallGold.copy(alpha = recAlpha),
                                                                topLeft = Offset(size.width / 2f, 0f),
                                                                size = Size(size.width / 2f, size.height)
                                                            )
                                                        }
                                                        isLuc -> {
                                                            // Only Lucid lighting up entire cell
                                                            val lucAlpha = 0.25f + (entryForCell.lucidIntensity / 10f) * 0.75f
                                                            drawRect(
                                                                color = LucidPurple.copy(alpha = lucAlpha)
                                                            )
                                                        }
                                                        isRec -> {
                                                            // Only recall lighting up entire cell
                                                            val recAlpha = 0.25f + (entryForCell.recallLevel / 10f) * 0.75f
                                                            drawRect(
                                                                color = RecallGold.copy(alpha = recAlpha)
                                                            )
                                                        }
                                                        else -> {
                                                            drawRect(color = Color(0xFF221F3A))
                                                        }
                                                    }
                                                } else {
                                                    // Day with no entries is dim
                                                    drawRect(color = Color(0xFF1E1C2F).copy(alpha = 0.5f))
                                                }
                                            }
                                            .testTag("calendar_day_${cell.dayValue}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cell.dayValue.toString(),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (entryForCell != null) Color.White else Color.Gray.copy(alpha = 0.8f)
                                            )
                                        )
                                    }
                                }
                            }
                            // Fill trailing spacers if row is incomplete
                            if (rowCells.size < 7) {
                                for (k in 0 until (7 - rowCells.size)) {
                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Subconscious Review Panel below Calendar
        Text(
            text = "Subconscious Details",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = CosmicSecondary
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        AnimatedContent(
            targetState = selectedEntry,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "SubconsciousPanel"
        ) { entry ->
            if (entry != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("review_details_card"),
                    colors = CardDefaults.cardColors(containerColor = CosmicCardBg),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDateLong(entry.dateString),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicSecondary
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (entry.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (entry.isFavorite) CosmicAccent else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                // Core sleep star quality rating
                                Row {
                                    for (s in 1..entry.sleepRating) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Star",
                                            tint = StarActive,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dream Title
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                        )

                        // Tags Row
                        if (entry.tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                entry.tags.split(",").forEach { tag ->
                                    val cleaned = tag.trim()
                                    if (cleaned.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(text = cleaned, fontSize = 11.sp, color = CosmicPrimary)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = Color.Gray.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Dual details expanders
                        if (entry.isLucid) {
                            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(LucidPurple.copy(alpha = 0.2f), CircleShape)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Lucid Active", fontSize = 10.sp, color = LucidTeal, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Int: ${entry.lucidIntensity}/10  Cla: ${entry.lucidClarity}/10",
                                        fontSize = 12.sp,
                                        color = Color.LightGray
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = entry.lucidDescription.ifEmpty { "Woke up in the dream with no further details save." },
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.9f))
                                )
                                if (entry.lucidAudioPath != null) {
                                    VoicePlaybackButton(
                                        title = "Play preserved lucid voice log",
                                        onPlay = { viewModel.playAudio(entry.lucidAudioPath, true) },
                                        onStop = { viewModel.stopAudio(true) }
                                    )
                                }
                            }
                        }

                        if (entry.hasDreamRecall) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(RecallGold.copy(alpha = 0.2f), CircleShape)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Recall Detail", fontSize = 10.sp, color = RecallGold, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Vibrancy Score: ${entry.recallLevel}/10",
                                        fontSize = 12.sp,
                                        color = Color.LightGray
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = entry.recallDescription.ifEmpty { "Detailed non-lucid story preserved." },
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.9f))
                                )
                                if (entry.recallAudioPath != null) {
                                    VoicePlaybackButton(
                                        title = "Play preserved recall voice log",
                                        onPlay = { viewModel.playAudio(entry.recallAudioPath, false) },
                                        onStop = { viewModel.stopAudio(false) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Offline Coach button
                            Button(
                                onClick = { viewModel.requestAiInterpretation(entry) },
                                colors = ButtonDefaults.buttonColors(containerColor = CosmicSecondary),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.weight(1.2f).height(38.dp).testTag("ai_coach_button")
                            ) {
                                Icon(Icons.Default.QueryStats, contentDescription = "Offline Analyst", modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Offline Coach", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    viewModel.loadEntryForEditing(entry)
                                    viewModel.selectTab(0) // go back to logger
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.weight(0.8f).height(38.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Note", fontSize = 11.sp)
                            }

                            IconButton(
                                onClick = { viewModel.deleteEntry(entry) },
                                modifier = Modifier.size(38.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.9f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            } else {
                // Empty state for this day
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("empty_calendar_card"),
                    colors = CardDefaults.cardColors(containerColor = CosmicCardBg.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "No Voyage",
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "A Night of Undocumented Sleep",
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "No dreams logged on ${formatDateLong(selectedDate)}. Establish intention before rest to heighten lucidity.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )
                        Button(
                            onClick = { viewModel.createNewLogForDate(selectedDate) },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary, contentColor = CosmicBackground),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Log", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Embark Dream Log", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Offline local Analysis popup Dialog
    if (aiText != null || isAnalyzing) {
        AlertDialog(
            onDismissRequest = { viewModel.clearAiAnalysis() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QueryStats, contentDescription = "Offline Coach", tint = CosmicSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Local Dream Analysis",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(color = CosmicSecondary, modifier = Modifier.size(32.dp).padding(vertical = 16.dp))
                        Text("Reading local subconscious ledgers, decoding dream patterns...", fontSize = 13.sp, color = Color.Gray)
                    } else {
                        Text(
                            text = aiText ?: "Vaporizing stardust logs. Try asking again.",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Left,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearAiAnalysis() },
                    colors = ButtonDefaults.textButtonColors(contentColor = CosmicSecondary)
                ) {
                    Text("Close Scroll", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CosmicCardBg,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun VoicePlaybackButton(
    title: String,
    onPlay: () -> Unit,
    onStop: () -> Unit
) {
    var isPlayingStatus by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .clickable {
                if (isPlayingStatus) {
                    onStop()
                    isPlayingStatus = false
                } else {
                    onPlay()
                    isPlayingStatus = true
                }
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isPlayingStatus) Icons.Filled.StopCircle else Icons.Filled.PlayCircle,
            contentDescription = "Play",
            tint = CosmicSecondary,
            modifier = Modifier.size(20.dp)
        )
        Text(text = title, fontSize = 11.sp, color = CosmicSecondary, modifier = Modifier.weight(1f))
    }
}

data class DateCellInfo(
    val dateString: String,
    val dayValue: Int,
    val isDummy: Boolean
)

fun formatDateLong(dateStr: String): String {
    if (dateStr.isEmpty()) return ""
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = parser.parse(dateStr)
        val formatter = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        if (date != null) formatter.format(date) else dateStr
    } catch (e: Exception) {
        dateStr
    }
}
