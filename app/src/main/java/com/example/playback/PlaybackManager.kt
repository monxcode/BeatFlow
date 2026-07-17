package com.example.playback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.MusicRepository
import com.example.data.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PlaybackManager {
    private const val TAG = "PlaybackManager"

    private var exoPlayer: ExoPlayer? = null
    private var repository: MusicRepository? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionTrackerJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var bufferedListeningTimeMs = 0L

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

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    @OptIn(UnstableApi::class)
    fun getExoPlayer(context: Context): ExoPlayer {
        return exoPlayer ?: synchronized(this) {
            val repo = MusicRepository(context.applicationContext)
            repository = repo
            
            val player = ExoPlayer.Builder(context.applicationContext)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true // Manage audio focus automatically
                )
                .setHandleAudioBecomingNoisy(true) // Pause automatically when earphone unplugged
                .build()

            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                    if (playing) {
                        startTrackingPosition()
                    } else {
                        stopTrackingPosition()
                    }
                }

                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_READY -> {
                            _duration.value = player.duration
                            _currentPosition.value = player.currentPosition
                        }
                        Player.STATE_ENDED -> {
                            playNext(context)
                        }
                    }
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    _currentPosition.value = player.currentPosition
                }
            })

            exoPlayer = player
            player
        }
    }

    private fun startTrackingPosition() {
        positionTrackerJob?.cancel()
        positionTrackerJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    _currentPosition.value = player.currentPosition
                    if (player.isPlaying) {
                        // Buffer listening time in memory
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

    private fun flushListeningTime() {
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
        flushListeningTime()
        val player = getExoPlayer(context)
        
        _queue.value = newQueue
        val index = newQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        _currentQueueIndex.value = index
        _currentSong.value = song

        // Prepare player
        player.stop()
        player.clearMediaItems()
        
        val mediaItem = createMediaItem(song)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        // Start Foreground Service to keep playing in background
        startService(context)

        scope.launch {
            repository?.recordSongPlay(song.id)
        }
    }

    fun playQueue(context: Context, songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val index = startIndex.coerceIn(0, songs.size - 1)
        _queue.value = songs
        playSong(context, songs[index], songs)
    }

    fun togglePlayPause(context: Context) {
        val player = getExoPlayer(context)
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.mediaItemCount == 0 && _currentSong.value != null) {
                // Restore item
                val mediaItem = createMediaItem(_currentSong.value!!)
                player.setMediaItem(mediaItem)
                player.seekTo(_currentPosition.value)
                player.prepare()
            }
            player.play()
            startService(context)
        }
    }

    fun playNext(context: Context) {
        val q = _queue.value
        if (q.isEmpty()) return

        var nextIndex = _currentQueueIndex.value + 1
        if (_shuffleEnabled.value) {
            nextIndex = (0 until q.size).random()
        } else if (nextIndex >= q.size) {
            nextIndex = if (_repeatMode.value == Player.REPEAT_MODE_ALL) 0 else return
        }

        _currentQueueIndex.value = nextIndex
        val nextSong = q[nextIndex]
        playSong(context, nextSong, q)
    }

    fun playPrevious(context: Context) {
        val q = _queue.value
        if (q.isEmpty()) return

        var prevIndex = _currentQueueIndex.value - 1
        if (prevIndex < 0) {
            prevIndex = if (_repeatMode.value == Player.REPEAT_MODE_ALL) q.size - 1 else 0
        }

        _currentQueueIndex.value = prevIndex
        val prevSong = q[prevIndex]
        playSong(context, prevSong, q)
    }

    fun seekTo(context: Context, positionMs: Long) {
        val player = getExoPlayer(context)
        player.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun toggleShuffle() {
        _shuffleEnabled.value = !_shuffleEnabled.value
    }

    fun cycleRepeatMode(context: Context) {
        val player = getExoPlayer(context)
        val nextMode = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = nextMode
        player.repeatMode = nextMode
    }

    fun setPlaybackSpeed(context: Context, speed: Float) {
        val player = getExoPlayer(context)
        _playbackSpeed.value = speed
        player.setPlaybackSpeed(speed)
        scope.launch {
            repository?.updatePlayback(true, false, speed)
        }
    }

    fun insertIntoQueueNext(song: Song) {
        val currentList = _queue.value.toMutableList()
        val currentIndex = _currentQueueIndex.value
        
        // Remove existing occurrence if any
        val existingIndex = currentList.indexOfFirst { it.id == song.id }
        if (existingIndex != -1) {
            currentList.removeAt(existingIndex)
        }
        
        // Find new index of current song after removal
        val adjustedCurrentIndex = if (currentIndex != -1) {
            currentList.indexOfFirst { it.id == _currentSong.value?.id }
        } else {
            -1
        }
        
        if (adjustedCurrentIndex == -1) {
            // Queue is empty or no current song, add at start
            currentList.add(0, song)
            _queue.value = currentList
            _currentQueueIndex.value = 0
            _currentSong.value = song
        } else {
            // Add right after the current song
            currentList.add(adjustedCurrentIndex + 1, song)
            _queue.value = currentList
            _currentQueueIndex.value = adjustedCurrentIndex
        }
    }

    fun removeFromQueue(songId: Long) {
        val currentList = _queue.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == songId }
        if (index != -1) {
            currentList.removeAt(index)
            _queue.value = currentList
            if (index == _currentQueueIndex.value) {
                // If we removed the active song, skip to next
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
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
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
            // Timer finished, fade out volume and pause player!
            fadeOutAndPause(context)
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
            fadeOutAndPause(context)
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingSec.value = 0L
        exoPlayer?.volume = 1.0f
    }

    private suspend fun fadeOutAndPause(context: Context) {
        val player = getExoPlayer(context)
        val steps = 10
        val delayInterval = 300L // total fade out over 3 seconds
        for (i in steps downTo 0) {
            player.volume = i.toFloat() / steps
            delay(delayInterval)
        }
        player.pause()
        // Reset volume for future playback
        player.volume = 1.0f
        cancelSleepTimer()
    }

    private fun startService(context: Context) {
        val intent = Intent(context.applicationContext, MusicService::class.java)
        context.startService(intent)
    }

    fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
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
            .setUri(song.path)
            .setMediaMetadata(mediaMetadata)
            .build()
    }
}
