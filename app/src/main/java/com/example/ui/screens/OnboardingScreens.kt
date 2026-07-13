package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.glow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HotPink
import com.example.ui.theme.NeonGreen
import kotlinx.coroutines.delay

@Composable
fun OnboardingContainer(
    viewModel: MainViewModel,
    onFinished: () -> Unit
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.SPLASH) }
    var inputName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)) // AMOLED pitch black background
    ) {
        // Decorative ambient glow spots
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .glow(ElectricBlue, radius = 80.dp, alpha = 0.12f)
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomStart)
                .glow(HotPink, radius = 100.dp, alpha = 0.12f)
        )

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            },
            label = "OnboardingTransition"
        ) { step ->
            when (step) {
                OnboardingStep.SPLASH -> {
                    SplashScreen {
                        currentStep = OnboardingStep.PRIVACY
                    }
                }
                OnboardingStep.PRIVACY -> {
                    PrivacyPolicyScreen(
                        onNext = { currentStep = OnboardingStep.FEATURES }
                    )
                }
                OnboardingStep.FEATURES -> {
                    FeaturesShowcaseScreen(
                        onNext = { currentStep = OnboardingStep.ASK_NAME }
                    )
                }
                OnboardingStep.ASK_NAME -> {
                    AskNameScreen(
                        name = inputName,
                        onNameChanged = { inputName = it },
                        onNext = {
                            viewModel.saveUserName(inputName)
                            currentStep = OnboardingStep.WELCOME
                        }
                    )
                }
                OnboardingStep.WELCOME -> {
                    WelcomeScreen(
                        name = inputName,
                        onNext = {
                            viewModel.completeOnboarding()
                            onFinished()
                        }
                    )
                }
            }
        }
    }
}

enum class OnboardingStep {
    SPLASH, PRIVACY, FEATURES, ASK_NAME, WELCOME
}

