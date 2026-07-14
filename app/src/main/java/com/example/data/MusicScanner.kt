package com.example.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MusicScanner {
    private const val TAG = "MusicScanner"

    private fun getSongUniqueKey(title: String, artist: String): String {
        val cleanTitle = title.lowercase().trim()
        val cleanArtist = artist.lowercase().trim()
            .replace("unknown artist", "")
            .replace("unknown", "")
            .replace("local artist", "")
        return if (cleanArtist.isEmpty()) {
            cleanTitle
        } else {
            "$cleanTitle|$cleanArtist"
        }
    }

    suspend fun scanMusic(
        context: Context,
        songDao: SongDao,
        settings: BeatFlowSettings,
        onProgress: (percent: Int, filesFound: Int, status: String) -> Unit = { _, _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting Smart Music Scan...")
        var scannedCount = 0

        onProgress(0, 0, "Initializing music scanner...")

        // 1. Delete any existing sample tracks if they exist on storage
        try {
            val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
            val sampleFiles = listOf("sunset_chill.wav", "cyber_pulse.wav", "midnight_rain.wav")
            for (fileName in sampleFiles) {
                val file = File(musicDir, fileName)
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete old seed files", e)
        }

        onProgress(3, 0, "Clearing sample files from database...")

        // 1.5 Delete sample songs from database to clean up UI
        try {
            songDao.deleteSampleSongs()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete sample songs from DB", e)
        }

        onProgress(5, 0, "Checking existing songs...")

        val allExistingSongs = try {
            songDao.getAllSongsSync()
        } catch (e: Exception) {
            emptyList()
        }

        val songsToInsert = mutableListOf<Song>()
        val existingPaths = allExistingSongs.map { it.path.lowercase().trim() }.toMutableSet()
        val existingKeys = allExistingSongs.map { getSongUniqueKey(it.title, it.artist) }.filter { it.isNotEmpty() }.toMutableSet()

        // Helper to check filters
        fun passesFilters(file: File, durationMs: Long, sizeBytes: Long): Boolean {
            if (settings.ignoreShorterThan1Min && durationMs < 60000) return false
            if (settings.ignoreSmallerThan100KB && sizeBytes < 100000) return false
            if (settings.ignoreHidden) {
                if (file.name.startsWith(".") || file.parentFile?.name?.startsWith(".") == true) return false
            }
            return true
        }

        var fsFilesScanned = 0

        // 2.5 Scan custom user folders forcefully from direct filesystem path
        val customFolderPath = settings.customScanFolderPath
        if (customFolderPath.isNotBlank()) {
            try {
                val customFolder = File(customFolderPath)
                if (customFolder.exists() && customFolder.isDirectory) {
                    Log.d(TAG, "Force Scanning custom folder directly: $customFolderPath")
                    onProgress(10, songsToInsert.size, "Scanning custom directory...")
                    scanDirectory(customFolder, songDao, settings, existingPaths, existingKeys, songsToInsert, onProgress, 10, 20, { fsFilesScanned++ })
                } else {
                    Log.w(TAG, "Custom scan folder does not exist or is not a directory: $customFolderPath")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error force scanning custom folder: $customFolderPath", e)
            }
        }

        // 2.7 Force Scan the entire primary external storage directory recursively (e.g., /sdcard or /storage/emulated/0)
        try {
            val root = Environment.getExternalStorageDirectory()
            if (root != null && root.exists() && root.isDirectory) {
                Log.d(TAG, "Force scanning entire external storage recursively: ${root.absolutePath}")
                onProgress(20, songsToInsert.size, "Scanning internal storage folders...")
                scanDirectory(root, songDao, settings, existingPaths, existingKeys, songsToInsert, onProgress, 20, 40, { fsFilesScanned++ })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error force scanning primary external storage root", e)
        }

        // 2.8 Scan secondary storage volumes (like SD Cards or OTG drives) mounted under /storage
        try {
            val storageDir = File("/storage")
            if (storageDir.exists() && storageDir.isDirectory) {
                val volumes = storageDir.listFiles()
                if (volumes != null) {
                    for (volume in volumes) {
                        if (volume.isDirectory && !volume.name.equals("self", ignoreCase = true) && !volume.name.equals("emulated", ignoreCase = true)) {
                            Log.d(TAG, "Scanning secondary storage volume recursively: ${volume.absolutePath}")
                            onProgress(40, songsToInsert.size, "Scanning external storage volume...")
                            scanDirectory(volume, songDao, settings, existingPaths, existingKeys, songsToInsert, onProgress, 40, 50, { fsFilesScanned++ })
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning secondary storage volumes", e)
        }

        // 3. Scan system MediaStore for real user songs (requires storage permissions)
        try {
            onProgress(50, songsToInsert.size, "Preparing system media database query...")
            val collectionUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.ALBUM_ID
            )

            // Retrieve ALL audio indexed by the MediaStore (including WhatsApp, voice notes, audio recordings, podcasts)
            val selection = null

            context.contentResolver.query(
                collectionUri,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                val totalCount = cursor.count
                var currentIndex = 0
                while (cursor.moveToNext()) {
                    currentIndex++
                    val path = cursor.getString(pathCol) ?: continue
                    val pathLower = path.lowercase().trim()
                    if (existingPaths.contains(pathLower)) continue

                    val file = File(path)
                    val duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)

                    val progressPercent = 50 + ((currentIndex.toFloat() / totalCount.coerceAtLeast(1)) * 45).toInt()
                    onProgress(progressPercent, songsToInsert.size, "Scanning Media Store ($currentIndex/$totalCount)...")

                    if (passesFilters(file, duration, size)) {
                        val title = cursor.getString(titleCol) ?: file.nameWithoutExtension
                        val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                        val album = cursor.getString(albumCol) ?: "Unknown Album"
                        val dateAdded = cursor.getLong(dateCol) * 1000 // Convert to ms

                        val key = getSongUniqueKey(title, artist)
                        if (existingKeys.contains(key)) continue

                        val albumId = cursor.getLong(albumIdCol)
                        val artworkUri = if (albumId != -1L) {
                            "content://media/external/audio/albumart/$albumId"
                        } else {
                            null
                        }

                        val song = Song(
                            path = path,
                            title = title,
                            artist = artist,
                            album = album,
                            genre = "Local Audio",
                            duration = duration,
                            size = size,
                            dateAdded = dateAdded,
                            folderName = file.parentFile?.name ?: "Downloads",
                            artworkUri = artworkUri
                        )
                        songsToInsert.add(song)
                        existingPaths.add(pathLower)
                        existingKeys.add(key)
                        scannedCount++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning MediaStore", e)
        }

        if (songsToInsert.isNotEmpty()) {
            onProgress(95, songsToInsert.size, "Saving ${songsToInsert.size} new songs to local database...")
            songDao.insertSongs(songsToInsert)
            Log.d(TAG, "Successfully scanned and added ${songsToInsert.size} songs to Room Database.")
        } else {
            Log.d(TAG, "No new songs found that match filter criteria.")
        }

        onProgress(100, songsToInsert.size, "Scan Completed! Found ${songsToInsert.size} new tracks.")
        songsToInsert.size
    }

    private suspend fun scanDirectory(
        directory: File,
        songDao: SongDao,
        settings: BeatFlowSettings,
        existingPaths: MutableSet<String>,
        existingKeys: MutableSet<String>,
        songsToInsert: MutableList<Song>,
        onProgress: (percent: Int, filesFound: Int, status: String) -> Unit,
        progressStart: Int,
        progressEnd: Int,
        onIncrementCount: () -> Unit
    ) {
        val files = directory.listFiles() ?: return
        val audioExtensions = setOf("mp3", "wav", "m4a", "flac", "ogg", "aac")
        for (file in files) {
            if (file.isDirectory) {
                if (settings.ignoreHidden && file.name.startsWith(".")) continue
                // Skip the "Android" folder completely to prevent OS-level permission blocks and extremely slow scans of private app caches
                if (file.name.equals("Android", ignoreCase = true)) continue
                scanDirectory(file, songDao, settings, existingPaths, existingKeys, songsToInsert, onProgress, progressStart, progressEnd, onIncrementCount)
            } else if (file.isFile) {
                onIncrementCount()
                val ext = file.extension.lowercase()
                if (ext in audioExtensions) {
                    val path = file.absolutePath
                    val pathLower = path.lowercase().trim()
                    if (existingPaths.contains(pathLower)) continue

                    val sizeBytes = file.length()
                    if (settings.ignoreSmallerThan100KB && sizeBytes < 100000) continue
                    if (settings.ignoreHidden && file.name.startsWith(".")) continue

                    var durationMs = 180000L // 3 mins fallback
                    var songTitle = file.nameWithoutExtension
                    var songArtist = "Local Artist"
                    var songAlbum = "Local Album"
                    try {
                        val retriever = android.media.MediaMetadataRetriever()
                        retriever.setDataSource(path)
                        val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                        if (durationStr != null) {
                            durationMs = durationStr.toLong()
                        }
                        val titleStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                        if (!titleStr.isNullOrBlank()) {
                            songTitle = titleStr
                        }
                        val artistStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        if (!artistStr.isNullOrBlank()) {
                            songArtist = artistStr
                        }
                        val albumStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM)
                        if (!albumStr.isNullOrBlank()) {
                            songAlbum = albumStr
                        }
                        retriever.release()
                    } catch (e: Exception) {
                        // ignore
                    }

                    val key = getSongUniqueKey(songTitle, songArtist)
                    if (existingKeys.contains(key)) continue

                    if (settings.ignoreShorterThan1Min && durationMs < 60000) continue

                    val song = Song(
                        path = path,
                        title = songTitle,
                        artist = songArtist,
                        album = songAlbum,
                        genre = "Local Audio",
                        duration = durationMs,
                        size = sizeBytes,
                        dateAdded = file.lastModified(),
                        folderName = file.parentFile?.name ?: "CustomFolder",
                        artworkUri = null
                    )
                    songsToInsert.add(song)
                    existingPaths.add(pathLower)
                    existingKeys.add(key)
                }
            }
        }
    }
}
