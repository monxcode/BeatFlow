package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MusicRepository(private val context: Context) {

    private val db = BeatFlowDatabase.getDatabase(context)
    private val songDao = db.songDao()
    private val playlistDao = db.playlistDao()
    private val statsDao = db.statsDao()
    private val dataStoreManager = DataStoreManager(context)

    // Exposed Flows
    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    val favoriteSongs: Flow<List<Song>> = songDao.getFavoriteSongs()
    val latestAddedSongs: Flow<List<Song>> = songDao.getLatestAddedSongs()
    val recentlyPlayedSongs: Flow<List<Song>> = songDao.getRecentlyPlayedSongs()
    val mostPlayedSongs: Flow<List<Song>> = songDao.getMostPlayedSongs()
    val playlists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()
    val listeningStats: Flow<List<ListeningStats>> = statsDao.getAllStats()
    val settings: Flow<BeatFlowSettings> = dataStoreManager.settingsFlow

    // System Date helper
    private fun getCurrentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // Music Smart Scanning
    suspend fun triggerScan(onProgress: (percent: Int, filesFound: Int, status: String) -> Unit = { _, _, _ -> }): Int {
        val currentSettings = settings.first()
        return MusicScanner.scanMusic(context, songDao, currentSettings, onProgress)
    }

    suspend fun triggerDocumentFolderScan(folderUriStr: String, onProgress: (percent: Int, filesFound: Int, status: String) -> Unit = { _, _, _ -> }): Int {
        val currentSettings = settings.first()
        val uri = Uri.parse(folderUriStr)
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            Log.e("MusicRepository", "Failed to take persistent URI permission", e)
        }
        return MusicScanner.scanDocumentFolder(context, songDao, uri, currentSettings, onProgress)
    }

    suspend fun deleteSampleSongs() {
        songDao.deleteSampleSongs()
    }

    // Song Operations
    suspend fun toggleFavorite(songId: Long, isFavorite: Boolean) {
        songDao.updateFavorite(songId, isFavorite)
    }

    suspend fun recordSongPlay(songId: Long) {
        songDao.incrementPlayCount(songId)
        songDao.updateLastPlayedAt(songId, System.currentTimeMillis())
        val today = getCurrentDateString()
        statsDao.insertOrUpdateStats(
            ListeningStats(
                date = today,
                listeningTimeMs = 0,
                songsPlayed = 1
            )
        )
        // Increment count directly
        statsDao.incrementStats(today, 0, 1)
    }

    suspend fun incrementListeningTime(timeMs: Long) {
        val today = getCurrentDateString()
        val existing = statsDao.getStatsForDate(today)
        if (existing == null) {
            statsDao.insertOrUpdateStats(ListeningStats(date = today, listeningTimeMs = timeMs, songsPlayed = 0))
        } else {
            statsDao.incrementStats(today, timeMs, 0)
        }
    }

    suspend fun getSongById(id: Long): Song? {
        return songDao.getSongById(id)
    }

    suspend fun deleteSong(song: Song) {
        songDao.deleteSong(song)
    }

    suspend fun updateSong(song: Song) {
        songDao.updateSong(song)
    }

    // Playlist Operations
    suspend fun createPlaylist(name: String, description: String = ""): Long {
        return playlistDao.insertPlaylist(
            Playlist(name = name, description = description, isSystem = false)
        )
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        playlistDao.deletePlaylist(playlist)
    }

    suspend fun updatePlaylist(playlist: Playlist) {
        playlistDao.updatePlaylist(playlist)
    }

    fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>> {
        return playlistDao.getSongsInPlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        playlistDao.insertPlaylistSongCrossRef(
            PlaylistSongCrossRef(playlistId = playlistId, songId = songId)
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    // Preference Operations
    suspend fun updateUserName(name: String) {
        dataStoreManager.updateUserName(name)
    }

    suspend fun setOnboardingCompleted() {
        dataStoreManager.setOnboardingCompleted(true)
    }

    suspend fun updateTheme(isDarkMode: Boolean, isAmoledMode: Boolean, isGlass: Boolean, accentIndex: Int) {
        dataStoreManager.updateThemeSettings(isDarkMode, isAmoledMode, isGlass, accentIndex)
    }

    suspend fun updateFilters(
        shorterThan1Min: Boolean,
        smallerThan100KB: Boolean,
        hidden: Boolean,
        duplicates: Boolean,
        downloads: Boolean,
        sdCard: Boolean
    ) {
        dataStoreManager.updateScanFilters(
            shorterThan1Min,
            smallerThan100KB,
            hidden,
            duplicates,
            downloads,
            sdCard
        )
    }

    suspend fun updatePlayback(gapless: Boolean, crossfade: Boolean, speed: Float) {
        dataStoreManager.updatePlaybackSettings(gapless, crossfade, speed)
    }

    suspend fun updateProfileImage(path: String) {
        dataStoreManager.updateProfileImage(path)
    }

    suspend fun updateCustomScanFolderPath(path: String) {
        dataStoreManager.updateCustomScanFolderPath(path)
    }
}
