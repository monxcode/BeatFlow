package com.example.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.R
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MusicService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        try {
            // Build ExoPlayer with automatic audio focus handling and headphone disconnection handling
            player = ExoPlayer.Builder(this)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true // Manage audio focus automatically
                )
                .setHandleAudioBecomingNoisy(true)
                .build()

            // Intent to open MainActivity when clicking notification
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 
                0, 
                intent, 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Build MediaSession with CustomSessionCallback
            mediaSession = MediaSession.Builder(this, player!!)
                .setSessionActivity(pendingIntent)
                .setCallback(CustomSessionCallback())
                .build()

            // Listen to player events to update Favorite button layout dynamically
            player!!.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    updateFavoriteButtonLayout()
                }
            })

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.launch {
            PlaybackManager.flushListeningTime()
        }
        try {
            mediaSession?.run {
                release()
                mediaSession = null
            }
            player?.run {
                release()
                player = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = player
        if (p == null || !p.playWhenReady || p.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    // Custom Callback to handle Favorite actions and custom layout commands
    private inner class CustomSessionCallback : MediaSession.Callback {
        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val sessionCommands = connectionResult.availableSessionCommands.buildUpon()
            sessionCommands.add(SessionCommand("ACTION_FAVORITE", Bundle.EMPTY))
            
            // Build initial Favorite button layout based on active song's favorite status
            val currentMediaItem = player?.currentMediaItem
            val songId = currentMediaItem?.mediaId?.toLongOrNull()
            val isFav = if (songId != null) {
                PlaybackManager.isSongFavoriteInCache(songId)
            } else {
                false
            }
            val favoriteButton = buildFavoriteButton(isFav)

            session.setCustomLayout(listOf(favoriteButton))

            return MediaSession.ConnectionResult.accept(
                sessionCommands.build(),
                connectionResult.availablePlayerCommands
            )
        }

        @OptIn(UnstableApi::class)
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == "ACTION_FAVORITE") {
                val currentMediaItem = player?.currentMediaItem
                val songId = currentMediaItem?.mediaId?.toLongOrNull()
                if (songId != null) {
                    serviceScope.launch {
                        val isFav = PlaybackManager.toggleFavoriteFromService(this@MusicService, songId)
                        updateFavoriteButtonLayout(isFav)
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
    }

    @OptIn(UnstableApi::class)
    private fun buildFavoriteButton(isFav: Boolean): CommandButton {
        return CommandButton.Builder()
            .setDisplayName(if (isFav) "Unfavorite" else "Favorite")
            .setIconResId(if (isFav) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border)
            .setSessionCommand(SessionCommand("ACTION_FAVORITE", Bundle.EMPTY))
            .build()
    }

    @OptIn(UnstableApi::class)
    private fun updateFavoriteButtonLayout(isFav: Boolean? = null) {
        val session = mediaSession ?: return
        val currentMediaItem = player?.currentMediaItem
        val songId = currentMediaItem?.mediaId?.toLongOrNull() ?: return
        
        val actualIsFav = isFav ?: PlaybackManager.isSongFavoriteInCache(songId)
        val favoriteButton = buildFavoriteButton(actualIsFav)
        session.setCustomLayout(listOf(favoriteButton))
    }
}
