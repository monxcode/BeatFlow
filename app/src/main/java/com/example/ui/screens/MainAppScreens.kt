package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.ui.theme.GlassDarkSurface
import com.example.ui.theme.GlassLightSurface
import com.example.ui.theme.Accents
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HotPink
import com.example.ui.theme.NeonGreen
import java.util.Calendar

// Pre-baked Avatar Gradients for high-fidelity offline personalization
val AvatarGradients = listOf(
    Brush.linearGradient(listOf(NeonGreen, ElectricBlue)),
    Brush.linearGradient(listOf(HotPink, ElectricBlue)),
    Brush.linearGradient(listOf(Color(0xFFFFB300), HotPink)),
    Brush.linearGradient(listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)))
)

@Composable
fun HomeScreenContent(
    viewModel: MainViewModel,
    onNavigateToProfile: () -> Unit
) {
    val songs by viewModel.allSongs.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val latest by viewModel.latestAddedSongs.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredSongs by viewModel.filteredSongs.collectAsStateWithLifecycle()

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
                            tint = Color.White
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
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
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
                                            color = Color.White.copy(alpha = 0.3f),
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
                                    tint = Color.White.copy(alpha = 0.6f),
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
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "$greeting, ${settings.userName} 👋",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Your music, secured offline.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f),
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
                            .background(Color.White.copy(alpha = 0.08f))
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
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Profile Avatar (Smaller size on scroll)
                val avatarIdx = settings.accentColorIndex.coerceIn(0, AvatarGradients.size - 1)
                val avatarSize = if (isScrolled) 36.dp else 48.dp
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(AvatarGradients[avatarIdx])
                        .clickable(onClick = onNavigateToProfile),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = settings.userName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = if (isScrolled) 13.sp else 18.sp
                    )
                }
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
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
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
                                        color = Color.White.copy(alpha = 0.3f),
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
                                tint = Color.White.copy(alpha = 0.6f),
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
                    color = Color.White
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
                items(filteredSongs) { song ->
                    SongListItem(
                        song = song,
                        settings = settings,
                        onPlay = { viewModel.playSong(song, filteredSongs) },
                        onFavoriteToggle = { viewModel.toggleFavorite(song.id, !song.isFavorite) }
                    )
                }
            }
        } else {
            // Quick Stats Card (Professional Polish Style)
            item {
                val statsList by viewModel.listeningStats.collectAsStateWithLifecycle()
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
                            Text("Listening Time", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                            Text(formatListenTime, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 4.dp))
                        }
                        Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.1f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Tracks", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                            Text(songs.size.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 4.dp))
                        }
                        Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.1f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Favorites", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                            Text(favorites.size.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 4.dp))
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
                    // Active Chip ("All")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Accents[settings.accentColorIndex])
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "All",
                            color = if (settings.accentColorIndex == 0) com.example.ui.theme.DeepViolet else Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Inactive Chips ("Albums", "Artists", "Playlists")
                    listOf("Albums", "Artists", "Playlists").forEach { label ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (settings.isGlassEnabled) GlassDarkSurface else Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 3. RECENTLY PLAYED / LATEST ADDED HORIZONTAL GRID
            if (latest.isNotEmpty()) {
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
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "See All",
                            fontSize = 12.sp,
                            color = Accents[settings.accentColorIndex],
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(latest.take(5)) { song ->
                            LatestTrackCard(
                                song = song,
                                settings = settings,
                                onPlay = { viewModel.playSong(song, latest) }
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
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(favorites) { song ->
                            LatestTrackCard(
                                song = song,
                                settings = settings,
                                onPlay = { viewModel.playSong(song, favorites) }
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
                        color = Color.White
                    )
                    TextButton(onClick = { viewModel.triggerScan() }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize), tint = Accents[settings.accentColorIndex])
                            Spacer(Modifier.width(4.dp))
                            Text("Rescan", color = Accents[settings.accentColorIndex], fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (songs.isEmpty()) {
                item {
                    EmptyStatePlaceholder(
                        title = "Your library is silent",
                        subtitle = "We seeded 3 procedural tracks. Press scan to load them!",
                        icon = Icons.Default.MusicNote,
                        actionText = "Scan local files",
                        onAction = { viewModel.triggerScan() }
                    )
                }
            } else {
                items(songs) { song ->
                    SongListItem(
                        song = song,
                        settings = settings,
                        onPlay = { viewModel.playSong(song, songs) },
                        onFavoriteToggle = { viewModel.toggleFavorite(song.id, !song.isFavorite) }
                    )
                }
            }
        }
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
            color = Color.White
        )
        Text(
            text = "Songs you've bookmarked offline.",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f),
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
                items(favorites) { song ->
                    SongListItem(
                        song = song,
                        settings = settings,
                        onPlay = { viewModel.playSong(song, favorites) },
                        onFavoriteToggle = { viewModel.toggleFavorite(song.id, !song.isFavorite) }
                    )
                }
            }
        }
    }
}

