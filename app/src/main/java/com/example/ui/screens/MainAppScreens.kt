package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.provider.MediaStore
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BeatFlowSettings
import com.example.data.Playlist
import com.example.data.Song
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.glow
import com.example.ui.theme.*
import com.example.ui.theme.*
import com.example.ui.theme.*
import java.util.Calendar

// Pre-baked Avatar Gradients for high-fidelity offline personalization
val AvatarGradients = listOf(
    Brush.linearGradient(listOf(NeonGreen, ElectricBlue)),
    Brush.linearGradient(listOf(HotPink, ElectricBlue)),
    Brush.linearGradient(listOf(Color(0xFFFFB300), HotPink)),
    Brush.linearGradient(listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)))
)

@Composable
fun ProfileAvatar(
    profileImagePath: String,
    userName: String,
    accentColorIndex: Int,
    size: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    val hasValidImage = remember(profileImagePath) {
        profileImagePath.isNotEmpty()
    }
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (hasValidImage) {
                    Modifier.background(Color.DarkGray)
                } else {
                    val avatarIdx = accentColorIndex.coerceIn(0, AvatarGradients.size - 1)
                    Modifier.background(AvatarGradients[avatarIdx])
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (hasValidImage) {
            AsyncImage(
                model = profileImagePath,
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = userName.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = fontSize
            )
        }
    }
}

enum class SongSortOrder {
    NEW_ADD,
    OLD_ADD,
    A_Z
}

