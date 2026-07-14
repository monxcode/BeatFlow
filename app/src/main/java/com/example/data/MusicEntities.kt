package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["path"], unique = true),
        Index(value = ["isFavorite"])
    ]
)
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val duration: Long, // in ms
    val size: Long, // in bytes
    val dateAdded: Long,
    val playCount: Int = 0,
    val isFavorite: Boolean = false,
    val folderName: String,
    val artworkUri: String? = null
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val isSystem: Boolean = false,
    val dateCreated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId"), Index("songId")]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long
)

@Entity(tableName = "listening_stats")
data class ListeningStats(
    @PrimaryKey val date: String, // format: YYYY-MM-DD
    val listeningTimeMs: Long = 0,
    val songsPlayed: Int = 0
)
