package com.example.ui.screens

import android.content.Context
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.media3.common.Player
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.Song
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.glow
import com.example.ui.theme.Accents
import com.example.ui.theme.HotPink
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.GlassDarkSurface
import com.example.ui.theme.GlassLightSurface
import kotlinx.coroutines.delay

fun getDynamicAccentColor(song: Song?, defaultAccent: Color): Color {
    if (song == null || song.artworkUri.isNullOrEmpty()) {
        return defaultAccent
    }
    val gradientIdx = (song.id.toInt() % ProfessionalPolishGradients.size).let { if (it < 0) -it else it }
    return when (gradientIdx) {
        0 -> Color(0xFFD0BCFF) // Violet/Lavender dominant
        1 -> Color(0xFFFF2E93) // HotPink dominant
        2 -> Color(0xFF00E5FF) // ElectricBlue dominant
        3 -> Color(0xFFD0BCFF) // Purple/Lavender dominant
        4 -> Color(0xFFD0BCFF) // Lavender dominant
        else -> defaultAccent
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainerScreen(
    viewModel: MainViewModel,
    onNavigateToProfile: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0=Home, 1=Fav, 2=Library, 3=Settings
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }

    val accentColor = Accents[settings.accentColorIndex]
    val context = LocalContext.current
    var dominantColorState by remember(currentSong) { mutableStateOf<Color?>(null) }
    
    LaunchedEffect(currentSong, accentColor) {
        val song = currentSong
        if (song != null && !song.artworkUri.isNullOrEmpty()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val resolvedColor = try {
                    val uri = Uri.parse(song.artworkUri)
                    val bitmap = loadDownscaledBitmap(context, uri)
                    if (bitmap != null) {
                        val scaled = Bitmap.createScaledBitmap(bitmap, 1, 1, true)
                        val colorVal = scaled.getPixel(0, 0)
                        scaled.recycle()
                        val col = Color(colorVal)
                        if (col.luminance() < 0.15f) {
                            val baseDynamic = getDynamicAccentColor(song, accentColor)
                            Color(
                                red = (col.red + baseDynamic.red) / 2f,
                                green = (col.green + baseDynamic.green) / 2f,
                                blue = (col.blue + baseDynamic.blue) / 2f,
                                alpha = 1f
                            )
                        } else {
                            col
                        }
                    } else {
                        getDynamicAccentColor(song, accentColor)
                    }
                } catch (e: Exception) {
                    getDynamicAccentColor(song, accentColor)
                }
                dominantColorState = resolvedColor
            }
        } else {
            dominantColorState = getDynamicAccentColor(song, accentColor)
        }
    }

    val resolvedArtworkColor = dominantColorState ?: getDynamicAccentColor(currentSong, accentColor)
    val animatedArtworkColor by animateColorAsState(
        targetValue = resolvedArtworkColor,
        animationSpec = tween(durationMillis = 800),
        label = "animatedArtworkColor"
    )

    val showUnifiedPlayer = currentSong != null && !isPlayerExpanded

    val barHeight by animateDpAsState(
        targetValue = if (showUnifiedPlayer) 128.dp else 64.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "barHeight"
    )

    val cardCornerRadius by animateDpAsState(
        targetValue = if (showUnifiedPlayer) 24.dp else 32.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "cornerRadius"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent, // Managed by parent background
        bottomBar = {
            // FLOATING GLASS BOTTOM NAVIGATION
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Bottom Floating Glass Container
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight),
                    cornerRadius = cardCornerRadius,
                    isGlassEnabled = settings.isGlassEnabled,
                    isDark = settings.isDarkMode,
                    applyBlur = true // Static bottom bar: safe to apply backdrop blur
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AnimatedVisibility(
                            visible = showUnifiedPlayer,
                            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                        ) {
                            if (currentSong != null) {
                                MiniPlayer(
                                    song = currentSong!!,
                                    isPlaying = isPlaying,
                                    accentColor = accentColor,
                                    artworkColor = animatedArtworkColor,
                                    isGlassEnabled = settings.isGlassEnabled,
                                    isDark = settings.isDarkMode,
                                    viewModel = viewModel,
                                    onPlayPause = { viewModel.togglePlayPause() },
                                    onNext = { viewModel.playNext() },
                                    onClick = { isPlayerExpanded = true },
                                    isEmbedded = true
                                )
                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.08f),
                                    thickness = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Navigation Bar Row
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Crossfade(
                                targetState = showUnifiedPlayer,
                                animationSpec = tween(durationMillis = 300),
                                label = "NavBarTabsTransition"
                            ) { unified ->
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Tab 1: Home
                                    NavBarItem(
                                        icon = Icons.Default.Home,
                                        label = "Home",
                                        isActive = activeTab == 0,
                                        accentColor = accentColor,
                                        isDark = settings.isDarkMode,
                                        onClick = { activeTab = 0 }
                                    )

                                    // Tab 2: Favorites
                                    NavBarItem(
                                        icon = Icons.Default.Favorite,
                                        label = "Favs",
                                        isActive = activeTab == 1,
                                        accentColor = accentColor,
                                        isDark = settings.isDarkMode,
                                        onClick = { activeTab = 1 }
                                    )

                                    if (!unified) {
                                        // Placeholder for Center Circular Player Button
                                        Spacer(modifier = Modifier.width(64.dp))
                                    }

                                    // Tab 3: Playlists/Folders
                                    NavBarItem(
                                        icon = Icons.Default.LibraryMusic,
                                        label = "Library",
                                        isActive = activeTab == 2,
                                        accentColor = accentColor,
                                        isDark = settings.isDarkMode,
                                        onClick = { activeTab = 2 }
                                    )

                                    // Tab 4: Settings
                                    NavBarItem(
                                        icon = Icons.Default.Settings,
                                        label = "Settings",
                                        isActive = activeTab == 3,
                                        accentColor = accentColor,
                                        isDark = settings.isDarkMode,
                                        onClick = { activeTab = 3 }
                                    )
                                }
                            }
                        }
                    }
                }

                // LARGE CENTER FLOATING CIRCULAR PLAYER BUTTON
                AnimatedVisibility(
                    visible = !showUnifiedPlayer,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    CenterCirclePlayerButton(
                        isPlaying = isPlaying,
                        viewModel = viewModel,
                        currentSong = currentSong,
                        accentColor = accentColor,
                        isGlassEnabled = settings.isGlassEnabled,
                        isDark = settings.isDarkMode,
                        onClick = { isPlayerExpanded = true }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = innerPadding.calculateBottomPadding() / 2) // Compact spacing
        ) {
            // Render Selected Active Tab Content
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "TabContentTransition"
            ) { tab ->
                when (tab) {
                    0 -> HomeScreenContent(
                        viewModel = viewModel,
                        onNavigateToProfile = onNavigateToProfile,
                        onNavigateToScan = {
                            activeTab = 3
                            viewModel.setShowScanOverlay(true)
                        }
                    )
                    1 -> FavoritesScreenContent(viewModel)
                    2 -> PlaylistsScreenContent(viewModel)
                    3 -> SettingsScreenContent(viewModel)
                }
            }
        }
    }

    // SLIDE-UP FULL SCREEN PLAYER OVERLAY
    AnimatedVisibility(
        visible = isPlayerExpanded,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        FullPlayerScreen(
            viewModel = viewModel,
            accentColor = accentColor,
            artworkColor = animatedArtworkColor,
            onClose = { isPlayerExpanded = false },
            onOpenSleepTimer = { showSleepTimerSheet = true },
            onOpenQueue = { showQueueSheet = true }
        )
    }

    // Sleep Timer Bottom Sheet
    if (showSleepTimerSheet) {
        SleepTimerBottomSheet(
            viewModel = viewModel,
            accentColor = accentColor,
            onDismiss = { showSleepTimerSheet = false }
        )
    }

    // Queue Bottom Sheet
    if (showQueueSheet) {
        QueueBottomSheet(
            viewModel = viewModel,
            accentColor = accentColor,
            onDismiss = { showQueueSheet = false }
        )
    }
}

