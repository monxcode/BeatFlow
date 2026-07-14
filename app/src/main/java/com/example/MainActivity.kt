package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.MainContainerScreen
import com.example.ui.screens.OnboardingContainer
import com.example.ui.screens.ProfileScreenContent
import com.example.ui.theme.BeatFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            val settingsState by viewModel.settings.collectAsStateWithLifecycle()

            BeatFlowTheme(
                isDarkMode = settingsState.isDarkMode,
                isAmoledMode = settingsState.isAmoledMode,
                accentColorIndex = settingsState.accentColorIndex
            ) {
                var isShowingProfile by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AnimatedContent(
                        targetState = settingsState.isOnboardingCompleted,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "MainFlowTransition"
                    ) { onboardingCompleted ->
                        if (!onboardingCompleted) {
                            OnboardingContainer(
                                viewModel = viewModel,
                                onFinished = {
                                    // Complete
                                }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (isShowingProfile) {
                                    // Immersive profile screen container
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        // Top Bar
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .windowInsetsPadding(WindowInsets.statusBars)
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = { isShowingProfile = false }) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowBack,
                                                    contentDescription = "Back",
                                                    tint = MaterialTheme.colorScheme.onBackground
                                                )
                                            }
                                            Text(
                                                text = "Profile",
                                                color = MaterialTheme.colorScheme.onBackground,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(start = 12.dp)
                                            )
                                        }

                                        Box(modifier = Modifier.weight(1f)) {
                                            ProfileScreenContent(viewModel = viewModel)
                                        }
                                    }
                                } else {
                                    MainContainerScreen(
                                        viewModel = viewModel,
                                        onNavigateToProfile = { isShowingProfile = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
