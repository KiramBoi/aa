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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DreamEntry
import com.example.ui.DreamViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightsScreen(viewModel: DreamViewModel) {
    val entries by viewModel.allEntries.collectAsState()
    val realityResult by viewModel.realityCheckStatus.collectAsState()

    val scrollState = rememberScrollState()

    // Calculate high level metrics
    val totalNights = entries.size
    val lucidCount = entries.count { it.isLucid }
    val lucidRatio = if (totalNights > 0) ((lucidCount.toFloat() / totalNights) * 100).toInt() else 0
    
    val averageRecall = if (entries.count { it.hasDreamRecall } > 0) {
        val sum = entries.filter { it.hasDreamRecall }.sumOf { it.recallLevel }
        String.format("%.1f", sum.toFloat() / entries.count { it.hasDreamRecall })
    } else {
        "0.0"
    }

    val averageSleep = if (totalNights > 0) {
        String.format("%.1f", entries.sumOf { it.sleepRating }.toFloat() / totalNights)
    } else {
        "0.0"
    }

    // Filter last 7 lucid entries to draw the trend line
    val trendEntries = remember(entries) {
        entries.filter { it.isLucid || it.hasDreamRecall }.take(7).reversed()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Lucid Coach",
                tint = CosmicPrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Dream Coach & Insights",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        // Stats Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Voyages",
                value = "$totalNights",
                desc = "Tracked nights",
                icon = Icons.Default.NightsStay,
                color = CosmicPrimary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Lucidity",
                value = "$lucidRatio%",
                desc = "Lucid success",
                icon = Icons.Default.Brightness4,
                color = LucidTeal,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Recall Index",
                value = "$averageRecall/10",
                desc = "Clarity score",
                icon = Icons.Default.MenuBook,
                color = RecallGold,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Sleep Quality",
                value = "$averageSleep/5",
                desc = "Average stars",
                icon = Icons.Default.Star,
                color = StarActive,
                modifier = Modifier.weight(1f)
            )
        }

        // Lucid Sparkline Canvas Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicCardBg),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Subconscious Waveforms (Activity Trend)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CosmicSecondary
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Historical progression of intensity and sleep metrics",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (trendEntries.size >= 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .padding(horizontal = 8.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val points = trendEntries.mapIndexed { idx, entry ->
                                val x = (idx.toFloat() / (trendEntries.size - 1)) * size.width
                                val level = if (entry.isLucid) entry.lucidIntensity else entry.recallLevel
                                val y = size.height - ((level - 1) / 9f) * (size.height * 0.8f) - (size.height * 0.1f)
                                Offset(x, y)
                            }

                            // Draw glowing area under path
                            val fillPath = Path().apply {
                                moveTo(0f, size.height)
                                points.forEachIndexed { index, point ->
                                    if (index == 0) lineTo(point.x, point.y) else lineTo(point.x, point.y)
                                }
                                lineTo(size.width, size.height)
                                close()
                            }

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(LucidTeal.copy(alpha = 0.25f), Color.Transparent)
                                )
                            )

                            // Draw line
                            val polylinePath = Path().apply {
                                points.forEachIndexed { index, point ->
                                    if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                                }
                            }

                            drawPath(
                                path = polylinePath,
                                color = LucidTeal,
                                style = Stroke(width = 3.dp.toPx())
                            )

                            // Draw dots
                            points.forEachIndexed { idx, point ->
                                drawCircle(
                                    color = Color.White,
                                    radius = 4.dp.toPx(),
                                    center = point
                                )
                                drawCircle(
                                    color = LucidPurple,
                                    radius = 2.dp.toPx(),
                                    center = point
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("← Past", fontSize = 10.sp, color = Color.Gray)
                        Text("Vibrancy Wave (1-10)", fontSize = 10.sp, color = CosmicSecondary, fontWeight = FontWeight.Bold)
                        Text("Recent →", fontSize = 10.sp, color = Color.Gray)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Log at least 2 entries to display wave analytics",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Reality Check Coach Action Module
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicCardBg),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Reality Check Coach",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = RecallGold
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Perform these checks multiple times daily to train lucidity sparks inside dreams.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Reality Exercise 1
                CoachActionRow(
                    title = "Gravity Stabiliser Test",
                    desc = "Test your relationship with physics. Jumping in dreams causes soft orbits.",
                    icon = Icons.Default.KeyboardArrowUp,
                    color = LucidTeal,
                    onClick = { viewModel.performRealityCheck("gravity") }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Reality Exercise 2
                CoachActionRow(
                    title = "Digital Hand Verification",
                    desc = "Observe finger count and solidity. Hands bend and duplicate during sleep.",
                    icon = Icons.Default.PanTool,
                    color = LucidPurple,
                    onClick = { viewModel.performRealityCheck("fingers") }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Reality Exercise 3
                CoachActionRow(
                    title = "Refracted Text Reading",
                    desc = "Read any sentence twice. Text transforms drift dynamically in dreams.",
                    icon = Icons.Default.FontDownload,
                    color = RecallGold,
                    onClick = { viewModel.performRealityCheck("text") }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }

    // Modal Sheet or Card revealing Reality Check verification.
    if (realityResult != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearRealityCheck() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = "Security", tint = LucidTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reality Check Complete", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = realityResult ?: "",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearRealityCheck() },
                    colors = ButtonDefaults.textButtonColors(contentColor = LucidTeal)
                ) {
                    Text("Close Probe", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CosmicCardBg,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CosmicCardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(text = desc, fontSize = 10.sp, color = Color.Gray.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun CoachActionRow(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            Text(text = desc, fontSize = 10.sp, color = Color.Gray)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "Run Test", tint = Color.Gray, modifier = Modifier.size(16.dp))
    }
}