// Nav bar helper
@Composable
fun NavBarItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    accentColor: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) accentColor else (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp)
        )
    }
}

// Center floating Circle Player Button with progressive outline
@Composable
fun CenterCirclePlayerButton(
    isPlaying: Boolean,
    viewModel: MainViewModel,
    currentSong: Song?,
    accentColor: Color,
    isGlassEnabled: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val progress by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val progressPercent = if (duration > 0) progress.toFloat() / duration else 0f

    // Dynamic scale beat effect
    val animatedScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "beatScale"
    )

    Box(
        modifier = Modifier
            .offset(y = (-24).dp) // floats above navigation bar
            .size(72.dp)
            .glow(accentColor, radius = 20.dp, alpha = 0.35f)
            .scale(animatedScale)
            .clip(CircleShape)
            .background(accentColor)
            .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // SVG Progress Ring drawing around border in contrast color
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            drawArc(
                color = com.example.ui.theme.DeepViolet,
                startAngle = -90f,
                sweepAngle = 360f * progressPercent,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )
        }

        // Inner play/artwork
        val iconColor = if (accentColor.luminance() > 0.5f) {
            com.example.ui.theme.DeepViolet
        } else {
            Color.White
        }

        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = iconColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

// FLOATING MINI PLAYER
@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    accentColor: Color,
    artworkColor: Color = accentColor,
    isGlassEnabled: Boolean,
    isDark: Boolean,
    viewModel: MainViewModel,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    isEmbedded: Boolean = false
) {
    val progress by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val progressPercent = if (duration > 0) progress.toFloat() / duration else 0f

    val content = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(artworkColor.copy(alpha = 0.15f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = artworkColor,
                    modifier = Modifier.size(20.dp)
                )
                if (!song.artworkUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = song.artworkUri,
                        contentDescription = "Album Art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text titles
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Small controls
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play Pause",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Skip Next",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    if (isEmbedded) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .drawBehind {
                    val barHeight = 2.dp.toPx()
                    // Background track matching progress bar in HTML (white/5)
                    drawRect(
                        color = Color.White.copy(alpha = 0.05f),
                        size = size.copy(height = barHeight),
                        topLeft = Offset(0f, size.height - barHeight)
                    )
                    // Active progress track filled with accent color
                    drawRect(
                        color = accentColor,
                        size = size.copy(width = size.width * progressPercent, height = barHeight),
                        topLeft = Offset(0f, size.height - barHeight)
                    )
                }
        ) {
            content()
        }
    } else {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .testTag("mini_player")
                .drawBehind {
                    val barHeight = 2.dp.toPx()
                    // Background track matching progress bar in HTML (white/5)
                    drawRect(
                        color = Color.White.copy(alpha = 0.05f),
                        size = size.copy(height = barHeight),
                        topLeft = Offset(0f, size.height - barHeight)
                    )
                    // Active progress track filled with accent color
                    drawRect(
                        color = accentColor,
                        size = size.copy(width = size.width * progressPercent, height = barHeight),
                        topLeft = Offset(0f, size.height - barHeight)
                    )
                }
                .clickable(onClick = onClick),
            cornerRadius = 16.dp,
            isGlassEnabled = isGlassEnabled,
            isDark = isDark,
            applyBlur = true // Static mini-player overlay: safe to apply backdrop blur
        ) {
            content()
        }
    }
}

