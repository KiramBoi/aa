package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DreamViewModel
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.CosmicCardBg
import com.example.ui.theme.CosmicPrimary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppHost()
            }
        }
    }
}

@Composable
fun MainAppHost() {
    val viewModel: DreamViewModel = viewModel()
    val activeTab by viewModel.currentTab.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = CosmicCardBg,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("app_bottom_bar")
            ) {
                // Tab 0: Submit Log
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 0) Icons.Filled.AddCircle else Icons.Outlined.AddCircle,
                            contentDescription = "Journal Logs"
                        )
                    },
                    label = { Text("Log Entry", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CosmicPrimary,
                        selectedTextColor = CosmicPrimary,
                        indicatorColor = CosmicPrimary.copy(alpha = 0.15f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("nav_tab_journal")
                )

                // Tab 1: Calendar View
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 1) Icons.Filled.DateRange else Icons.Outlined.DateRange,
                            contentDescription = "Subconscious Calendar"
                        )
                    },
                    label = { Text("Calendar", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CosmicPrimary,
                        selectedTextColor = CosmicPrimary,
                        indicatorColor = CosmicPrimary.copy(alpha = 0.15f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("nav_tab_calendar")
                )

                // Tab 2: Insights Dashboard
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 2) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                            contentDescription = "Lucidity Insights"
                        )
                    },
                    label = { Text("Insights", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CosmicPrimary,
                        selectedTextColor = CosmicPrimary,
                        indicatorColor = CosmicPrimary.copy(alpha = 0.15f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("nav_tab_insights")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                0 -> JournalScreen(viewModel)
                1 -> CalendarScreen(viewModel)
                2 -> InsightsScreen(viewModel)
            }
        }
    }
}