// ---------------- PLAYLISTS / FOLDERS SCREEN ----------------
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
            color = Color.White
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
                    color = Color.White
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
                    items(playlists) { playlist ->
                        PlaylistListItem(
                            playlist = playlist,
                            settings = settings,
                            onDelete = { viewModel.deletePlaylist(playlist) }
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
                    items(folders.keys.toList()) { folderName ->
                        val folderSongs = folders[folderName] ?: emptyList()
                        FolderListItem(
                            name = folderName,
                            songsCount = folderSongs.size,
                            settings = settings,
                            onPlayFolder = { viewModel.playQueue(folderSongs, 0) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Offline Playlist", color = Color.White) },
            text = {
                TextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("e.g. Coding Beats", color = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
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
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
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
                color = Color.White
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
                    // Cyclic Avatar Selector
                    val currentAvatarIdx = settings.accentColorIndex.coerceIn(0, AvatarGradients.size - 1)
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(AvatarGradients[currentAvatarIdx])
                            .clickable {
                                // Cycle profile avatar by changing accent color
                                val nextAccent = (settings.accentColorIndex + 1) % Accents.size
                                viewModel.updateThemeSettings(
                                    settings.isDarkMode,
                                    settings.isAmoledMode,
                                    settings.isGlassEnabled,
                                    nextAccent
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = settings.userName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
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
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
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
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = settings.userName,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
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
                                color = Color.White.copy(alpha = 0.5f)
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
                color = Color.White
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

        // ABOUT DEVELOPER ROW (Mohan Singh Parmar)
        item {
            Text(
                text = "About Developer",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                isGlassEnabled = settings.isGlassEnabled,
                isDark = settings.isDarkMode
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Mohan Singh Parmar",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Android Developer • Cybersecurity & AI Enthusiast",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    // Clickable GitHub Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
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
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Footer Credit
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Made with ❤️ by Mohan Singh Parmar",
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            )
        }
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
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.padding(top = 2.dp))
        }
    }
}

// ---------------- SETTINGS SCREEN ----------------
@Composable
fun SettingsScreenContent(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
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
                color = Color.White
            )
        }

        // 1. APPEARANCE
        item {
            Text("Appearance", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                        Text("Dark Mode", color = Color.White, fontSize = 14.sp)
                        Switch(
                            checked = settings.isDarkMode,
                            onCheckedChange = { viewModel.updateThemeSettings(it, settings.isAmoledMode, settings.isGlassEnabled, settings.accentColorIndex) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Accents[settings.accentColorIndex])
                        )
                    }

                    // AMOLED Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AMOLED Pitch Black", color = Color.White, fontSize = 14.sp)
                        Switch(
                            checked = settings.isAmoledMode,
                            onCheckedChange = { viewModel.updateThemeSettings(settings.isDarkMode, it, settings.isGlassEnabled, settings.accentColorIndex) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Accents[settings.accentColorIndex])
                        )
                    }

                    // Accent Colors Row Selector
                    Column {
                        Text("Glow Accent Color", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
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
                                            color = Color.White,
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

        // 2. SMART MUSIC SCANS
        item {
            Text("Music Scanner", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
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
                        Text("Ignore shorter than 1 min", color = Color.White, fontSize = 14.sp)
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
                            colors = SwitchDefaults.colors(checkedThumbColor = Accents[settings.accentColorIndex])
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ignore smaller than 100 KB", color = Color.White, fontSize = 14.sp)
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
                            colors = SwitchDefaults.colors(checkedThumbColor = Accents[settings.accentColorIndex])
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ignore hidden files/folders", color = Color.White, fontSize = 14.sp)
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
                            colors = SwitchDefaults.colors(checkedThumbColor = Accents[settings.accentColorIndex])
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Scan SD Card folders", color = Color.White, fontSize = 14.sp)
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
                            colors = SwitchDefaults.colors(checkedThumbColor = Accents[settings.accentColorIndex])
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Scan Downloads directory", color = Color.White, fontSize = 14.sp)
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
                            colors = SwitchDefaults.colors(checkedThumbColor = Accents[settings.accentColorIndex])
                        )
                    }

                    // Scan triggers
                    Button(
                        onClick = { viewModel.triggerScan() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accents[settings.accentColorIndex],
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rescan Music Assets Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Storage permissions card
        item {
            Text("System Permissions & Storage", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
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
                            Text("Storage Permission", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("BeatFlow is fully authorized to load media.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Direct folder scanner card (force add)
        item {
            Text("Immersive Folder Scanner (Force Add)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                isGlassEnabled = settings.isGlassEnabled,
                isDark = settings.isDarkMode
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Specify a direct directory path to scan files recursively. This bypasses system indexing databases entirely.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    TextField(
                        value = folderInputPath,
                        onValueChange = { folderInputPath = it },
                        label = { Text("Custom Folder Path", color = Color.White.copy(alpha = 0.4f)) },
                        placeholder = { Text("e.g. /sdcard/Music", color = Color.White.copy(alpha = 0.3f)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                            focusedLabelColor = Accents[settings.accentColorIndex],
                            unfocusedLabelColor = Color.White.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick Presets Selector
                    Column {
                        Text("Quick Presets Path Selection", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "/sdcard/Music" to "Music SD",
                                "/storage/emulated/0/Music" to "Music Storage",
                                "/storage/emulated/0/Download" to "Downloads"
                            ).forEach { (path, label) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (folderInputPath == path) Accents[settings.accentColorIndex].copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                        .border(
                                            width = 1.dp,
                                            color = if (folderInputPath == path) Accents[settings.accentColorIndex] else Color.White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { folderInputPath = path }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (folderInputPath == path) Accents[settings.accentColorIndex] else Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

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

        // 3. PLAYBACK speed
        item {
            Text("Playback", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                        Text("Playback Speed", color = Color.White, fontSize = 14.sp)
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
            Text("About BeatFlow", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                isGlassEnabled = settings.isGlassEnabled,
                isDark = settings.isDarkMode
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Offline Architecture", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text("This app operates exclusively as a client-side database. It contains zero background servers, cloud modules, API keys, tracking SDKs, or promotional items.", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Product Version", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        Text("v1.0.0 (Production Stable)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            .background(if (settings.isGlassEnabled) GlassDarkSurface.copy(alpha = 0.1f) else Color(0xFF1E1E1E))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable(onClick = onPlay)
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
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
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
    }
}

@Composable
fun SongListItem(
    song: Song,
    settings: BeatFlowSettings,
    onPlay: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (settings.isGlassEnabled) GlassDarkSurface.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
            .clickable(onClick = onPlay)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Compact art preview
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Accents[settings.accentColorIndex],
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Action Keys (Favorite toggle)
        IconButton(onClick = onFavoriteToggle) {
            Icon(
                imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite Toggle",
                tint = if (song.isFavorite) HotPink else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PlaylistListItem(
    playlist: Playlist,
    settings: BeatFlowSettings,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                tint = Accents[settings.accentColorIndex],
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(playlist.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Text("Offline collection", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
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
    onPlayFolder: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .clickable(onClick = onPlayFolder)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = Accents[settings.accentColorIndex],
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Text("$songsCount tracks inside", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play Folder",
            tint = Accents[settings.accentColorIndex],
            modifier = Modifier.size(20.dp)
        )
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
            tint = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.4f),
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