// FULL PLAYER SCREEN (immersive Glass space)
@Composable
fun FullPlayerScreen(
    viewModel: MainViewModel,
    accentColor: Color,
    artworkColor: Color,
    onClose: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenQueue: () -> Unit
) {
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val shuffle by viewModel.shuffleEnabled.collectAsStateWithLifecycle()
    val repeat by viewModel.repeatMode.collectAsStateWithLifecycle()
    val sleepSec by viewModel.sleepTimerRemainingSec.collectAsStateWithLifecycle()

    val context = LocalContext.current

    if (currentSong == null) {
        onClose()
        return
    }

    val formatTime = { ms: Long ->
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        "${m}:${"%02d".format(s)}"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Restore original premium dark AMOLED background exactly as it was before
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        // Organic glowing backlight blooms matching active track
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(400.dp)
                .glow(accentColor, radius = 120.dp, alpha = 0.22f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .windowInsetsPadding(WindowInsets.statusBars),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text(
                    text = "NOW PLAYING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 2.sp
                )
                IconButton(onClick = onOpenQueue) {
                    Icon(Icons.Default.QueueMusic, contentDescription = "Active Queue", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            // LARGE DECORATIVE ALBUM ARTWORK
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(270.dp)
                        .glow(artworkColor, radius = 60.dp, alpha = 0.35f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(artworkColor.copy(alpha = 0.2f), artworkColor.copy(alpha = 0.05f))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = artworkColor,
                        modifier = Modifier.size(110.dp)
                    )
                    if (currentSong != null && !currentSong!!.artworkUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = currentSong!!.artworkUri,
                            contentDescription = "Album Art",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }
            }

            // SONG METADATA
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSong!!.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentSong!!.artist,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Favorite toggler
                    IconButton(onClick = { viewModel.toggleFavorite(currentSong!!.id, !currentSong!!.isFavorite) }) {
                        Icon(
                            imageVector = if (currentSong!!.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Bookmark",
                            tint = if (currentSong!!.isFavorite) HotPink else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // INTERACTIVE SEEK BAR
                Slider(
                    value = if (duration > 0) progress.toFloat() / duration else 0f,
                    onValueChange = { viewModel.seekTo((it * duration).toLong()) },
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(progress), fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                    Text(formatTime(duration), fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PRIMARY PLAYBACK CONTROLS KEYBOARD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev
                IconButton(onClick = { viewModel.playPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(36.dp))
                }

                // Pulsing central Play/Pause key
                val playIconColor = if (accentColor.luminance() > 0.5f) Color.Black else Color.White
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .glow(accentColor, radius = 20.dp, alpha = 0.35f)
                        .clip(CircleShape)
                        .background(accentColor)
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = playIconColor,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Next
                IconButton(onClick = { viewModel.playNext() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECONDARY AUX CONTROLS ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Key
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffle) accentColor else Color.White.copy(alpha = 0.4f)
                    )
                }

                // Sleep Timer moon icon with countdown indicator tag
                Box(contentAlignment = Alignment.TopEnd) {
                    IconButton(onClick = onOpenSleepTimer) {
                        Icon(
                            imageVector = Icons.Default.NightsStay,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepSec > 0) accentColor else Color.White.copy(alpha = 0.4f)
                        )
                    }
                    if (sleepSec > 0) {
                        val minutesLeft = (sleepSec / 60) + 1
                        Box(
                            modifier = Modifier
                                .offset(x = (-2).dp, y = 2.dp)
                                .clip(CircleShape)
                                .background(HotPink)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${minutesLeft}m",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Repeat Mode Cycle Key
                IconButton(onClick = { viewModel.cycleRepeatMode() }) {
                    Icon(
                        imageVector = if (repeat == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat Mode",
                        tint = if (repeat != Player.REPEAT_MODE_OFF) accentColor else Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// SLEEP TIMER BOTTOM SHEET SELECTOR
private fun formatRemainingTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format("%dh %02dm %02ds", h, m, s)
    } else {
        String.format("%dm %02ds", m, s)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(
    viewModel: MainViewModel,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    var isCustomSelected by remember { mutableStateOf(false) }
    var hoursInput by remember { mutableStateOf("") }
    var minutesInput by remember { mutableStateOf("") }
    
    val sleepRemaining by viewModel.sleepTimerRemainingSec.collectAsStateWithLifecycle()
    val finishSong by viewModel.finishSongOnTimerEnd.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isDark = settings.isDarkMode
    val bTextColor = if (isDark) Color.White else Color(0xFF121212)
    val bSubTextColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF121212).copy(alpha = 0.6f)
    val bMutedColor = if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFF121212).copy(alpha = 0.4f)
    val bPlaceholderColor = if (isDark) Color.White.copy(alpha = 0.3f) else Color(0xFF121212).copy(alpha = 0.3f)
    val bContainerColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF121212) else Color.White,
        contentColor = bTextColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Configure Sleep Timer",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = bTextColor
            )

            if (sleepRemaining > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Red.copy(alpha = 0.05f))
                        .border(1.dp, Color.Red.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Active Timer Running", fontSize = 12.sp, color = bMutedColor)
                        Text(
                            text = formatRemainingTime(sleepRemaining),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.cancelSleepTimer()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red.copy(alpha = 0.15f),
                            contentColor = Color.Red
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Finish the Song Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bContainerColor)
                    .clickable { viewModel.setFinishSongOnTimerEnd(!finishSong) }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Finish the song playing when the timer ends",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = bTextColor
                    )
                    Text(
                        text = "Let the current track finish before stopping playback",
                        fontSize = 11.sp,
                        color = bMutedColor
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = finishSong,
                    onCheckedChange = { viewModel.setFinishSongOnTimerEnd(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (isDark) Color.Black else Color.White,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = bMutedColor,
                        uncheckedTrackColor = bContainerColor
                    )
                )
            }

            Text(
                text = "Select preset duration:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = bMutedColor
            )

            // Preset options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    TimerChip("15 Min", onClick = { viewModel.startSleepTimer(15); onDismiss() })
                }
                Box(modifier = Modifier.weight(1f)) {
                    TimerChip("30 Min", onClick = { viewModel.startSleepTimer(30); onDismiss() })
                }
                Box(modifier = Modifier.weight(1f)) {
                    TimerChip("45 Min", onClick = { viewModel.startSleepTimer(45); onDismiss() })
                }
                Box(modifier = Modifier.weight(1f)) {
                    TimerChip("1 Hour", onClick = { viewModel.startSleepTimer(60); onDismiss() })
                }
            }

            // Custom toggle option
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isCustomSelected) accentColor.copy(alpha = 0.1f) else bContainerColor)
                    .border(
                        1.dp,
                        if (isCustomSelected) accentColor.copy(alpha = 0.3f) else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { isCustomSelected = !isCustomSelected }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = if (isCustomSelected) accentColor else bTextColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Custom",
                        fontWeight = FontWeight.Bold,
                        color = if (isCustomSelected) accentColor else bTextColor,
                        fontSize = 14.sp
                    )
                }
            }

            // Custom selection expansion
            AnimatedVisibility(
                visible = isCustomSelected,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hours Input
                        TextField(
                            value = hoursInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 2) {
                                    hoursInput = input
                                }
                            },
                            label = { Text("H (Hours)", color = bPlaceholderColor, fontSize = 10.sp) },
                            placeholder = { Text("0", color = bPlaceholderColor) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = bTextColor,
                                unfocusedTextColor = bTextColor,
                                focusedContainerColor = bContainerColor,
                                unfocusedContainerColor = bContainerColor,
                                focusedIndicatorColor = accentColor,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // Minutes Input
                        TextField(
                            value = minutesInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 3) {
                                    minutesInput = input
                                }
                            },
                            label = { Text("M (Minutes)", color = bPlaceholderColor, fontSize = 10.sp) },
                            placeholder = { Text("0", color = bPlaceholderColor) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = bTextColor,
                                unfocusedTextColor = bTextColor,
                                focusedContainerColor = bContainerColor,
                                unfocusedContainerColor = bContainerColor,
                                focusedIndicatorColor = accentColor,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // Set Button
                        val hours = hoursInput.toIntOrNull() ?: 0
                        val minutes = minutesInput.toIntOrNull() ?: 0
                        val isValid = (hours > 0 || minutes > 0) && (hoursInput.isNotBlank() || minutesInput.isNotBlank())

                        Button(
                            onClick = {
                                if (isValid) {
                                    val totalSeconds = (hours * 3600L) + (minutes * 60L)
                                    viewModel.startSleepTimerCustom(totalSeconds)
                                    onDismiss()
                                }
                            },
                            enabled = isValid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.Black,
                                disabledContainerColor = bContainerColor,
                                disabledContentColor = bMutedColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("Set", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimerChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// ACTIVE QUEUE BOTTOM SHEET
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    viewModel: MainViewModel,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val queueList by viewModel.queue.collectAsStateWithLifecycle()
    val activeIndex by viewModel.currentQueueIndex.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isDark = settings.isDarkMode
    val bTextColor = if (isDark) Color.White else Color(0xFF121212)
    val bMutedColor = if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFF121212).copy(alpha = 0.4f)
    val bCardBg = if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.05f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White,
        contentColor = bTextColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Play Queue (${queueList.size} songs)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = bTextColor
                )
                TextButton(onClick = { viewModel.clearQueue(); onDismiss() }) {
                    Text("Clear All", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (queueList.isEmpty()) {
                Text("Queue is empty.", color = bMutedColor, modifier = Modifier.padding(vertical = 32.dp))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(queueList, key = { index, song -> "${song.id}_$index" }) { index, song ->
                        val isCurrent = index == activeIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCurrent) accentColor.copy(alpha = 0.15f) else bCardBg)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (isCurrent) Icons.Default.PlayArrow else Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (isCurrent) accentColor else bMutedColor
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(song.title, fontWeight = FontWeight.Bold, color = if (isCurrent) accentColor else bTextColor, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(song.artist, color = bMutedColor, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            IconButton(onClick = { viewModel.removeFromQueue(song.id) }) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove", tint = bMutedColor, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
