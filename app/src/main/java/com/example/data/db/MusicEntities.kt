package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isSmart: Boolean = false,
    val smartType: String? = null, // "recently_played", "most_played", "added_range"
    val smartParam: String? = null  // bounds, options or limit
)

@Entity(tableName = "song_stats")
data class SongStatsEntity(
    @PrimaryKey val songId: String,
    val playCount: Int = 1,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uriString: String,
    val path: String,
    val albumArtUri: String?,
    val folder: String
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uriString: String,
    val path: String,
    val albumArtUri: String?,
    val folder: String
)

@Entity(tableName = "recently_played")
data class RecentEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uriString: String,
    val path: String,
    val albumArtUri: String?,
    val folder: String,
    val timestamp: Long
)
