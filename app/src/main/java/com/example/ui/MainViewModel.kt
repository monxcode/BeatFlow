package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.playback.PlaybackManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val context: Context = application.applicationContext

    // Settings
    val settings: StateFlow<BeatFlowSettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BeatFlowSettings(
                userName = "",
                isOnboardingCompleted = false,
                isDarkMode = true,
                isAmoledMode = true,
                accentColorIndex = 0,
                isGlassEnabled = false,
                ignoreShorterThan1Min = true,
                ignoreSmallerThan100KB = true,
                ignoreHidden = true,
                ignoreDuplicates = true,
                scanDownloads = true,
                scanSDCard = true,
                gaplessPlayback = true,
                crossfadePlayback = false,
                playbackSpeed = 1.0f,
                profileImagePath = "",
                customScanFolderPath = ""
            )
        )

    // Music Tables
    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestAddedSongs: StateFlow<List<Song>> = repository.latestAddedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayedSongs: StateFlow<List<Song>> = repository.recentlyPlayedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayedSongs: StateFlow<List<Song>> = repository.mostPlayedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val listeningStats: StateFlow<List<ListeningStats>> = repository.listeningStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredSongs: StateFlow<List<Song>> = combine(allSongs, searchQuery) { songs, query ->
        if (query.isBlank()) {
            songs
        } else {
            songs.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Playback state bridged from PlaybackManager
    val currentSong: StateFlow<Song?> = PlaybackManager.currentSong
    val isPlaying: StateFlow<Boolean> = PlaybackManager.isPlaying
    val currentPosition: StateFlow<Long> = PlaybackManager.currentPosition
    val duration: StateFlow<Long> = PlaybackManager.duration
    val queue: StateFlow<List<Song>> = PlaybackManager.queue
    val currentQueueIndex: StateFlow<Int> = PlaybackManager.currentQueueIndex
    val shuffleEnabled: StateFlow<Boolean> = PlaybackManager.shuffleEnabled
    val repeatMode: StateFlow<Int> = PlaybackManager.repeatMode
    val sleepTimerRemainingSec: StateFlow<Long> = PlaybackManager.sleepTimerRemainingSec
    val finishSongOnTimerEnd: StateFlow<Boolean> = PlaybackManager.finishSongOnTimerEnd
    val playbackSpeed: StateFlow<Float> = PlaybackManager.playbackSpeed

    // Loading State
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0)
    val scanProgress: StateFlow<Int> = _scanProgress.asStateFlow()

    private val _scanFilesFound = MutableStateFlow(0)
    val scanFilesFound: StateFlow<Int> = _scanFilesFound.asStateFlow()

    private val _scanStatus = MutableStateFlow("Ready to Scan")
    val scanStatus: StateFlow<String> = _scanStatus.asStateFlow()

    private val _showScanOverlay = MutableStateFlow(false)
    val showScanOverlay: StateFlow<Boolean> = _showScanOverlay.asStateFlow()

    fun setShowScanOverlay(show: Boolean) {
        _showScanOverlay.value = show
    }

    init {
        // Automatically start background position monitoring
        PlaybackManager.getExoPlayer(context)

        // Force delete existing sample songs from database on startup
        viewModelScope.launch {
            try {
                repository.deleteSampleSongs()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Smart Music Scanner Trigger
    fun triggerScan() {
        if (_isScanning.value) return
        _isScanning.value = true
        _scanProgress.value = 0
        _scanFilesFound.value = 0
        _scanStatus.value = "Starting scan..."
        viewModelScope.launch {
            try {
                repository.triggerScan { percent, filesFound, status ->
                    _scanProgress.value = percent
                    _scanFilesFound.value = filesFound
                    _scanStatus.value = status
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _scanStatus.value = "Error: ${e.localizedMessage ?: "Unknown scanning error"}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun triggerDocumentFolderScan(folderUriStr: String) {
        if (_isScanning.value) return
        _isScanning.value = true
        _scanProgress.value = 0
        _scanFilesFound.value = 0
        _scanStatus.value = "Starting folder scan..."
        viewModelScope.launch {
            try {
                repository.triggerDocumentFolderScan(folderUriStr) { percent, filesFound, status ->
                    _scanProgress.value = percent
                    _scanFilesFound.value = filesFound
                    _scanStatus.value = status
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _scanStatus.value = "Error: ${e.localizedMessage ?: "Unknown scanning error"}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    // Onboarding handlers
    fun saveUserName(name: String) {
        viewModelScope.launch {
            repository.updateUserName(name)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.setOnboardingCompleted()
            // Auto trigger a quick scan on onboarding completion to load our seeded files!
            triggerScan()
        }
    }

    // Playback control delegation
    fun playSong(song: Song, customQueue: List<Song> = emptyList()) {
        val q = if (customQueue.isNotEmpty()) customQueue else allSongs.value
        PlaybackManager.playSong(context, song, q)
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        PlaybackManager.playQueue(context, songs, startIndex)
    }

    fun togglePlayPause() {
        PlaybackManager.togglePlayPause(context)
    }

    fun playNext() {
        PlaybackManager.playNext(context)
    }

    fun playPrevious() {
        PlaybackManager.playPrevious(context)
    }

    fun seekTo(positionMs: Long) {
        PlaybackManager.seekTo(context, positionMs)
    }

    fun toggleShuffle() {
        PlaybackManager.toggleShuffle()
    }

    fun cycleRepeatMode() {
        PlaybackManager.cycleRepeatMode(context)
    }

    fun setPlaybackSpeed(speed: Float) {
        PlaybackManager.setPlaybackSpeed(context, speed)
    }

    fun removeFromQueue(songId: Long) {
        PlaybackManager.removeFromQueue(songId)
    }

    fun clearQueue() {
        PlaybackManager.clearQueue()
    }

    // Sleep Timer delegate
    fun startSleepTimer(minutes: Int) {
        PlaybackManager.startSleepTimer(context, minutes)
    }

    fun startSleepTimerCustom(seconds: Long) {
        PlaybackManager.startSleepTimerCustom(context, seconds)
    }

    fun cancelSleepTimer() {
        PlaybackManager.cancelSleepTimer()
    }

    fun setFinishSongOnTimerEnd(value: Boolean) {
        PlaybackManager.setFinishSongOnTimerEnd(value)
    }

    // Search query update
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Database writes - Favorites
    fun toggleFavorite(songId: Long, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(songId, isFav)
        }
    }

    // Database writes - Playlists
    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            repository.createPlaylist(name, description)
        }
    }

    fun renamePlaylist(playlist: Playlist, newName: String) {
        viewModelScope.launch {
            repository.updatePlaylist(playlist.copy(name = newName))
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun createPlaylistAndAddSong(playlistName: String, songId: Long) {
        viewModelScope.launch {
            val playlistId = repository.createPlaylist(playlistName)
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>> {
        return repository.getSongsInPlaylist(playlistId)
    }

    // Database writes - Settings
    fun updateThemeSettings(isDark: Boolean, isAmoled: Boolean, isGlass: Boolean, accentIndex: Int) {
        viewModelScope.launch {
            repository.updateTheme(isDark, isAmoled, isGlass, accentIndex)
        }
    }

    fun updateScanFilters(
        shorterThan1Min: Boolean,
        smallerThan100KB: Boolean,
        hidden: Boolean,
        duplicates: Boolean,
        downloads: Boolean,
        sdCard: Boolean
    ) {
        viewModelScope.launch {
            repository.updateFilters(
                shorterThan1Min,
                smallerThan100KB,
                hidden,
                duplicates,
                downloads,
                sdCard
            )
        }
    }

    fun updateProfileImage(path: String) {
        viewModelScope.launch {
            repository.updateProfileImage(path)
        }
    }

    fun updateCustomScanFolderPath(path: String) {
        viewModelScope.launch {
            repository.updateCustomScanFolderPath(path)
        }
    }

    // Song actions from option menu
    fun playNext(song: Song) {
        PlaybackManager.insertIntoQueueNext(song)
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            repository.deleteSong(song)
        }
    }

    fun updateSong(song: Song) {
        viewModelScope.launch {
            repository.updateSong(song)
        }
    }

    fun renameSong(song: Song, newTitle: String, newArtist: String) {
        viewModelScope.launch {
            repository.updateSong(song.copy(title = newTitle, artist = newArtist))
        }
    }
}