@Composable
fun HomeScreenContent(
    viewModel: MainViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToScan: () -> Unit
) {
    val songs by viewModel.allSongs.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val latest by viewModel.latestAddedSongs.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayedSongs.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredSongs by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val statsList by viewModel.listeningStats.collectAsStateWithLifecycle()

    var currentSortOrder by remember { mutableStateOf(SongSortOrder.NEW_ADD) }

    val sortedSongs = remember(songs, currentSortOrder) {
        when (currentSortOrder) {
            SongSortOrder.NEW_ADD -> songs.sortedByDescending { it.dateAdded }
            SongSortOrder.OLD_ADD -> songs.sortedBy { it.dateAdded }
            SongSortOrder.A_Z -> songs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
        }
    }

    var selectedCategory by remember { mutableStateOf("All") }
    var showRecentlyPlayedScreen by remember { mutableStateOf(false) }
    var selectedAlbum by remember { mutableStateOf<String?>(null) }
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }

    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    val albums = remember(songs) {
        songs.map { it.album }.distinct().filter { it.isNotBlank() }
    }

    val artists = remember(songs) {
        songs.map { it.artist }.distinct().filter { it.isNotBlank() }
    }

    val albumSongs = remember(songs, selectedAlbum) {
        if (selectedAlbum != null) {
            songs.filter { it.album == selectedAlbum }
        } else {
            emptyList()
        }
    }

    val artistSongs = remember(songs, selectedArtist) {
        if (selectedArtist != null) {
            songs.filter { it.artist == selectedArtist }
        } else {
            emptyList()
        }
    }

    val playlistSongsFlow = remember(selectedPlaylist) {
        selectedPlaylist?.let { viewModel.getSongsInPlaylist(it.id) } ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }
    val playlistSongs by playlistSongsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var isSearchExpandedByIcon by remember { mutableStateOf(false) }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    // Reset header search toggle if scrolled back to the top
    LaunchedEffect(isScrolled) {
        if (!isScrolled) {
            isSearchExpandedByIcon = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
        
        // 1. FIXED TOP HEADER & PROFILE ICON (Adapts responsive height on scroll)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val showSearchInputInHeader = isScrolled && (isSearchExpandedByIcon || searchQuery.isNotEmpty())

            if (showSearchInputInHeader) {
                // Expanded compact search input in the header row
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            isSearchExpandedByIcon = false
                            viewModel.updateSearchQuery("")
                            focusManager.clearFocus()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Collapse Search",
                            tint = themeText
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))

                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        isGlassEnabled = settings.isGlassEnabled,
                        isDark = settings.isDarkMode,
                        cornerRadius = 12.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Icon",
                                tint = themeTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                textStyle = TextStyle(color = themeText, fontSize = 14.sp),
                                singleLine = true,
                                cursorBrush = SolidColor(Color.White),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Search...",
                                            color = themeTextFaint,
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = themeTextMuted,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { viewModel.updateSearchQuery("") }
                                )
                            }
                        }
                    }
                }
            } else {
                // Header Titles (Responsive typography sizes)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    if (isScrolled) {
                        Text(
                            text = "BeatFlow",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = themeText
                        )
                    } else {
                        Text(
                            text = "$greeting, ${settings.userName} 👋",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = themeText
                        )
                        Text(
                            text = "Your music, secured offline.",
                            fontSize = 12.sp,
                            color = themeTextMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Search Icon (when scrolled and not typing) and Profile Avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isScrolled && !showSearchInputInHeader) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(themeCardBgSelected)
                            .clickable {
                                isSearchExpandedByIcon = true
                                coroutineScope.launch {
                                    delay(100)
                                    focusRequester.requestFocus()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Open Search",
                            tint = themeText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Profile Avatar (Smaller size on scroll)
                val avatarSize = if (isScrolled) 36.dp else 48.dp
                val avatarFontSize = if (isScrolled) 13.sp else 18.sp
                ProfileAvatar(
                    profileImagePath = settings.profileImagePath,
                    userName = settings.userName,
                    accentColorIndex = settings.accentColorIndex,
                    size = avatarSize,
                    fontSize = avatarFontSize,
                    modifier = Modifier.clickable(onClick = onNavigateToProfile)
                )
            }
        }

        // 2. FIXED GLOSSY INSTANT SEARCH (Reduced height and auto-collapses on scroll)
        AnimatedVisibility(
            visible = !isScrolled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    isGlassEnabled = settings.isGlassEnabled,
                    isDark = settings.isDarkMode,
                    cornerRadius = 14.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = themeTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            textStyle = TextStyle(color = themeText, fontSize = 14.sp),
                            singleLine = true,
                            cursorBrush = SolidColor(Color.White),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search songs, artists, albums...",
                                        color = themeTextFaint,
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = themeTextMuted,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { viewModel.updateSearchQuery("") }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. SCROLLABLE CONTENT (Songs, stats, list, etc.)
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

        // CONDITIONAL SEARCH VIEW vs HOME SECTIONS
        if (searchQuery.isNotBlank()) {
            item {
                Text(
                    text = "Search Results (${filteredSongs.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeText
                )
            }

            if (filteredSongs.isEmpty()) {
                item {
                    EmptyStatePlaceholder(
                        title = "No songs match",
                        subtitle = "Try refining your search keyword.",
                        icon = Icons.Default.AudioFile
                    )
                }
            } else {
                items(filteredSongs, key = { it.id }) { song ->
                    val onPlay = remember(song, filteredSongs) { { viewModel.playSong(song, filteredSongs) } }
                    val onFavoriteToggle = remember(song.id, song.isFavorite) { { viewModel.toggleFavorite(song.id, !song.isFavorite) } }
                    SongListItem(
                        song = song,
                        settings = settings,
                        onPlay = onPlay,
                        onFavoriteToggle = onFavoriteToggle,
                        viewModel = viewModel
                    )
                }
            }
        } else {
            // Quick Stats Card (Professional Polish Style)
            item {
                val totalListenTimeMs = remember(statsList) { statsList.sumOf { it.listeningTimeMs } }
                val totalListenTimeSec = totalListenTimeMs / 1000
                val formatListenTime = remember(totalListenTimeSec) {
                    val h = totalListenTimeSec / 3600
                    val m = (totalListenTimeSec % 3600) / 60
                    val s = totalListenTimeSec % 60
                    if (h > 0) "${h}h ${m}m" else if (m > 0) "${m}m" else "${s}s"
                }
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    isGlassEnabled = settings.isGlassEnabled,
                    isDark = settings.isDarkMode
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Listening Time", fontSize = 11.sp, color = themeTextFaint)
                            Text(formatListenTime, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = themeText, modifier = Modifier.padding(top = 4.dp))
                        }
                        Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.1f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Tracks", fontSize = 11.sp, color = themeTextFaint)
                            Text(songs.size.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = themeText, modifier = Modifier.padding(top = 4.dp))
                        }
                        Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.1f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Favorites", fontSize = 11.sp, color = themeTextFaint)
                            Text(favorites.size.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = themeText, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            // Quick Filter Chips (Professional Polish Style)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Albums", "Artists", "Playlists").forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isSelected) Accents[settings.accentColorIndex]
                                    else if (settings.isGlassEnabled) GlassDarkSurface else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(50)
                                )
                                .bounceClick {
                                    selectedCategory = cat
                                    selectedAlbum = null
                                    selectedArtist = null
                                    selectedPlaylist = null
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) {
                                    if (settings.accentColorIndex == 0) com.example.ui.theme.DeepViolet else Color.Black
                                } else Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (selectedCategory == "All") {
                // 3. RECENTLY PLAYED / LATEST ADDED HORIZONTAL GRID
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recently Played",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeTextMuted
                        )
                        if (recentlyPlayed.isNotEmpty()) {
                            Text(
                                text = "See All",
                                fontSize = 12.sp,
                                color = Accents[settings.accentColorIndex],
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { showRecentlyPlayedScreen = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("recently_played_see_all")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (recentlyPlayed.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .border(1.dp, themeDivider, RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.02f))
                                .padding(vertical = 20.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = themeTextFaint,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No tracks played yet",
                                    color = themeText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Your playback history will appear here",
                                    color = themeTextMuted,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(recentlyPlayed.take(5), key = { it.id }) { song ->
                                val onPlay = remember(song, recentlyPlayed) { { viewModel.playSong(song, recentlyPlayed) } }
                                LatestTrackCard(
                                    song = song,
                                    settings = settings,
                                    onPlay = onPlay
                                )
                            }
                        }
                    }
                }

                // 4. FAVORITES QUICK GRID
                if (favorites.isNotEmpty()) {
                    item {
                        Text(
                            text = "Favorites Grid",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeText
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(favorites, key = { it.id }) { song ->
                                val onPlay = remember(song, favorites) { { viewModel.playSong(song, favorites) } }
                                LatestTrackCard(
                                    song = song,
                                    settings = settings,
                                    onPlay = onPlay
                                )
                            }
                        }
                    }
                }

                // 5. ALL TRACKS LIST
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "All Songs (${songs.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeText
                        )
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(themeCardBg)
                                    .border(
                                        width = 1.dp,
                                        color = themeDivider,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { expanded = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort Options",
                                    tint = Accents[settings.accentColorIndex],
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Sort by: " + when (currentSortOrder) {
                                        SongSortOrder.NEW_ADD -> "New Add"
                                        SongSortOrder.OLD_ADD -> "Old Add"
                                        SongSortOrder.A_Z -> "A-Z"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeText
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = themeTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(themeDialogBg)
                            ) {
                                listOf(
                                    SongSortOrder.NEW_ADD to "New Add",
                                    SongSortOrder.OLD_ADD to "Old Add",
                                    SongSortOrder.A_Z to "A-Z"
                                ).forEach { (order, label) ->
                                    val isSelected = currentSortOrder == order
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = label,
                                                color = if (isSelected) Accents[settings.accentColorIndex] else Color.White,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            )
                                        },
                                        onClick = {
                                            currentSortOrder = order
                                            expanded = false
                                        },
                                        leadingIcon = {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Accents[settings.accentColorIndex],
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (sortedSongs.isEmpty()) {
                    item {
                        EmptyStatePlaceholder(
                            title = "Your library is silent",
                            subtitle = "Press scan to load local tracks!",
                            icon = Icons.Default.MusicNote,
                            actionText = "Scan local files",
                            onAction = onNavigateToScan
                        )
                    }
                } else {
                    items(sortedSongs, key = { it.id }) { song ->
                        val onPlay = remember(song, sortedSongs) { { viewModel.playSong(song, sortedSongs) } }
                        val onFavoriteToggle = remember(song.id, song.isFavorite) { { viewModel.toggleFavorite(song.id, !song.isFavorite) } }
                        SongListItem(
                            song = song,
                            settings = settings,
                            onPlay = onPlay,
                            onFavoriteToggle = onFavoriteToggle,
                            viewModel = viewModel
                        )
                    }
                }
            } else if (selectedCategory == "Albums") {
                if (selectedAlbum == null) {
                    item {
                        Text(
                            text = "Albums (${albums.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeText
                        )
                    }

                    if (albums.isEmpty()) {
                        item {
                            EmptyStatePlaceholder(
                                title = "No albums found",
                                subtitle = "Scan some music files to see their albums here.",
                                icon = Icons.Default.Album
                            )
                        }
                    } else {
                        items(albums, key = { it }) { albumName ->
                            val currentAlbumSongs = remember(songs, albumName) {
                                songs.filter { it.album == albumName }
                            }
                            val primaryArtist = currentAlbumSongs.firstOrNull()?.artist ?: "Unknown Artist"
                            AlbumListItem(
                                album = albumName,
                                artist = primaryArtist,
                                trackCount = currentAlbumSongs.size,
                                settings = settings,
                                onClick = { selectedAlbum = albumName }
                            )
                        }
                    }
                } else {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = { selectedAlbum = null }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = themeText)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = selectedAlbum ?: "Album Details",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${albumSongs.size} ${if (albumSongs.size == 1) "Song" else "Songs"}",
                                    fontSize = 12.sp,
                                    color = themeTextMuted
                                )
                            }
                        }
                    }

                    items(albumSongs, key = { it.id }) { song ->
                        val onPlay = remember(song, albumSongs) { { viewModel.playSong(song, albumSongs) } }
                        val onFavoriteToggle = remember(song.id, song.isFavorite) { { viewModel.toggleFavorite(song.id, !song.isFavorite) } }
                        SongListItem(
                            song = song,
                            settings = settings,
                            onPlay = onPlay,
                            onFavoriteToggle = onFavoriteToggle,
                            viewModel = viewModel
                        )
                    }
                }
            } else if (selectedCategory == "Artists") {
                if (selectedArtist == null) {
                    item {
                        Text(
                            text = "Artists (${artists.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeText
                        )
                    }

                    if (artists.isEmpty()) {
                        item {
                            EmptyStatePlaceholder(
                                title = "No artists found",
                                subtitle = "Scan some music files to see artists here.",
                                icon = Icons.Default.Person
                            )
                        }
                    } else {
                        items(artists, key = { it }) { artistName ->
                            val currentArtistSongs = remember(songs, artistName) {
                                songs.filter { it.artist == artistName }
                            }
                            ArtistListItem(
                                artist = artistName,
                                trackCount = currentArtistSongs.size,
                                settings = settings,
                                onClick = { selectedArtist = artistName }
                            )
                        }
                    }
                } else {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = { selectedArtist = null }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = themeText)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = selectedArtist ?: "Artist Details",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${artistSongs.size} ${if (artistSongs.size == 1) "Song" else "Songs"}",
                                    fontSize = 12.sp,
                                    color = themeTextMuted
                                )
                            }
                        }
                    }

                    items(artistSongs, key = { it.id }) { song ->
                        val onPlay = remember(song, artistSongs) { { viewModel.playSong(song, artistSongs) } }
                        val onFavoriteToggle = remember(song.id, song.isFavorite) { { viewModel.toggleFavorite(song.id, !song.isFavorite) } }
                        SongListItem(
                            song = song,
                            settings = settings,
                            onPlay = onPlay,
                            onFavoriteToggle = onFavoriteToggle,
                            viewModel = viewModel
                        )
                    }
                }
            } else if (selectedCategory == "Playlists") {
                if (selectedPlaylist == null) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Playlists (${playlists.size})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeText
                            )
                        }
                    }

                    if (playlists.isEmpty()) {
                        item {
                            EmptyStatePlaceholder(
                                title = "No playlists found",
                                subtitle = "Go to the Playlists screen or use options to group your tracks.",
                                icon = Icons.Default.QueueMusic
                            )
                        }
                    } else {
                        items(playlists, key = { it.id }) { playlist ->
                            PlaylistListItemHome(
                                playlist = playlist,
                                settings = settings,
                                onClick = { selectedPlaylist = playlist },
                                onDelete = { viewModel.deletePlaylist(playlist) }
                            )
                        }
                    }
                } else {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = { selectedPlaylist = null }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = themeText)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = selectedPlaylist?.name ?: "Playlist Details",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${playlistSongs.size} ${if (playlistSongs.size == 1) "Song" else "Songs"}",
                                    fontSize = 12.sp,
                                    color = themeTextMuted
                                )
                            }
                        }
                    }

                    if (playlistSongs.isEmpty()) {
                        item {
                            EmptyStatePlaceholder(
                                title = "This playlist is empty",
                                subtitle = "Add songs to this playlist from the song options.",
                                icon = Icons.Default.MusicNote
                            )
                        }
                    } else {
                        items(playlistSongs, key = { it.id }) { song ->
                            val onPlay = remember(song, playlistSongs) { { viewModel.playSong(song, playlistSongs) } }
                            val onFavoriteToggle = remember(song.id, song.isFavorite) { { viewModel.toggleFavorite(song.id, !song.isFavorite) } }
                            SongListItem(
                                song = song,
                                settings = settings,
                                onPlay = onPlay,
                                onFavoriteToggle = onFavoriteToggle,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
        
    AnimatedVisibility(
        visible = showRecentlyPlayedScreen,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        RecentlyPlayedScreen(
            viewModel = viewModel,
            settings = settings,
            onClose = { showRecentlyPlayedScreen = false }
        )
    }
}
}

// ---------------- FAVORITES SCREEN ----------------
@Composable
fun FavoritesScreenContent(viewModel: MainViewModel) {
    val favorites by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Your Favorites",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = themeText
        )
        Text(
            text = "Songs you've bookmarked offline.",
            fontSize = 12.sp,
            color = themeTextMuted,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
        )

        if (favorites.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                EmptyStatePlaceholder(
                    title = "No Favorites yet",
                    subtitle = "Tap the heart icon in the player or list to add tracks here.",
                    icon = Icons.Default.FavoriteBorder
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favorites, key = { it.id }) { song ->
                    val onPlay = remember(song, favorites) { { viewModel.playSong(song, favorites) } }
                    val onFavoriteToggle = remember(song.id, song.isFavorite) { { viewModel.toggleFavorite(song.id, !song.isFavorite) } }
                    SongListItem(
                        song = song,
                        settings = settings,
                        onPlay = onPlay,
                        onFavoriteToggle = onFavoriteToggle,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

// ---------------- PLAYLISTS / FOLDERS SCREEN ----------------
sealed interface LibraryDetailScreen {
    data class PlaylistDetails(val playlist: Playlist) : LibraryDetailScreen
    data class FolderDetails(val folderName: String) : LibraryDetailScreen
}

@Composable
fun PlaylistsScreenContent(viewModel: MainViewModel) {
    val songs by viewModel.allSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    
    // Group songs by folder name for folders tab
    val folders = remember(songs) {
        songs.groupBy { it.folderName }
    }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Playlists, 1 = Folders
    var activeDetailScreen by remember { mutableStateOf<LibraryDetailScreen?>(null) }

    BackHandler(enabled = activeDetailScreen != null) {
        activeDetailScreen = null
    }

    AnimatedContent(
        targetState = activeDetailScreen,
        transitionSpec = {
            if (targetState != null) {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                    slideOutHorizontally { width -> -width } + fadeOut()
            } else {
                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                    slideOutHorizontally { width -> width } + fadeOut()
            }
        },
        label = "LibraryScreenTransition"
    ) { screen ->
        when (screen) {
            null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Your Library",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themeText
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Custom tabs row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LibraryTabButton(
                            text = "Playlists",
                            isActive = selectedTab == 0,
                            accentColor = Accents[settings.accentColorIndex],
                            onClick = { selectedTab = 0 }
                        )
                        LibraryTabButton(
                            text = "Folders",
                            isActive = selectedTab == 1,
                            accentColor = Accents[settings.accentColorIndex],
                            onClick = { selectedTab = 1 }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedTab == 0) {
                        // Playlists Tab
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Custom Playlists",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeText
                            )
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Accents[settings.accentColorIndex].copy(alpha = 0.15f),
                                    contentColor = Accents[settings.accentColorIndex]
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                                Spacer(Modifier.width(4.dp))
                                Text("New", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (playlists.isEmpty()) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                EmptyStatePlaceholder(
                                    title = "No Playlists",
                                    subtitle = "Create your custom offline music collections.",
                                    icon = Icons.AutoMirrored.Filled.List
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(bottom = 120.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(playlists, key = { it.id }) { playlist ->
                                    val onDelete = remember(playlist) { { viewModel.deletePlaylist(playlist) } }
                                    PlaylistListItem(
                                        playlist = playlist,
                                        settings = settings,
                                        onDelete = onDelete,
                                        onClick = { activeDetailScreen = LibraryDetailScreen.PlaylistDetails(playlist) }
                                    )
                                }
                            }
                        }
                    } else {
                        // Folders Tab
                        if (folders.isEmpty()) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                EmptyStatePlaceholder(
                                    title = "No folders identified",
                                    subtitle = "Scan your device downloads or documents for audios.",
                                    icon = Icons.Default.FolderOpen
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(bottom = 120.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(folders.keys.toList(), key = { it }) { folderName ->
                                    val folderSongs = folders[folderName] ?: emptyList()
                                    val onPlayFolder = remember(folderName, folderSongs) { { viewModel.playQueue(folderSongs, 0) } }
                                    FolderListItem(
                                        name = folderName,
                                        songsCount = folderSongs.size,
                                        settings = settings,
                                        onPlayFolder = onPlayFolder,
                                        onClick = { activeDetailScreen = LibraryDetailScreen.FolderDetails(folderName) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            is LibraryDetailScreen.PlaylistDetails -> {
                // If the playlist gets deleted by user, go back safely
                val currentPlaylist = playlists.find { it.id == screen.playlist.id }
                if (currentPlaylist != null) {
                    PlaylistDetailsScreen(
                        playlist = currentPlaylist,
                        viewModel = viewModel,
                        onBack = { activeDetailScreen = null }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        activeDetailScreen = null
                    }
                }
            }
            is LibraryDetailScreen.FolderDetails -> {
                FolderDetailsScreen(
                    folderName = screen.folderName,
                    viewModel = viewModel,
                    onBack = { activeDetailScreen = null }
                )
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Offline Playlist", color = themeText) },
            text = {
                TextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("e.g. Coding Beats", color = themeTextFaint) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = themeText,
                        unfocusedTextColor = themeText,
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName.trim())
                            newPlaylistName = ""
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Create", color = Accents[settings.accentColorIndex], fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = themeTextMuted)
                }
            },
            containerColor = themeDialogBg
        )
    }
}

@Composable
fun AddSongsToPlaylistDialog(
    playlist: Playlist,
    playlistSongs: List<Song>,
    viewModel: MainViewModel,
    settings: BeatFlowSettings,
    onDismiss: () -> Unit
) {
    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    
    val isDark = LocalIsDarkMode.current
    val itemText = if (isDark) Color.White else Color(0xFF121212)
    val itemTextMuted = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF121212).copy(alpha = 0.6f)
    val itemDialogBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Songs to '${playlist.name}'", color = itemText, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (allSongs.isEmpty()) {
                    Text("No songs found in your library to add.", color = itemTextMuted, fontSize = 14.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allSongs, key = { it.id }) { song ->
                            val isAlreadyInPlaylist = remember(playlistSongs, song.id) {
                                playlistSongs.any { it.id == song.id }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(song.title, color = itemText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(song.artist, color = itemTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                if (isAlreadyInPlaylist) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Added",
                                        tint = NeonGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    IconButton(
                                        onClick = {
                                            viewModel.addSongToPlaylist(playlist.id, song.id)
                                            android.widget.Toast.makeText(context, "Added '${song.title}' to playlist", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add",
                                            tint = Accents[settings.accentColorIndex],
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = itemTextMuted)
            }
        },
        containerColor = itemDialogBg
    )
}

@Composable
fun PlaylistDetailsScreen(
    playlist: Playlist,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val playlistSongs by remember(playlist.id) {
        viewModel.getSongsInPlaylist(playlist.id)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val accentColor = Accents[settings.accentColorIndex]

    var showAddSongsDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var renameValue by remember(playlist.name) { mutableStateOf(playlist.name) }

    val totalDurationMs = remember(playlistSongs) {
        playlistSongs.sumOf { it.duration }
    }
    val totalDurationText = remember(totalDurationMs) {
        val totalSeconds = totalDurationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    if (showAddSongsDialog) {
        AddSongsToPlaylistDialog(
            playlist = playlist,
            playlistSongs = playlistSongs,
            viewModel = viewModel,
            settings = settings,
            onDismiss = { showAddSongsDialog = false }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Playlist", color = themeText) },
            text = {
                TextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = themeText,
                        unfocusedTextColor = themeText,
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameValue.isNotBlank()) {
                            viewModel.renamePlaylist(playlist, renameValue.trim())
                            showRenameDialog = false
                        }
                    }
                ) {
                    Text("Save", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = themeTextMuted)
                }
            },
            containerColor = themeDialogBg
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Playlist", color = themeText) },
            text = { Text("Are you sure you want to delete '${playlist.name}'? This cannot be undone.", color = themeTextMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlaylist(playlist)
                        showDeleteConfirmDialog = false
                        onBack()
                    }
                ) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = themeTextMuted)
                }
            },
            containerColor = themeDialogBg
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = themeText)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Playlist Details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeText
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { showRenameDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = themeTextMuted)
                }
                IconButton(onClick = { showDeleteConfirmDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Playlist Summary Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(themeCardBg)
                .border(1.dp, themeCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = playlist.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = themeText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${playlistSongs.size} songs • $totalDurationText",
                    fontSize = 12.sp,
                    color = themeTextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick add action header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tracks",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = themeText
            )
            if (playlistSongs.isNotEmpty()) {
                Button(
                    onClick = { showAddSongsDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.15f),
                        contentColor = accentColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (playlistSongs.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyStatePlaceholder(
                    title = "This Playlist is empty",
                    subtitle = "Curate your perfect vibe by adding tracks from your library.",
                    icon = Icons.AutoMirrored.Filled.List,
                    actionText = "Add Songs",
                    onAction = { showAddSongsDialog = true }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(playlistSongs, key = { it.id }) { song ->
                    val onPlay = remember(song, playlistSongs) { { viewModel.playSong(song, playlistSongs) } }
                    val onFavoriteToggle = remember(song.id, song.isFavorite) { { viewModel.toggleFavorite(song.id, !song.isFavorite) } }
                    val onRemove = remember(playlist.id, song.id) {
                        {
                            viewModel.removeSongFromPlaylist(playlist.id, song.id)
                        }
                    }
                    SongListItem(
                        song = song,
                        settings = settings,
                        onPlay = onPlay,
                        onFavoriteToggle = onFavoriteToggle,
                        viewModel = viewModel,
                        onRemoveFromPlaylist = onRemove
                    )
                }
            }
        }
    }
}

@Composable
fun FolderDetailsScreen(
    folderName: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val songs by viewModel.allSongs.collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val accentColor = Accents[settings.accentColorIndex]

    val folderSongs = remember(songs, folderName) {
        songs.filter { it.folderName == folderName }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = themeText)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Folder Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = themeText
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Folder Summary Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(themeCardBg)
                .border(1.dp, themeCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = folderName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = themeText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${folderSongs.size} audio files",
                    fontSize = 12.sp,
                    color = themeTextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Files",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = themeText
            )
            if (folderSongs.isNotEmpty()) {
                Button(
                    onClick = { viewModel.playQueue(folderSongs, 0) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.15f),
                        contentColor = accentColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Play All", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (folderSongs.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyStatePlaceholder(
                    title = "No music found",
                    subtitle = "This folder contains no supported audio tracks.",
                    icon = Icons.Default.FolderOpen
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(folderSongs, key = { it.id }) { song ->
                    val onPlay = remember(song, folderSongs) { { viewModel.playSong(song, folderSongs) } }
                    val onFavoriteToggle = remember(song.id, song.isFavorite) { { viewModel.toggleFavorite(song.id, !song.isFavorite) } }
                    SongListItem(
                        song = song,
                        settings = settings,
                        onPlay = onPlay,
                        onFavoriteToggle = onFavoriteToggle,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryTabButton(text: String, isActive: Boolean, accentColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) accentColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f))
            .border(1.dp, if (isActive) accentColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            color = if (isActive) accentColor else Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp
        )
    }
}

fun loadDownscaledBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val contentResolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { decoder, info, _ ->
                decoder.isMutableRequired = true
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                if (info.size.width > 1200 || info.size.height > 1200) {
                    val ratio = info.size.width.toFloat() / info.size.height
                    if (ratio > 1f) {
                        decoder.setTargetSize(1200, (1200 / ratio).toInt())
                    } else {
                        decoder.setTargetSize((1200 * ratio).toInt(), 1200)
                    }
                }
            }
        } else {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            var sampleSize = 1
            while (options.outWidth / sampleSize > 1200 || options.outHeight / sampleSize > 1200) {
                sampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun cropAndSaveBitmap(
    context: Context,
    original: Bitmap,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    viewportSizePx: Float
): String? {
    return try {
        val targetSize = 512
        val cropped = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(cropped)
        
        val w = original.width.toFloat()
        val h = original.height.toFloat()
        val baseScale = targetSize / w.coerceAtMost(h)
        
        val scaleMultiplier = targetSize / viewportSizePx
        val canvasOffsetX = offsetX * scaleMultiplier
        val canvasOffsetY = offsetY * scaleMultiplier
        
        val matrix = Matrix()
        matrix.postTranslate(-w / 2f, -h / 2f)
        val finalScale = baseScale * scale
        matrix.postScale(finalScale, finalScale)
        matrix.postTranslate(targetSize / 2f, targetSize / 2f)
        matrix.postTranslate(canvasOffsetX, canvasOffsetY)
        
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(original, matrix, paint)
        
        val outputDir = context.filesDir
        val file = File(outputDir, "profile_cropped_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        
        cropped.recycle()
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun ImageCropperDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    onCropped: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(uri) {
        isLoading = true
        withContext(Dispatchers.IO) {
            bitmap = loadDownscaledBitmap(context, uri)
        }
        isLoading = false
    }
    
    if (isLoading) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {},
            title = { Text("Loading Image...", color = themeText) },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = themeDialogBg
        )
    } else if (bitmap == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("OK", color = themeText)
                }
            },
            title = { Text("Error", color = themeText) },
            text = { Text("Unable to load the selected image.", color = themeText.copy(alpha = 0.7f)) },
            containerColor = themeDialogBg
        )
    } else {
        val loadedBitmap = bitmap!!
        val canvasStrokeColor = themeText
        
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
        
        val viewportSizeDp = 280.dp
        val density = LocalDensity.current
        val viewportSizePx = remember(density) { with(density) { viewportSizeDp.toPx() } }
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Crop Profile Picture",
                    color = themeText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Drag to position, pinch or use slider to zoom",
                        color = themeTextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(viewportSizeDp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    val maxPanX = (scale - 1f) * viewportSizePx / 2f
                                    val maxPanY = (scale - 1f) * viewportSizePx / 2f
                                    offset = androidx.compose.ui.geometry.Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxPanX - 100f, maxPanX + 100f),
                                        y = (offset.y + pan.y).coerceIn(-maxPanY - 100f, maxPanY + 100f)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = remember(loadedBitmap) { loadedBitmap.asImageBitmap() },
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                ),
                            contentScale = ContentScale.Fit
                        )
                        
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val circleRadius = canvasWidth.coerceAtMost(canvasHeight) / 2f * 0.9f
                            
                            val path = androidx.compose.ui.graphics.Path().apply {
                                addRect(androidx.compose.ui.geometry.Rect(0f, 0f, canvasWidth, canvasHeight))
                            }
                            val circlePath = androidx.compose.ui.graphics.Path().apply {
                                addOval(
                                    androidx.compose.ui.geometry.Rect(
                                        center = androidx.compose.ui.geometry.Offset(canvasWidth / 2f, canvasHeight / 2f),
                                        radius = circleRadius
                                    )
                                )
                            }
                            
                            val differencePath = androidx.compose.ui.graphics.Path.combine(
                                operation = androidx.compose.ui.graphics.PathOperation.Difference,
                                path1 = path,
                                path2 = circlePath
                            )
                            
                            drawPath(
                                path = differencePath,
                                color = Color.Black.copy(alpha = 0.7f)
                            )
                            
                            drawCircle(
                                color = canvasStrokeColor,
                                radius = circleRadius,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = themeTextMuted)
                        Slider(
                            value = scale,
                            onValueChange = { scale = it },
                            valueRange = 1f..5f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                            )
                        )
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = themeTextMuted)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val savedPath = withContext(Dispatchers.IO) {
                                cropAndSaveBitmap(
                                    context = context,
                                    original = loadedBitmap,
                                    scale = scale,
                                    offsetX = offset.x,
                                    offsetY = offset.y,
                                    viewportSizePx = viewportSizePx
                                )
                            }
                            isLoading = false
                            if (savedPath != null) {
                                onCropped(savedPath)
                            }
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save Photo", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = themeTextMuted)
                }
            },
            containerColor = themeDialogBg,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// ---------------- PROFILE SCREEN & STATS ----------------
@Composable
fun ProfileScreenContent(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val songs by viewModel.allSongs.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val statsList by viewModel.listeningStats.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(settings.userName) }
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }
    var cropImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flag)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            cropImageUri = uri
        }
    }

    // Calculate aggregated statistics
    val totalPlayCount = remember(songs) { songs.sumOf { it.playCount } }
    val uniqueArtists = remember(songs) { songs.map { it.artist }.distinct().count() }
    val uniqueAlbums = remember(songs) { songs.map { it.album }.distinct().count() }
    
    val totalListenTimeMs = remember(statsList) { statsList.sumOf { it.listeningTimeMs } }
    val totalListenTimeSec = totalListenTimeMs / 1000
    val formatListenTime = remember(totalListenTimeSec) {
        val h = totalListenTimeSec / 3600
        val m = (totalListenTimeSec % 3600) / 60
        val s = totalListenTimeSec % 60
        if (h > 0) "${h}h ${m}m" else if (m > 0) "${m}m ${s}s" else "${s}s"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "My Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themeText
            )
        }

        // Profile Avatar + Name edit Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                isGlassEnabled = settings.isGlassEnabled,
                isDark = settings.isDarkMode
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar Selector with Image picker
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clickable { showPhotoOptionsDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileAvatar(
                            profileImagePath = settings.profileImagePath,
                            userName = settings.userName,
                            accentColorIndex = settings.accentColorIndex,
                            size = 72.dp,
                            fontSize = 28.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Edit profile picture", tint = themeText, modifier = Modifier.size(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        if (isEditingName) {
                            TextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (editedName.trim().isNotBlank()) {
                                        viewModel.saveUserName(editedName.trim())
                                        isEditingName = false
                                    }
                                }),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = themeText,
                                    unfocusedTextColor = themeText
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = settings.userName,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeText
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Name",
                                    tint = Accents[settings.accentColorIndex],
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            editedName = settings.userName
                                            isEditingName = true
                                        }
                                )
                            }
                            Text(
                                text = "Premium BeatFlow Listener",
                                fontSize = 12.sp,
                                color = themeTextMuted
                            )
                        }
                    }
                }
            }
        }

        // Music Statistics Section
        item {
            Text(
                text = "Music Statistics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = themeText
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Grid layout for stats
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard("Total Tracks", songs.size.toString(), Icons.Default.MusicNote, settings)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard("Listening Time", formatListenTime, Icons.Default.Schedule, settings)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard("Unique Artists", uniqueArtists.toString(), Icons.Default.Group, settings)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard("Unique Albums", uniqueAlbums.toString(), Icons.Default.Album, settings)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard("Total Plays", totalPlayCount.toString(), Icons.Default.PlayArrow, settings)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard("Favorites", favorites.size.toString(), Icons.Default.Favorite, settings)
                    }
                }
            }
        }




    }

    if (showPhotoOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoOptionsDialog = false },
            title = { Text("Profile Photo", color = themeText) },
            text = { Text("Choose an option to update your profile picture.", color = themeText.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPhotoOptionsDialog = false
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Text("Choose from Gallery", color = Accents[settings.accentColorIndex], fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    if (settings.profileImagePath.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                showPhotoOptionsDialog = false
                                viewModel.updateProfileImage("")
                            }
                        ) {
                            Text("Remove Photo", color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TextButton(onClick = { showPhotoOptionsDialog = false }) {
                        Text("Cancel", color = themeTextMuted)
                    }
                }
            },
            containerColor = themeDialogBg
        )
    }

    if (cropImageUri != null) {
        ImageCropperDialog(
            uri = cropImageUri!!,
            onDismiss = { cropImageUri = null },
            onCropped = { croppedPath ->
                if (settings.profileImagePath.isNotEmpty() && settings.profileImagePath.startsWith(context.filesDir.absolutePath)) {
                    try {
                        val oldFile = File(settings.profileImagePath)
                        if (oldFile.exists()) {
                            oldFile.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                viewModel.updateProfileImage(croppedPath)
            }
        )
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, settings: BeatFlowSettings) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        isGlassEnabled = settings.isGlassEnabled,
        isDark = settings.isDarkMode
    ) {
        Column {
            Icon(imageVector = icon, contentDescription = null, tint = Accents[settings.accentColorIndex], modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = themeText)
            Text(text = label, fontSize = 11.sp, color = themeTextFaint, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

// ---------------- SETTINGS SCREEN ----------------
@Composable
fun SettingsScreenContent(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val accentColor = Accents[settings.accentColorIndex]
    val contrastingThumbColor = remember(settings.accentColorIndex) {
        val r = accentColor.red
        val g = accentColor.green
        val b = accentColor.blue
        val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
        if (luminance > 0.5f) Color(0xFF121212) else Color.White
    }

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

    val permissionToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermissionState by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permissionToCheck) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissionState = ContextCompat.checkSelfPermission(context, permissionToCheck) == PackageManager.PERMISSION_GRANTED
    }

    var folderInputPath by remember(settings.customScanFolderPath) { mutableStateOf(settings.customScanFolderPath) }
    val showMusicScannerScreen by viewModel.showScanOverlay.collectAsStateWithLifecycle()
    var showAboutDeveloperScreen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themeText
            )
        }

        // 1. APPEARANCE
        item {
            Text("Appearance", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeText)
            Spacer(modifier = Modifier.height(12.dp))
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                isGlassEnabled = settings.isGlassEnabled,
                isDark = settings.isDarkMode
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Dark Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark Mode", color = themeText, fontSize = 14.sp)
                        Switch(
                            checked = settings.isDarkMode,
                            onCheckedChange = { viewModel.updateThemeSettings(it, settings.isAmoledMode, settings.isGlassEnabled, settings.accentColorIndex) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = contrastingThumbColor,
                                checkedTrackColor = accentColor
                            )
                        )
                    }

                    // AMOLED Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AMOLED Pitch Black", color = themeText, fontSize = 14.sp)
                        Switch(
                            checked = settings.isAmoledMode,
                            onCheckedChange = { viewModel.updateThemeSettings(settings.isDarkMode, it, settings.isGlassEnabled, settings.accentColorIndex) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = contrastingThumbColor,
                                checkedTrackColor = accentColor
                            )
                        )
                    }

                    // Accent Colors Row Selector
                    Column {
                        Text("Glow Accent Color", color = themeTextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Accents.forEachIndexed { index, color ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (settings.accentColorIndex == index) 3.dp else 0.dp,
                                            color = themeText,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            viewModel.updateThemeSettings(
                                                settings.isDarkMode,
                                                settings.isAmoledMode,
                                                settings.isGlassEnabled,
                                                index
                                            )
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. LIBRARY SCANNER (CONCISE SINGLE OPTION ENTRY)
        item {
            Text("Library", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeText)
            Spacer(modifier = Modifier.height(12.dp))
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                isGlassEnabled = settings.isGlassEnabled,
                isDark = settings.isDarkMode
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setShowScanOverlay(true) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Accents[settings.accentColorIndex].copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Accents[settings.accentColorIndex]
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Music Scanner & Filters", color = themeText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Manage directories, file rules, and rescan audio", color = themeTextMuted, fontSize = 11.sp)
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Navigate",
                        tint = themeTextFaint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }





        // 3. PLAYBACK speed
        item {
            Text("Playback", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeText)
            Spacer(modifier = Modifier.height(12.dp))
            val currentSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                isGlassEnabled = settings.isGlassEnabled,
                isDark = settings.isDarkMode
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Playback Speed", color = themeText, fontSize = 14.sp)
                        Text("${"%.2f".format(currentSpeed)}x", color = Accents[settings.accentColorIndex], fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = currentSpeed,
                        onValueChange = { viewModel.setPlaybackSpeed(it.coerceIn(0.5f, 2.0f)) },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Accents[settings.accentColorIndex],
                            activeTrackColor = Accents[settings.accentColorIndex]
                        )
                    )
                }
            }
        }

        // 4. ABOUT & LEGAL
        item {
            Text("About BeatFlow", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeText)
            Spacer(modifier = Modifier.height(12.dp))
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                isGlassEnabled = settings.isGlassEnabled,
                isDark = settings.isDarkMode
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Offline Architecture", fontWeight = FontWeight.Bold, color = themeText, fontSize = 14.sp)
                    Text("This app operates exclusively as a client-side database. It contains zero background servers, cloud modules, API keys, tracking SDKs, or promotional items.", color = themeTextMuted, fontSize = 12.sp)
                    HorizontalDivider(color = themeDivider)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Product Version", color = themeTextMuted, fontSize = 12.sp)
                        Text("v1.0.2", color = themeText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = themeDivider)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://beatflowmusic.vercel.app/"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "No web browser found on this device.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Website", color = themeText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open Website",
                            tint = Accents[settings.accentColorIndex],
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 5. DEVELOPER
        item {
            Text("Developer", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeText)
            Spacer(modifier = Modifier.height(12.dp))
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                isGlassEnabled = settings.isGlassEnabled,
                isDark = settings.isDarkMode
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAboutDeveloperScreen = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Accents[settings.accentColorIndex].copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Accents[settings.accentColorIndex]
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("About Developer", color = themeText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Learn more about Mohan Singh Parmar", color = themeTextMuted, fontSize = 11.sp)
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Navigate",
                        tint = themeTextFaint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

        // Dedicated sub-page overlay for About Developer
        if (showAboutDeveloperScreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (!settings.isDarkMode) LightCanvas else if (settings.isAmoledMode) Color.Black else Color(0xFF121212))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header with back navigation button
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showAboutDeveloperScreen = false },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(themeCardBg)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = themeText
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "About Developer",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeText
                            )
                        }
                    }

                    // Developer Information Card (Unchanged functionality, design, typography, spacing, icons)
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            isGlassEnabled = settings.isGlassEnabled,
                            isDark = settings.isDarkMode
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Mohan Singh Parmar",
                                    fontWeight = FontWeight.Bold,
                                    color = themeText,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Android Developer • Cybersecurity & AI Enthusiast",
                                    fontSize = 12.sp,
                                    color = themeTextMuted,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                                )

                                // Clickable GitHub Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(themeCardBg)
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/monxcode"))
                                            context.startActivity(intent)
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = "GitHub Icon",
                                        tint = Accents[settings.accentColorIndex],
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "GitHub Row: @monxcode",
                                        fontWeight = FontWeight.SemiBold,
                                        color = themeText,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = themeTextFaint, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dedicated sub-page overlay for Music Scanner
        if (showMusicScannerScreen) {
            val folderPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                if (uri != null) {
                    viewModel.triggerDocumentFolderScan(uri.toString())
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (!settings.isDarkMode) LightCanvas else if (settings.isAmoledMode) Color.Black else Color(0xFF121212))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header with back navigation button
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.setShowScanOverlay(false) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(themeCardBg)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = themeText
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Music Scanner",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeText
                            )
                        }
                    }

                    // 1. ACTIVE PROGRESS & CONTROLS SCREEN
                    item {
                        val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
                        val progress by viewModel.scanProgress.collectAsStateWithLifecycle()
                        val filesFound by viewModel.scanFilesFound.collectAsStateWithLifecycle()
                        val scanStatus by viewModel.scanStatus.collectAsStateWithLifecycle()

                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            isGlassEnabled = settings.isGlassEnabled,
                            isDark = settings.isDarkMode
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            ) {
                                if (isScanning) {
                                    Box(
                                        modifier = Modifier.size(160.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { progress / 100f },
                                            modifier = Modifier.size(140.dp),
                                            color = Accents[settings.accentColorIndex],
                                            strokeWidth = 10.dp,
                                            trackColor = Color.White.copy(alpha = 0.1f)
                                        )
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "$progress%",
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Black,
                                                color = themeText
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Scanning",
                                                fontSize = 11.sp,
                                                color = themeTextMuted,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Text(
                                        text = scanStatus,
                                        color = themeText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(themeCardBg)
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AudioFile,
                                            contentDescription = null,
                                            tint = Accents[settings.accentColorIndex],
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "$filesFound new files discovered",
                                            color = themeText.copy(alpha = 0.8f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                } else {
                                    val isCompleted = scanStatus.startsWith("Scan Completed!")
                                    val isError = scanStatus.startsWith("Error:")
                                    
                                    if (isCompleted) {
                                        if (filesFound > 0) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                tint = Color(0xFF4CAF50),
                                                contentDescription = null,
                                                modifier = Modifier.size(72.dp)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "$filesFound songs imported successfully",
                                                color = themeText,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Your library has been successfully updated with new tracks.",
                                                color = themeTextMuted,
                                                fontSize = 12.sp,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                tint = Accents[settings.accentColorIndex].copy(alpha = 0.8f),
                                                contentDescription = null,
                                                modifier = Modifier.size(72.dp)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "No music found in the selected folder",
                                                color = themeText,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Make sure the directory contains supported files (MP3, FLAC, WAV, etc.) and is accessible.",
                                                color = themeTextMuted,
                                                fontSize = 12.sp,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        }
                                    } else if (isError) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            tint = Color(0xFFE57373),
                                            contentDescription = null,
                                            modifier = Modifier.size(72.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Scanning Failed",
                                            color = themeText,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = scanStatus.substringAfter("Error:"),
                                            color = Color(0xFFE57373).copy(alpha = 0.9f),
                                            fontSize = 12.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            tint = Accents[settings.accentColorIndex].copy(alpha = 0.6f),
                                            contentDescription = null,
                                            modifier = Modifier.size(72.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = if (scanStatus == "Ready to Scan") "Ready to Scan Device" else scanStatus,
                                            color = themeText,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Search local folders for new tracks.",
                                            color = themeTextMuted,
                                            fontSize = 12.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Button(
                                        onClick = { viewModel.triggerScan() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Accents[settings.accentColorIndex],
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Scan Music Library Now", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 2. STORAGE PERMISSIONS STATUS
                    item {
                        Text(
                            text = "System Permissions & Storage",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            isGlassEnabled = settings.isGlassEnabled,
                            isDark = settings.isDarkMode
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Storage Permission", color = themeText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = if (hasPermissionState) "Permission Granted" else "Access Required to scan audio files",
                                            color = if (hasPermissionState) NeonGreen else HotPink,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background((if (hasPermissionState) NeonGreen else HotPink).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (hasPermissionState) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (hasPermissionState) NeonGreen else HotPink,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                if (!hasPermissionState) {
                                    Button(
                                        onClick = {
                                            permissionLauncher.launch(permissionsToRequest)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = HotPink,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(Icons.Default.LockOpen, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Grant Storage Permission", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(themeCardBg)
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Authorized to load device audio files.", color = themeText.copy(alpha = 0.7f), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 3. COMPACT RULES FILTERING CARDS
                    item {
                        Text(
                            text = "Filter Rules",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            isGlassEnabled = settings.isGlassEnabled,
                            isDark = settings.isDarkMode
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Ignore shorter than 1 min", color = themeText, fontSize = 14.sp)
                                    Switch(
                                        checked = settings.ignoreShorterThan1Min,
                                        onCheckedChange = {
                                            viewModel.updateScanFilters(
                                                it,
                                                settings.ignoreSmallerThan100KB,
                                                settings.ignoreHidden,
                                                settings.ignoreDuplicates,
                                                settings.scanDownloads,
                                                settings.scanSDCard
                                            )
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = contrastingThumbColor,
                                            checkedTrackColor = accentColor
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Ignore smaller than 100 KB", color = themeText, fontSize = 14.sp)
                                    Switch(
                                        checked = settings.ignoreSmallerThan100KB,
                                        onCheckedChange = {
                                            viewModel.updateScanFilters(
                                                settings.ignoreShorterThan1Min,
                                                it,
                                                settings.ignoreHidden,
                                                settings.ignoreDuplicates,
                                                settings.scanDownloads,
                                                settings.scanSDCard
                                            )
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = contrastingThumbColor,
                                            checkedTrackColor = accentColor
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Ignore hidden files/folders", color = themeText, fontSize = 14.sp)
                                    Switch(
                                        checked = settings.ignoreHidden,
                                        onCheckedChange = {
                                            viewModel.updateScanFilters(
                                                settings.ignoreShorterThan1Min,
                                                settings.ignoreSmallerThan100KB,
                                                it,
                                                settings.ignoreDuplicates,
                                                settings.scanDownloads,
                                                settings.scanSDCard
                                            )
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = contrastingThumbColor,
                                            checkedTrackColor = accentColor
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Scan SD Card folders", color = themeText, fontSize = 14.sp)
                                    Switch(
                                        checked = settings.scanSDCard,
                                        onCheckedChange = {
                                            viewModel.updateScanFilters(
                                                settings.ignoreShorterThan1Min,
                                                settings.ignoreSmallerThan100KB,
                                                settings.ignoreHidden,
                                                settings.ignoreDuplicates,
                                                settings.scanDownloads,
                                                it
                                            )
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = contrastingThumbColor,
                                            checkedTrackColor = accentColor
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Scan Downloads directory", color = themeText, fontSize = 14.sp)
                                    Switch(
                                        checked = settings.scanDownloads,
                                        onCheckedChange = {
                                            viewModel.updateScanFilters(
                                                settings.ignoreShorterThan1Min,
                                                settings.ignoreSmallerThan100KB,
                                                settings.ignoreHidden,
                                                settings.ignoreDuplicates,
                                                it,
                                                settings.scanSDCard
                                            )
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = contrastingThumbColor,
                                            checkedTrackColor = accentColor
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // System Folder Browser (SAF)
                    item {
                        Text(
                            text = "System Folder Browser (Recommended)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            isGlassEnabled = settings.isGlassEnabled,
                            isDark = settings.isDarkMode
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    text = "Select any folder from internal storage or SD card. BeatFlow will request persistent permission to recursively import all supported audio tracks.",
                                    color = themeTextMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )

                                Button(
                                    onClick = {
                                        try {
                                            folderPickerLauncher.launch(null)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .glow(Accents[settings.accentColorIndex], radius = 8.dp, alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Accents[settings.accentColorIndex],
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Browse & Scan Folder", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 4. DIRECT SCAN CUSTOM FOLDER
                    item {
                        Text(
                            text = "Direct Folder Scanner (Force Add)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            isGlassEnabled = settings.isGlassEnabled,
                            isDark = settings.isDarkMode
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    text = "Specify a direct directory path to scan files recursively. This bypasses system indexing databases entirely.",
                                    color = themeTextMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )

                                TextField(
                                    value = folderInputPath,
                                    onValueChange = { folderInputPath = it },
                                    label = { Text("Custom Folder Path", color = themeTextFaint) },
                                    placeholder = { Text("e.g. /sdcard/Music", color = themeTextFaint) },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedTextColor = themeText,
                                        unfocusedTextColor = themeText,
                                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                                        focusedIndicatorColor = Accents[settings.accentColorIndex],
                                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.updateCustomScanFolderPath(folderInputPath.trim())
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = ButtonDefaults.outlinedButtonBorder,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save Path", fontSize = 13.sp)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.updateCustomScanFolderPath(folderInputPath.trim())
                                            viewModel.triggerScan()
                                        },
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .glow(Accents[settings.accentColorIndex], radius = 8.dp, alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Accents[settings.accentColorIndex],
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Force Scan Folder", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- AUXILIARY WIDGET COMPONENT HELPERS ----------------

val ProfessionalPolishGradients = listOf(
    Brush.linearGradient(listOf(Color(0xFF4F378B), Color(0xFF21005D))),
    Brush.linearGradient(listOf(Color(0xFF31111D), Color(0xFFFFD8E4))),
    Brush.linearGradient(listOf(Color(0xFF1A365D), Color(0xFF0A192F))),
    Brush.linearGradient(listOf(Color(0xFF2E0854), Color(0xFF4B0082))),
    Brush.linearGradient(listOf(Color(0xFF381E72), Color(0xFFD0BCFF)))
)

@Composable
fun LatestTrackCard(
    song: Song,
    settings: BeatFlowSettings,
    onPlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (settings.isGlassEnabled) (if (settings.isDarkMode) GlassDarkSurface.copy(alpha = 0.1f) else GlassLightSurface.copy(alpha = 0.15f)) else themeCardBg)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .bounceClick(onClick = onPlay)
            .padding(12.dp)
    ) {
        Column {
            // Album Art placeholder with sleek design gradient
            val gradientIdx = (song.id.toInt() % ProfessionalPolishGradients.size).let { if (it < 0) -it else it }
            Box(
                modifier = Modifier
                    .size(106.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ProfessionalPolishGradients[gradientIdx])
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = themeText.copy(alpha = 0.8f),
                    modifier = Modifier.size(36.dp)
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
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = song.title,
                fontWeight = FontWeight.Bold,
                color = themeText,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = themeTextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

fun getSongArtworkModel(song: Song): Any? {
    if (!song.artworkUri.isNullOrEmpty()) {
        return song.artworkUri
    }
    try {
        val file = java.io.File(song.path)
        if (file.exists()) {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(song.path)
            val picture = retriever.embeddedPicture
            retriever.release()
            if (picture != null) {
                return picture
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

fun saveImageToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
    return try {
        val directory = java.io.File(context.filesDir, "artworks")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = java.io.File(directory, fileName)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            java.io.FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        "file://${file.absolutePath}"
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun RenameDialog(
    song: Song,
    settings: BeatFlowSettings,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var tempTitle by remember { mutableStateOf(song.title) }
    var tempArtist by remember { mutableStateOf(song.artist) }
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isArtworkRemoved by remember { mutableStateOf(false) }

    val initialArtworkModel = remember(song) { getSongArtworkModel(song) }

    val previewModel = remember(selectedImageUri, isArtworkRemoved, initialArtworkModel) {
        when {
            isArtworkRemoved -> null
            selectedImageUri != null -> selectedImageUri
            else -> initialArtworkModel
        }
    }

    val isDark = LocalIsDarkMode.current
    val itemText = if (isDark) Color.White else Color(0xFF121212)
    val itemTextMuted = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF121212).copy(alpha = 0.6f)
    val itemDialogBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Music Info", color = itemText, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    label = { Text("Song Title") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = itemText,
                        unfocusedTextColor = itemText,
                        focusedBorderColor = Accents[settings.accentColorIndex],
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = tempArtist,
                    onValueChange = { tempArtist = it },
                    label = { Text("Artist") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = itemText,
                        unfocusedTextColor = itemText,
                        focusedBorderColor = Accents[settings.accentColorIndex],
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Artwork section title
                Text(
                    text = "Artwork",
                    color = itemText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val gradientIdx = remember(song.id) { (song.id.toInt() % ProfessionalPolishGradients.size).let { if (it < 0) -it else it } }
                    
                    // Artwork preview container
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ProfessionalPolishGradients[gradientIdx])
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                        if (previewModel != null) {
                            AsyncImage(
                                model = previewModel,
                                contentDescription = "Artwork Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }

                    // Artwork action buttons
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val galleryLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.GetContent()
                        ) { uri: Uri? ->
                            if (uri != null) {
                                selectedImageUri = uri
                                isArtworkRemoved = false
                            }
                        }

                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Accents[settings.accentColorIndex].copy(alpha = 0.15f),
                                contentColor = Accents[settings.accentColorIndex]
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        if (previewModel != null) {
                            OutlinedButton(
                                onClick = {
                                    selectedImageUri = null
                                    isArtworkRemoved = true
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.Red.copy(alpha = 0.8f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Remove", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (tempTitle.isNotBlank() && tempArtist.isNotBlank()) {
                        val finalArtworkUri = when {
                            isArtworkRemoved -> {
                                // Clean up old cached file if there was one
                                if (!song.artworkUri.isNullOrEmpty() && song.artworkUri.startsWith("file://")) {
                                    try {
                                        val oldPath = song.artworkUri.substring(7)
                                        val oldFile = java.io.File(oldPath)
                                        if (oldFile.exists() && oldFile.absolutePath.contains(context.filesDir.absolutePath)) {
                                            oldFile.delete()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                null
                            }
                            selectedImageUri != null -> {
                                // Clean up old cached file if there was one
                                if (!song.artworkUri.isNullOrEmpty() && song.artworkUri.startsWith("file://")) {
                                    try {
                                        val oldPath = song.artworkUri.substring(7)
                                        val oldFile = java.io.File(oldPath)
                                        if (oldFile.exists() && oldFile.absolutePath.contains(context.filesDir.absolutePath)) {
                                            oldFile.delete()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                val fileName = "artwork_${song.id}_${System.currentTimeMillis()}.jpg"
                                saveImageToInternalStorage(context, selectedImageUri!!, fileName)
                            }
                            else -> song.artworkUri // Keep original
                        }
                        
                        val updatedSong = song.copy(
                            title = tempTitle.trim(),
                            artist = tempArtist.trim(),
                            artworkUri = finalArtworkUri
                        )
                        viewModel.updateSong(updatedSong)
                        android.widget.Toast.makeText(context, "Music updated successfully", android.widget.Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                }
            ) {
                Text("Save", color = Accents[settings.accentColorIndex], fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = itemTextMuted)
            }
        },
        containerColor = itemDialogBg
    )
}

@Composable
fun DeleteDialog(
    song: Song,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkMode.current
    val itemText = if (isDark) Color.White else Color(0xFF121212)
    val itemTextMuted = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF121212).copy(alpha = 0.6f)
    val itemDialogBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove from Library", color = itemText, fontWeight = FontWeight.Bold) },
        text = { Text("Are you sure you want to remove '${song.title}' from your library? The database entry will be deleted, but the physical file won't be deleted.", color = itemText.copy(alpha = 0.7f)) },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.deleteSong(song)
                    android.widget.Toast.makeText(context, "'${song.title}' removed", android.widget.Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            ) {
                Text("Remove", color = Color.Red, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = itemTextMuted)
            }
        },
        containerColor = itemDialogBg
    )
}

@Composable
fun InfoDialog(
    song: Song,
    settings: BeatFlowSettings,
    onDismiss: () -> Unit
) {
    val formattedDuration = remember(song.duration) {
        val totalSeconds = song.duration / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }
    val fileSize = remember(song.path) {
        val file = java.io.File(song.path)
        if (file.exists()) {
            val bytes = file.length()
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            if (mb >= 1.0) {
                String.format("%.2f MB", mb)
            } else {
                String.format("%.2f KB", kb)
            }
        } else {
            "Unknown"
        }
    }
    val isDark = LocalIsDarkMode.current
    val itemText = if (isDark) Color.White else Color(0xFF121212)
    val itemTextMuted = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF121212).copy(alpha = 0.6f)
    val itemTextFaint = if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFF121212).copy(alpha = 0.4f)
    val itemDialogBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Song Information", color = itemText, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Title" to song.title,
                    "Artist" to song.artist,
                    "Album" to song.album,
                    "Duration" to formattedDuration,
                    "File Path" to song.path,
                    "File Size" to fileSize
                ).forEach { (label, value) ->
                    Column {
                        Text(text = label, color = itemTextFaint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = value, color = itemText, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Accents[settings.accentColorIndex])
            ) {
                Text("OK", color = itemText, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = itemDialogBg
    )
}

@Composable
fun SongListItem(
    song: Song,
    settings: BeatFlowSettings,
    onPlay: () -> Unit,
    onFavoriteToggle: () -> Unit,
    viewModel: MainViewModel,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    var expandedMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    
    // Resolve dynamic colors once per item to completely eliminate lookups during scroll
    val isDark = LocalIsDarkMode.current
    val itemText = remember(isDark) { if (isDark) Color.White else Color(0xFF121212) }
    val itemTextMuted = remember(isDark) { if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF121212).copy(alpha = 0.6f) }
    val itemCardBg = remember(isDark) { if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f) }
    val itemCardBorder = remember(isDark) { if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFF121212).copy(alpha = 0.1f) }
    val itemDialogBg = remember(isDark) { if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF) }
    val gradientIdx = remember(song.id) { (song.id.toInt() % ProfessionalPolishGradients.size).let { if (it < 0) -it else it } }

    if (showRenameDialog) {
        RenameDialog(
            song = song,
            settings = settings,
            viewModel = viewModel,
            onDismiss = { showRenameDialog = false }
        )
    }

    if (showDeleteDialog) {
        DeleteDialog(
            song = song,
            viewModel = viewModel,
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showInfoDialog) {
        InfoDialog(
            song = song,
            settings = settings,
            onDismiss = { showInfoDialog = false }
        )
    }

    if (showPlaylistDialog) {
        PlaylistDialog(
            song = song,
            settings = settings,
            viewModel = viewModel,
            onDismiss = { showPlaylistDialog = false }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (settings.isGlassEnabled) GlassDarkSurface.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f))
            .border(1.dp, itemCardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onPlay)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ProfessionalPolishGradients[gradientIdx])
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
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

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontWeight = FontWeight.SemiBold,
                color = itemText,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = itemTextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        IconButton(onClick = onFavoriteToggle) {
            Icon(
                imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite Toggle",
                tint = if (song.isFavorite) HotPink else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }

        Box {
            IconButton(onClick = { expandedMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = itemTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = expandedMenu,
                onDismissRequest = { expandedMenu = false },
                modifier = Modifier.background(itemDialogBg)
            ) {
                DropdownMenuItem(
                    text = { Text("Play Next", color = itemText, fontSize = 13.sp) },
                    onClick = {
                        viewModel.playNext(song)
                        android.widget.Toast.makeText(context, "Playing next: '${song.title}'", android.widget.Toast.LENGTH_SHORT).show()
                        expandedMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = null,
                            tint = Accents[settings.accentColorIndex],
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                DropdownMenuItem(
                    text = { Text("Add to Playlist", color = itemText, fontSize = 13.sp) },
                    onClick = {
                        showPlaylistDialog = true
                        expandedMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = null,
                            tint = Accents[settings.accentColorIndex],
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                if (onRemoveFromPlaylist != null) {
                    DropdownMenuItem(
                        text = { Text("Remove from Playlist", color = Color.Red, fontSize = 13.sp) },
                        onClick = {
                            onRemoveFromPlaylist()
                            expandedMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Song Info", color = itemText, fontSize = 13.sp) },
                    onClick = {
                        showInfoDialog = true
                        expandedMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Accents[settings.accentColorIndex],
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                DropdownMenuItem(
                    text = { Text("Rename", color = itemText, fontSize = 13.sp) },
                    onClick = {
                        showRenameDialog = true
                        expandedMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Accents[settings.accentColorIndex],
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                DropdownMenuItem(
                    text = { Text("Share", color = itemText, fontSize = 13.sp) },
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "audio/*"
                                val file = java.io.File(song.path)
                                if (file.exists()) {
                                    val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "com.example.fileprovider",
                                        file
                                    )
                                    putExtra(Intent.EXTRA_STREAM, fileUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                } else {
                                    putExtra(Intent.EXTRA_TEXT, "Listen to '${song.title}' by ${song.artist}")
                                }
                                putExtra(Intent.EXTRA_SUBJECT, song.title)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Music File"))
                        } catch (e: Exception) {
                            android.util.Log.e("SongListItem", "Failed to share song", e)
                            android.widget.Toast.makeText(context, "Could not share file", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        expandedMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Accents[settings.accentColorIndex],
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                DropdownMenuItem(
                    text = { Text("Remove from Library", color = Color.Red, fontSize = 13.sp) },
                    onClick = {
                        showDeleteDialog = true
                        expandedMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun PlaylistDialog(
    song: Song,
    settings: BeatFlowSettings,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsStateWithLifecycle(initialValue = emptyList())
    var newPlaylistName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist", color = themeText, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("New Playlist Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = themeText,
                            unfocusedTextColor = themeText,
                            focusedBorderColor = Accents[settings.accentColorIndex],
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                viewModel.createPlaylistAndAddSong(newPlaylistName.trim(), song.id)
                                android.widget.Toast.makeText(context, "Created & added to '${newPlaylistName.trim()}'", android.widget.Toast.LENGTH_SHORT).show()
                                newPlaylistName = ""
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Accents[settings.accentColorIndex].copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create and Add", tint = Accents[settings.accentColorIndex])
                    }
                }

                HorizontalDivider(color = themeCardBgSelected)

                Text("Or select existing:", color = themeTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                if (playlists.isEmpty()) {
                    Text("No playlists created yet.", color = themeTextFaint, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .clickable {
                                        viewModel.addSongToPlaylist(playlist.id, song.id)
                                        android.widget.Toast.makeText(context, "Added to '${playlist.name}'", android.widget.Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.QueueMusic, contentDescription = null, tint = Accents[settings.accentColorIndex], modifier = Modifier.size(18.dp))
                                Text(playlist.name, color = themeText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = themeTextMuted)
            }
        },
        containerColor = themeDialogBg
    )
}

@Composable
fun PlaylistListItem(
    playlist: Playlist,
    settings: BeatFlowSettings,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(themeCardBg)
            .border(1.dp, themeCardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                tint = Accents[settings.accentColorIndex],
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(playlist.name, fontWeight = FontWeight.Bold, color = themeText, fontSize = 14.sp)
                Text("Offline collection", color = themeTextFaint, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Playlist", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun FolderListItem(
    name: String,
    songsCount: Int,
    settings: BeatFlowSettings,
    onPlayFolder: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(themeCardBg)
            .border(1.dp, themeCardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = Accents[settings.accentColorIndex],
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(name, fontWeight = FontWeight.Bold, color = themeText, fontSize = 14.sp)
                Text("$songsCount tracks inside", color = themeTextFaint, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        IconButton(onClick = onPlayFolder) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play Folder",
                tint = Accents[settings.accentColorIndex],
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun EmptyStatePlaceholder(
    title: String,
    subtitle: String,
    icon: ImageVector,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = themeText.copy(alpha = 0.2f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = themeText.copy(alpha = 0.8f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            color = themeTextFaint,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp),
            lineHeight = 18.sp
        )
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(actionText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun AlbumListItem(
    album: String,
    artist: String,
    trackCount: Int,
    settings: BeatFlowSettings,
    onClick: () -> Unit
) {
    val gradientIdx = (album.hashCode() % ProfessionalPolishGradients.size).let { if (it < 0) -it else it }
    val isDark = LocalIsDarkMode.current
    val itemText = remember(isDark) { if (isDark) Color.White else Color(0xFF121212) }
    val itemTextMuted = remember(isDark) { if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF121212).copy(alpha = 0.6f) }
    val itemTextFaint = remember(isDark) { if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFF121212).copy(alpha = 0.4f) }
    val itemCardBorder = remember(isDark) { if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFF121212).copy(alpha = 0.1f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (settings.isGlassEnabled) GlassDarkSurface.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f))
            .border(1.dp, itemCardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album Cover Art (Sleek Gradient Disc)
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ProfessionalPolishGradients[gradientIdx])
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Album,
                contentDescription = null,
                tint = itemText.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Titles
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album,
                fontWeight = FontWeight.Bold,
                color = itemText,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$artist • $trackCount ${if (trackCount == 1) "Song" else "Songs"}",
                color = itemTextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "Open Album",
            tint = itemTextFaint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun ArtistListItem(
    artist: String,
    trackCount: Int,
    settings: BeatFlowSettings,
    onClick: () -> Unit
) {
    val gradientIdx = (artist.hashCode() % ProfessionalPolishGradients.size).let { if (it < 0) -it else it }
    val isDark = LocalIsDarkMode.current
    val itemText = remember(isDark) { if (isDark) Color.White else Color(0xFF121212) }
    val itemTextMuted = remember(isDark) { if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF121212).copy(alpha = 0.6f) }
    val itemTextFaint = remember(isDark) { if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFF121212).copy(alpha = 0.4f) }
    val itemCardBorder = remember(isDark) { if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFF121212).copy(alpha = 0.1f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (settings.isGlassEnabled) GlassDarkSurface.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f))
            .border(1.dp, itemCardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Artist Portrait Avatar Placeholder (Circular)
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(ProfessionalPolishGradients[gradientIdx])
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = itemText.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Titles
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist,
                fontWeight = FontWeight.Bold,
                color = itemText,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$trackCount ${if (trackCount == 1) "Song" else "Songs"}",
                color = itemTextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "Open Artist",
            tint = itemTextFaint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun PlaylistListItemHome(
    playlist: Playlist,
    settings: BeatFlowSettings,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val gradientIdx = (playlist.name.hashCode() % ProfessionalPolishGradients.size).let { if (it < 0) -it else it }
    val isDark = LocalIsDarkMode.current
    val itemText = remember(isDark) { if (isDark) Color.White else Color(0xFF121212) }
    val itemTextMuted = remember(isDark) { if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF121212).copy(alpha = 0.6f) }
    val itemCardBorder = remember(isDark) { if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFF121212).copy(alpha = 0.1f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (settings.isGlassEnabled) GlassDarkSurface.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f))
            .border(1.dp, itemCardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ProfessionalPolishGradients[gradientIdx])
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.QueueMusic,
                contentDescription = null,
                tint = itemText.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Titles
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                fontWeight = FontWeight.Bold,
                color = itemText,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Custom Playlist",
                color = itemTextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Playlist",
                tint = Color.Red.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

fun Modifier.bounceClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    if (enabled) {
        this
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
    } else {
        this
    }
}

@Composable
fun RecentlyPlayedScreen(
    viewModel: MainViewModel,
    settings: BeatFlowSettings,
    onClose: () -> Unit
) {
    val recentlyPlayed by viewModel.recentlyPlayedSongs.collectAsStateWithLifecycle()
    
    val bg = if (!settings.isDarkMode) LightCanvas else if (settings.isAmoledMode) Color.Black else Color(0xFF121212)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
    ) {
        // Top Header Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.testTag("recently_played_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Go Back",
                    tint = themeText
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Recently Played",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeText,
                    modifier = Modifier.testTag("recently_played_title")
                )
                Text(
                    text = if (recentlyPlayed.isEmpty()) "No tracks" else "${recentlyPlayed.size} ${if (recentlyPlayed.size == 1) "track" else "tracks"}",
                    fontSize = 12.sp,
                    color = themeTextMuted
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (recentlyPlayed.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                EmptyStatePlaceholder(
                    title = "No recently played songs",
                    subtitle = "Any songs you play will appear here in chronological order.",
                    icon = Icons.Default.History
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("recently_played_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentlyPlayed, key = { it.id }) { song ->
                    val onPlay = remember(song, recentlyPlayed) { { viewModel.playSong(song, recentlyPlayed) } }
                    val onFavoriteToggle = remember(song.id, song.isFavorite) { { viewModel.toggleFavorite(song.id, !song.isFavorite) } }
                    
                    SongListItem(
                        song = song,
                        settings = settings,
                        onPlay = onPlay,
                        onFavoriteToggle = onFavoriteToggle,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

