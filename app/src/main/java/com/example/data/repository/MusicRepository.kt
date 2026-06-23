package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.data.Song
import com.example.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(private val musicDao: MusicDao) {

    // === Database Flow Mappings ===

    val allPlaylistsFlow: Flow<List<PlaylistEntity>> = musicDao.getAllPlaylists()

    val allSongStatsFlow: Flow<List<SongStatsEntity>> = musicDao.getAllSongStats()

    val allFavoritesFlow: Flow<List<Song>> = musicDao.getAllFavorites().map { entities ->
        entities.map { it.toSong() }
    }

    val recentSongsFlow: Flow<List<Song>> = musicDao.getRecentSongs().map { entities ->
        entities.map { it.toSong() }
    }

    fun isFavoriteFlow(songId: String): Flow<Boolean> = musicDao.isFavorite(songId)

    suspend fun isFavoriteOneShot(songId: String): Boolean = musicDao.isFavoriteOneShot(songId)

    // === Playlist Functions ===

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        musicDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun createSmartPlaylist(name: String, smartType: String, smartParam: String?): Long = withContext(Dispatchers.IO) {
        musicDao.insertPlaylist(
            PlaylistEntity(
                name = name,
                isSmart = true,
                smartType = smartType,
                smartParam = smartParam
            )
        )
    }

    suspend fun updateSongPlayStats(song: Song) = withContext(Dispatchers.IO) {
        // Insert standard row if didn't exist
        musicDao.insertSongStatsIgnore(
            SongStatsEntity(
                songId = song.id,
                playCount = 0,
                // Assign a reproducible date added (can simulate randomly or use system timestamp)
                dateAdded = System.currentTimeMillis()
            )
        )
        // Increment playcount
        musicDao.incrementPlayCount(song.id)
    }

    suspend fun initSongStatsForScannedSongs(songs: List<Song>) = withContext(Dispatchers.IO) {
        songs.forEach { song ->
            // Mark date first seen
            val mockDateAdded = if (song.id.startsWith("demo_")) {
                // Let's stagger dates for beautiful demo criteria filtering
                System.currentTimeMillis() - (song.id.substringAfter("demo_").toLongOrNull() ?: 1L) * 24 * 60 * 60 * 1000L
            } else {
                System.currentTimeMillis()
            }
            musicDao.insertSongStatsIgnore(
                SongStatsEntity(
                    songId = song.id,
                    playCount = 1, // Start with 1 so we have some data
                    dateAdded = mockDateAdded
                )
            )
        }
    }

    suspend fun renamePlaylist(id: Long, name: String) = withContext(Dispatchers.IO) {
        musicDao.renamePlaylist(id, name)
    }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        musicDao.deletePlaylist(id)
        musicDao.deletePlaylistSongs(id)
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> {
        return musicDao.getSongsForPlaylist(playlistId).map { entities ->
            entities.map { it.toSong() }
        }
    }

    suspend fun addSongToPlaylist(playlistId: Long, song: Song) = withContext(Dispatchers.IO) {
        musicDao.insertPlaylistSong(
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                duration = song.duration,
                uriString = song.uriString,
                path = song.path,
                albumArtUri = song.albumArtUri,
                folder = song.folder
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) = withContext(Dispatchers.IO) {
        musicDao.removeSongFromPlaylist(playlistId, songId)
    }

    // === Favorites Functions ===

    suspend fun addToFavorites(song: Song) = withContext(Dispatchers.IO) {
        musicDao.insertFavorite(
            FavoriteEntity(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                duration = song.duration,
                uriString = song.uriString,
                path = song.path,
                albumArtUri = song.albumArtUri,
                folder = song.folder
            )
        )
    }

    suspend fun removeFromFavorites(songId: String) = withContext(Dispatchers.IO) {
        musicDao.deleteFavorite(songId)
    }

    // === Recently Played ===

    suspend fun addToRecentlyPlayed(song: Song) = withContext(Dispatchers.IO) {
        musicDao.insertRecent(
            RecentEntity(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                duration = song.duration,
                uriString = song.uriString,
                path = song.path,
                albumArtUri = song.albumArtUri,
                folder = song.folder,
                timestamp = System.currentTimeMillis()
            )
        )
        musicDao.trimRecentlyPlayed()
    }


    // === Local Music Media Scanning ===

    suspend fun scanDeviceSongs(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val songsList = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )

        // Select only music files (is_music != 0)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val title = c.getString(titleCol) ?: "Unknown Track"
                    val artist = c.getString(artistCol) ?: "Unknown Artist"
                    val album = c.getString(albumCol) ?: "Unknown Album"
                    val duration = c.getLong(durationCol)
                    val path = c.getString(dataCol) ?: ""
                    val albumId = c.getLong(albumIdCol)

                    // Construct Uri for playback
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    // Try to get Album Art Uri
                    val albumArtUri = Uri.parse("content://media/external/audio/albumart")
                    val artUri = ContentUris.withAppendedId(albumArtUri, albumId).toString()

                    // Extract folder name
                    val file = File(path)
                    val folder = file.parentFile?.name ?: "Internal"

                    // Supported extensions check
                    val extension = file.extension.lowercase()
                    val isSupportedFormat = extension in listOf("mp3", "wav", "aac", "flac", "ogg", "m4a")

                    if (isSupportedFormat || path.isEmpty()) {
                        songsList.add(
                            Song(
                                id = id.toString(),
                                title = title,
                                artist = artist,
                                album = album,
                                duration = duration,
                                uriString = contentUri,
                                path = path,
                                albumArtUri = artUri,
                                folder = folder
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Return local songs, or fallback to beautiful built-in streaming/demonstration items if 0 tracks are loaded
        if (songsList.isEmpty()) {
            val demo = getDemoSongs()
            initSongStatsForScannedSongs(demo)
            demo
        } else {
            initSongStatsForScannedSongs(songsList)
            songsList
        }
    }

    private fun getDemoSongs(): List<Song> {
        return listOf(
            Song(
                id = "demo_1",
                title = "Retro Synthwave",
                artist = "Solfeggio",
                album = "Vintage Dreams",
                duration = 312000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&auto=format&fit=crop",
                folder = "SoundHelix Cloud"
            ),
            Song(
                id = "demo_2",
                title = "Acoustic Horizon",
                artist = "Wild Whistle",
                album = "Campfire Memories",
                duration = 425000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&auto=format&fit=crop",
                folder = "SoundHelix Cloud"
            ),
            Song(
                id = "demo_3",
                title = "Cyber Ambient",
                artist = "Modulation Lab",
                album = "Neon Skies",
                duration = 344000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&auto=format&fit=crop",
                folder = "SoundHelix Cloud"
            ),
            Song(
                id = "demo_4",
                title = "Velvet Midnight",
                artist = "Lofi Curator",
                album = "Late Night Whispers",
                duration = 302000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=500&auto=format&fit=crop",
                folder = "SoundHelix Cloud"
            ),
            Song(
                id = "demo_5",
                title = "Vibrant Forest",
                artist = "Echo Wanderer",
                album = "Ethereal Walks",
                duration = 296000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1446057032654-9d8885b7a3f3?w=500&auto=format&fit=crop",
                folder = "SoundHelix Cloud"
            )
        )
    }

    // === Extension Conversion Helpers ===

    private fun FavoriteEntity.toSong() = Song(
        id = songId,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        uriString = uriString,
        path = path,
        isFavorite = true,
        albumArtUri = albumArtUri,
        folder = folder
    )

    private fun RecentEntity.toSong() = Song(
        id = songId,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        uriString = uriString,
        path = path,
        isFavorite = false, // Flow check is done dynamically
        albumArtUri = albumArtUri,
        folder = folder
    )

    private fun PlaylistSongEntity.toSong() = Song(
        id = songId,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        uriString = uriString,
        path = path,
        isFavorite = false,
        albumArtUri = albumArtUri,
        folder = folder
    )
}