// 1. SPLASH SCREEN
@Composable
fun SplashScreen(onComplete: () -> Unit) {
    val scale = remember { Animatable(0.5f) }
    val rotate = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        rotate.animateTo(
            targetValue = 360f,
            animationSpec = tween(1200, easing = LinearOutSlowInEasing)
        )
        delay(1000)
        onComplete()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .glow(NeonGreen, radius = 40.dp, alpha = 0.4f)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(NeonGreen.copy(alpha = 0.8f), ElectricBlue.copy(alpha = 0.8f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "BeatFlow Logo",
                    tint = Color.Black,
                    modifier = Modifier.size(64.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "BeatFlow",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Premium local audio engine",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )
        }
    }
}

// 2. PRIVACY POLICY SCREEN
@Composable
fun PrivacyPolicyScreen(onNext: () -> Unit) {
    var agreed by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    var faqOpen1 by remember { mutableStateOf(false) }
    var faqOpen2 by remember { mutableStateOf(false) }
    var faqOpen3 by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(36.dp))
        Text(
            text = "Privacy Shield",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "BeatFlow operates entirely offline.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Privacy Text Scrollable Panel
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF151515))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrivacyPoint("Privacy First", "All processing happens directly on your device. No telemetry, no logs, and no analytics.", Icons.Default.Shield)
                PrivacyPoint("Offline First", "We have no cloud databases or external sync servers. Your audio files never leave your device.", Icons.Default.SignalWifiOff)
                PrivacyPoint("Zero Ad Clutter", "We serve zero advertisements, promo banners, tracking pixels, or sponsorships. Just pure music.", Icons.Default.Star)
                PrivacyPoint("Local Files Only", "BeatFlow only reads folders you explicitly permit (such as your Music and Downloads folders).", Icons.Default.Folder)
                PrivacyPoint("Required Permissions", "We request access to local folders/media storage (to scan tracks) and notifications (to show lockscreen playback keys).", Icons.Default.LockOpen)

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "Frequently Asked Questions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                FAQItem(
                    question = "Does BeatFlow upload or sync songs?",
                    answer = "No. BeatFlow operates 100% offline. It has no networking APIs or cloud connectivity capabilities whatsoever. Your media files remain safely on your disk.",
                    isOpen = faqOpen1,
                    onToggle = { faqOpen1 = !faqOpen1 }
                )

                FAQItem(
                    question = "Does BeatFlow track listening stats?",
                    answer = "Yes, but listening statistics (total songs played and active timing) are calculated procedurally and saved inside your local secure on-device Room SQL database. No third party or server has access to it.",
                    isOpen = faqOpen2,
                    onToggle = { faqOpen2 = !faqOpen2 }
                )

                FAQItem(
                    question = "Can it access my personal photos?",
                    answer = "No. BeatFlow requests target-specific media access queries designed exclusively for audio and metadata. It has no filters or directories requesting pictures or documents.",
                    isOpen = faqOpen3,
                    onToggle = { faqOpen3 = !faqOpen3 }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Checkbox row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { agreed = !agreed }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = agreed,
                onCheckedChange = { agreed = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = NeonGreen,
                    uncheckedColor = Color.White.copy(alpha = 0.4f),
                    checkmarkColor = Color.Black
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "I read and agree to the Privacy Policy and local file scans.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Next Button
        Button(
            onClick = onNext,
            enabled = agreed,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .glow(if (agreed) NeonGreen else Color.Transparent, radius = 12.dp, alpha = 0.2f),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonGreen,
                contentColor = Color.Black,
                disabledContainerColor = Color.White.copy(alpha = 0.1f),
                disabledContentColor = Color.White.copy(alpha = 0.3f)
            )
        ) {
            Text("Acknowledge & Accept", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun PrivacyPoint(title: String, desc: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeonGreen,
            modifier = Modifier
                .size(24.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Text(desc, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun FAQItem(question: String, answer: String, isOpen: Boolean, onToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .clickable(onClick = onToggle)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(question, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (isOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier.size(18.dp)
            )
        }
        AnimatedVisibility(
            visible = isOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Text(
                text = answer,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// 3. FEATURES SHOWCASE SCREEN
@Composable
fun FeaturesShowcaseScreen(onNext: () -> Unit) {
    var activePage by remember { mutableStateOf(0) }
    val totalPages = 4

    val featureTitle = when (activePage) {
        0 -> "All popular audio formats"
        1 -> "Beautiful Glass Design"
        2 -> "Smart Local Scanners"
        else -> "Background & Lockscreens"
    }

    val featureDesc = when (activePage) {
        0 -> "Play MP3, AAC, FLAC, WAV, M4A, OGG, OPUS, and more smoothly with CD-quality playback fidelity."
        1 -> "Immerse yourself in a luxurious glassmorphic, responsive space matching dynamic lighting and AMOLED deep blacks."
        2 -> "Clean your library seamlessly. Ignore duplicate audios, bypass system ringtones, and filter out short files under 1 minute."
        else -> "Enjoy continuous listening outside the application with system notifications, lockscreen keys, and Bluetooth triggers."
    }

    val featureIcon = when (activePage) {
        0 -> Icons.Default.AudioFile
        1 -> Icons.Default.AutoAwesome
        2 -> Icons.Default.Search
        else -> Icons.Default.PlayCircleFilled
    }

    val glowColor = when (activePage) {
        0 -> NeonGreen
        1 -> ElectricBlue
        2 -> HotPink
        else -> NeonGreen
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        // Center Content Box
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .glow(glowColor, radius = 50.dp, alpha = 0.35f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF161616))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = featureIcon,
                    contentDescription = null,
                    tint = glowColor,
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = featureTitle,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = featureDesc,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
                lineHeight = 22.sp
            )
        }

        // Indicator and Next row
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalPages) { idx ->
                    val width = if (idx == activePage) 24.dp else 8.dp
                    val color = if (idx == activePage) glowColor else Color.White.copy(alpha = 0.2f)
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activePage > 0) {
                    TextButton(onClick = { activePage-- }) {
                        Text("Back", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(60.dp))
                }

                Button(
                    onClick = {
                        if (activePage < totalPages - 1) {
                            activePage++
                        } else {
                            onNext()
                        }
                    },
                    modifier = Modifier
                        .height(50.dp)
                        .padding(horizontal = 12.dp)
                        .glow(glowColor, radius = 10.dp, alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = glowColor,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = if (activePage < totalPages - 1) "Continue" else "Get Started",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// 4. ASK USER NAME SCREEN
@Composable
fun AskNameScreen(
    name: String,
    onNameChanged: (String) -> Unit,
    onNext: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isError = name.trim().length < 2 || name.trim().length > 15

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .glow(ElectricBlue, radius = 30.dp, alpha = 0.25f)
                    .clip(CircleShape)
                    .background(Color(0xFF161616)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "What should we call you?",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your name to personalize your dashboard statistics.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Text Input Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                isGlassEnabled = false,
                isDark = true
            ) {
                TextField(
                    value = name,
                    onValueChange = onNameChanged,
                    placeholder = { Text("e.g. Mohan", color = Color.White.copy(alpha = 0.3f)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            if (!isError) onNext()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (name.isNotEmpty() && isError) {
                Text(
                    text = "Name must be between 2 and 15 characters.",
                    color = Color.Red.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Button(
            onClick = onNext,
            enabled = !isError,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .glow(if (!isError) ElectricBlue else Color.Transparent, radius = 12.dp, alpha = 0.2f),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricBlue,
                contentColor = Color.Black,
                disabledContainerColor = Color.White.copy(alpha = 0.1f),
                disabledContentColor = Color.White.copy(alpha = 0.3f)
            )
        ) {
            Text("Proceed", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// 5. WELCOME SCREEN
@Composable
fun WelcomeScreen(
    name: String,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val isTiramisu = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // On completion, proceed forward
        onNext()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .glow(NeonGreen, radius = 40.dp, alpha = 0.3f)
                    .clip(CircleShape)
                    .background(Color(0xFF161616)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Hello $name 👋",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Welcome to BeatFlow.",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Your music,\nyour privacy,\nyour experience.",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }

        Button(
            onClick = {
                val permissionToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                val hasPermission = ContextCompat.checkSelfPermission(context, permissionToCheck) == PackageManager.PERMISSION_GRANTED
                if (!hasPermission) {
                    permissionLauncher.launch(permissionsToRequest)
                } else {
                    onNext()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .glow(NeonGreen, radius = 12.dp, alpha = 0.25f),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonGreen,
                contentColor = Color.Black
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Get Started", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = Color.Black)
            }
        }
    }
}
