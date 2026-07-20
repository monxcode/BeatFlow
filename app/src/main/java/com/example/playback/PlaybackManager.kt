package com.example.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.MusicRepository
import com.example.data.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

object PlaybackManager {
    private const val TAG = "PlaybackManager"

    private var mediaController: MediaController? = null
    private var repository: MusicRepository? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionTrackerJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var bufferedListeningTimeMs = 0L
    private var lastRecordedSongId: Long? = null

    // Memory Cache for extremely fast, zero-allocation metadata lookup
    private val songCache = ConcurrentHashMap<Long, Song>()

    // Pending play commands in case controller is not connected yet
    private var pendingPlayCommand: PlayCommand? = null

    private data class PlayCommand(val song: Song, val queue: List<Song>)

    // Exposed States
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(-1)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF) // 0=off, 1=one, 2=all
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _sleepTimerRemainingSec = MutableStateFlow(0L)
    val sleepTimerRemainingSec: StateFlow<Long> = _sleepTimerRemainingSec.asStateFlow()

    private val _finishSongOnTimerEnd = MutableStateFlow(false)
    val finishSongOnTimerEnd: StateFlow<Boolean> = _finishSongOnTimerEnd.asStateFlow()

    private var pendingStopOnSongEnd = false

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    @OptIn(UnstableApi::class)
    fun getExoPlayer(context: Context): Player? {
        initController(context)
        return mediaController
    }

    fun initController(context: Context) {
        if (mediaController != null) return
        
        synchronized(this) {
            if (mediaController != null) return
            
            val appCtx = context.applicationContext
            if (repository == null) {
                repository = MusicRepository(appCtx)
            }

            val sessionToken = SessionToken(appCtx, ComponentName(appCtx, MusicService::class.java))
            val controllerFuture = MediaController.Builder(appCtx, sessionToken).buildAsync()
            
            controllerFuture.addListener({
                try {
                    val controller = controllerFuture.get()
                    mediaController = controller
                    setupControllerListener(controller)
                    
                    val pending = pendingPlayCommand
                    if (pending != null) {
                        pendingPlayCommand = null
                        playSong(appCtx, pending.song, pending.queue)
                    } else {
                        syncStatesFromController(controller)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(appCtx))
        }
    }

    private fun setupControllerListener(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                    val currentItem = player.currentMediaItem
                    val currentIdx = player.currentMediaItemIndex
                    _currentQueueIndex.value = currentIdx
                    
                    val song = getSongFromMediaItem(currentItem)
                    _currentSong.value = song
                    _duration.value = player.duration.coerceAtLeast(0)

                    lastRecordedSongId = null
                    if (player.isPlaying && song != null) {
                        lastRecordedSongId = song.id
                        scope.launch {
                            repository?.recordSongPlay(song.id)
                        }
                    }

                    if (pendingStopOnSongEnd) {
                        pendingStopOnSongEnd = false
                        player.pause()
                        cancelSleepTimer()
                    }
                }
                
                if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                    _isPlaying.value = player.isPlaying
                    if (player.isPlaying) {
                        startTrackingPosition()
                        val currentItem = player.currentMediaItem
                        val song = getSongFromMediaItem(currentItem)
                        if (song != null && song.id != lastRecordedSongId) {
                            lastRecordedSongId = song.id
                            scope.launch {
                                repository?.recordSongPlay(song.id)
                            }
                        }
                    } else {
                        stopTrackingPosition()
                    }
                }
                
                if (events.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) {
                    _shuffleEnabled.value = player.shuffleModeEnabled
                }
                
                if (events.contains(Player.EVENT_REPEAT_MODE_CHANGED)) {
                    _repeatMode.value = player.repeatMode
                }
                
                if (events.contains(Player.EVENT_TIMELINE_CHANGED)) {
                    val playlist = mutableListOf<Song>()
                    for (i in 0 until player.mediaItemCount) {
                        getSongFromMediaItem(player.getMediaItemAt(i))?.let { playlist.add(it) }
                    }
                    _queue.value = playlist
                    _currentQueueIndex.value = player.currentMediaItemIndex
                }
                
                if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)) {
                    _duration.value = player.duration.coerceAtLeast(0)
                    if (player.playbackState == Player.STATE_ENDED) {
                        // Playback completed on native end
                        if (pendingStopOnSongEnd) {
                            pendingStopOnSongEnd = false
                            cancelSleepTimer()
                        }
                    }
                }
            }
        })
    }

    private fun syncStatesFromController(controller: MediaController) {
        _isPlaying.value = controller.isPlaying
        _currentPosition.value = controller.currentPosition
        _duration.value = controller.duration.coerceAtLeast(0)
        _shuffleEnabled.value = controller.shuffleModeEnabled
        _repeatMode.value = controller.repeatMode
        _playbackSpeed.value = controller.playbackParameters.speed
        
        val playlist = mutableListOf<Song>()
        for (i in 0 until controller.mediaItemCount) {
            getSongFromMediaItem(controller.getMediaItemAt(i))?.let { playlist.add(it) }
        }
        if (playlist.isNotEmpty()) {
            _queue.value = playlist
            val idx = controller.currentMediaItemIndex
            _currentQueueIndex.value = idx
            if (idx in playlist.indices) {
                _currentSong.value = playlist[idx]
            }
        }
        
        if (controller.isPlaying) {
            startTrackingPosition()
        }
    }

    private fun startTrackingPosition() {
        positionTrackerJob?.cancel()
        positionTrackerJob = scope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    _currentPosition.value = controller.currentPosition
                    if (controller.isPlaying) {
                        bufferedListeningTimeMs += 1000L
                        if (bufferedListeningTimeMs >= 10000L) {
                            flushListeningTime()
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    private fun stopTrackingPosition() {
        positionTrackerJob?.cancel()
        positionTrackerJob = null
        flushListeningTime()
    }

    fun flushListeningTime() {
        val timeToSave = bufferedListeningTimeMs
        if (timeToSave > 0) {
            bufferedListeningTimeMs = 0L
            scope.launch {
                withContext(Dispatchers.IO) {
                    repository?.incrementListeningTime(timeToSave)
                }
            }
        }
    }

    // Playback control interfaces
    fun playSong(context: Context, song: Song, newQueue: List<Song> = listOf(song)) {
        initController(context)
        flushListeningTime()
        
        _queue.value = newQueue
        val index = newQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        _currentQueueIndex.value = index
        _currentSong.value = song

        // Fill memory cache
        newQueue.forEach { songCache[it.id] = it }
        songCache[song.id] = song

        lastRecordedSongId = null
        pendingStopOnSongEnd = false

        val controller = mediaController
        if (controller != null) {
            controller.stop()
            controller.clearMediaItems()
            val mediaItems = newQueue.map { createMediaItem(it) }
            controller.setMediaItems(mediaItems)
            controller.seekTo(index, 0L)
            controller.prepare()
            controller.play()
        } else {
            pendingPlayCommand = PlayCommand(song, newQueue)
        }
    }

    fun playQueue(context: Context, songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val index = startIndex.coerceIn(0, songs.size - 1)
        playSong(context, songs[index], songs)
    }

    fun togglePlayPause(context: Context) {
        initController(context)
        val controller = mediaController
        if (controller != null) {
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }

    fun playNext(context: Context) {
        initController(context)
        val controller = mediaController
        if (controller != null) {
            if (controller.hasNextMediaItem()) {
                controller.seekToNextMediaItem()
            } else if (controller.repeatMode == Player.REPEAT_MODE_ALL && controller.mediaItemCount > 0) {
                controller.seekTo(0, 0L)
            }
        }
    }

    fun playPrevious(context: Context) {
        initController(context)
        val controller = mediaController
        if (controller != null) {
            if (controller.hasPreviousMediaItem()) {
                controller.seekToPreviousMediaItem()
            } else if (controller.repeatMode == Player.REPEAT_MODE_ALL && controller.mediaItemCount > 0) {
                controller.seekTo(controller.mediaItemCount - 1, 0L)
            }
        }
    }

    fun seekTo(context: Context, positionMs: Long) {
        initController(context)
        val controller = mediaController
        if (controller != null) {
            controller.seekTo(positionMs)
        }
        _currentPosition.value = positionMs
    }

    fun toggleShuffle() {
        val controller = mediaController
        if (controller != null) {
            val nextVal = !controller.shuffleModeEnabled
            controller.shuffleModeEnabled = nextVal
            _shuffleEnabled.value = nextVal
        } else {
            _shuffleEnabled.value = !_shuffleEnabled.value
        }
    }

    fun cycleRepeatMode(context: Context) {
        initController(context)
        val controller = mediaController
        if (controller != null) {
            val nextMode = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            controller.repeatMode = nextMode
            _repeatMode.value = nextMode
        }
    }

    fun setPlaybackSpeed(context: Context, speed: Float) {
        initController(context)
        val controller = mediaController
        if (controller != null) {
            controller.setPlaybackSpeed(speed)
        }
        _playbackSpeed.value = speed
        scope.launch {
            repository?.updatePlayback(true, false, speed)
        }
    }

    fun insertIntoQueueNext(song: Song) {
        val currentList = _queue.value.toMutableList()
        val currentIndex = _currentQueueIndex.value
        val songId = song.id
        
        val existingIndex = currentList.indexOfFirst { it.id == songId }
        val controller = mediaController
        
        if (existingIndex != -1) {
            currentList.removeAt(existingIndex)
            controller?.removeMediaItem(existingIndex)
        }
        
        val adjustedCurrentIndex = if (currentIndex != -1) {
            currentList.indexOfFirst { it.id == _currentSong.value?.id }
        } else {
            -1
        }
        
        val insertIndex = if (adjustedCurrentIndex == -1) 0 else adjustedCurrentIndex + 1
        currentList.add(insertIndex, song)
        songCache[song.id] = song
        
        _queue.value = currentList
        _currentQueueIndex.value = if (adjustedCurrentIndex == -1) 0 else adjustedCurrentIndex
        
        if (controller != null) {
            val mediaItem = createMediaItem(song)
            controller.addMediaItem(insertIndex, mediaItem)
        }
    }

    fun removeFromQueue(songId: Long) {
        val currentList = _queue.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == songId }
        if (index != -1) {
            currentList.removeAt(index)
            _queue.value = currentList
            
            val controller = mediaController
            controller?.removeMediaItem(index)
            
            if (index == _currentQueueIndex.value) {
                _currentQueueIndex.value = if (currentList.isEmpty()) -1 else index.coerceAtMost(currentList.size - 1)
            } else if (index < _currentQueueIndex.value) {
                _currentQueueIndex.value--
            }
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _currentQueueIndex.value = -1
        _currentSong.value = null
        val controller = mediaController
        if (controller != null) {
            controller.stop()
            controller.clearMediaItems()
        }
    }

    fun startSleepTimer(context: Context, minutes: Int) {
        cancelSleepTimer()
        val durationSec = minutes * 60L
        _sleepTimerRemainingSec.value = durationSec

        sleepTimerJob = scope.launch {
            while (_sleepTimerRemainingSec.value > 0) {
                delay(1000)
                _sleepTimerRemainingSec.value--
            }
            onSleepTimerExpired(context)
        }
    }

    fun startSleepTimerCustom(context: Context, seconds: Long) {
        cancelSleepTimer()
        _sleepTimerRemainingSec.value = seconds

        sleepTimerJob = scope.launch {
            while (_sleepTimerRemainingSec.value > 0) {
                delay(1000)
                _sleepTimerRemainingSec.value--
            }
            onSleepTimerExpired(context)
        }
    }

    fun setFinishSongOnTimerEnd(value: Boolean) {
        _finishSongOnTimerEnd.value = value
    }

    private fun onSleepTimerExpired(context: Context) {
        val controller = mediaController
        if (_finishSongOnTimerEnd.value && controller != null && controller.isPlaying) {
            pendingStopOnSongEnd = true
            _sleepTimerRemainingSec.value = 0L
        } else {
            scope.launch {
                fadeOutAndPause(context)
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingSec.value = 0L
        pendingStopOnSongEnd = false
        mediaController?.volume = 1.0f
    }

    private suspend fun fadeOutAndPause(context: Context) {
        val controller = mediaController ?: return
        val steps = 10
        val delayInterval = 300L
        for (i in steps downTo 0) {
            controller.volume = i.toFloat() / steps
            delay(delayInterval)
        }
        controller.pause()
        controller.volume = 1.0f
        cancelSleepTimer()
    }

    fun releasePlayer() {
        mediaController?.release()
        mediaController = null
        repository = null
    }

    private fun createMediaItem(song: Song): MediaItem {
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(if (!song.artworkUri.isNullOrEmpty()) Uri.parse(song.artworkUri) else null)
            .build()
        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.path)
            .setMediaMetadata(mediaMetadata)
            .build()
    }

    private fun getSongFromMediaItem(mediaItem: MediaItem?): Song? {
        if (mediaItem == null) return null
        val songId = mediaItem.mediaId.toLongOrNull() ?: return null
        return songCache[songId]
    }

    fun isSongFavoriteInCache(songId: Long): Boolean {
        return songCache[songId]?.isFavorite == true
    }

    suspend fun toggleFavoriteFromService(context: Context, songId: Long): Boolean {
        val currentSongObj = songCache[songId] ?: return false
        val newFav = !currentSongObj.isFavorite
        val updatedSong = currentSongObj.copy(isFavorite = newFav)
        
        songCache[songId] = updatedSong
        
        if (_currentSong.value?.id == songId) {
            _currentSong.value = updatedSong
        }
        
        val currentQueue = _queue.value.map {
            if (it.id == songId) updatedSong else it
        }
        _queue.value = currentQueue
        
        val repo = repository ?: MusicRepository(context.applicationContext).also { repository = it }
        repo.toggleFavorite(songId, newFav)
        
        return newFav
    }

    fun updateSongInCacheAndQueue(song: Song) {
        songCache[song.id] = song
        if (_currentSong.value?.id == song.id) {
            _currentSong.value = song
        }
        val currentQueue = _queue.value.map {
            if (it.id == song.id) song else it
        }
        _queue.value = currentQueue
    }
}
