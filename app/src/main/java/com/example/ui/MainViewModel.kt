package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Song
import com.example.data.db.MusicDatabase
import com.example.data.db.SongStatsEntity
import com.example.data.repository.MusicRepository
import com.example.service.PlaybackManager
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface Screen {
    object Splash : Screen
    object Home : Screen
    data class PlaylistDetails(val playlistId: Long, val name: String) : Screen
    data class AlbumDetails(val albumName: String) : Screen
    data class ArtistDetails(val artistName: String) : Screen
    data class FolderDetails(val folderPath: String) : Screen
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val repository: MusicRepository, private val context: Context) : ViewModel() {

    // Theme selector with offline persistence
    private val prefs = context.getSharedPreferences("music_star_prefs", Context.MODE_PRIVATE)
    private val _selectedTheme = MutableStateFlow<AppTheme>(
        AppTheme.values().find { it.id == prefs.getString("selected_theme", AppTheme.PILOT_GOLD_BLUE.id) }
            ?: AppTheme.PILOT_GOLD_BLUE
    )
    val selectedTheme = _selectedTheme.asStateFlow()

    fun setTheme(theme: AppTheme) {
        _selectedTheme.value = theme
        prefs.edit().putString("selected_theme", theme.id).apply()
    }

    // Device Songs State
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs = _allSongs.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    // Screen State
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Splash)
    val currentScreen = _currentScreen.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Search Filter Type & Sorting
    val searchFilterType = MutableStateFlow("ALL") // "ALL", "SONG", "ALBUM", "ARTIST"
    val searchSortOrder = MutableStateFlow("ASC") // "ASC", "DESC"

    // Dialog state
    var showCreatePlaylistDialog = MutableStateFlow(false)
    var showRenamePlaylistDialog = MutableStateFlow<Long?>(null)

    // Player states shared from PlaybackManager
    val currentSong = PlaybackManager.currentSong
    val isPlaying = PlaybackManager.isPlaying
    val progressMs = PlaybackManager.progressMs
    val durationMs = PlaybackManager.durationMs
    val shuffleMode = PlaybackManager.shuffleMode
    val repeatMode = PlaybackManager.repeatMode
    val playbackSpeed = PlaybackManager.playbackSpeed
    val sleepTimerMinutes = PlaybackManager.sleepTimerMinutes
    val equalizerEnabled = PlaybackManager.equalizerEnabled
    val equalizerBands = PlaybackManager.equalizerBands

    // Playlists & Favorites Flows
    val playlists = repository.allPlaylistsFlow
    val favorites = repository.allFavoritesFlow
    val recentSongs = repository.recentSongsFlow
    val songStatsList = repository.allSongStatsFlow

    // Spotify API Streaming Section
    private val _spotifyAuthToken = MutableStateFlow<String?>(null)
    val spotifyAuthToken = _spotifyAuthToken.asStateFlow()

    private val _spotifyUser = MutableStateFlow<String?>(null)
    val spotifyUser = _spotifyUser.asStateFlow()

    private val _isSpotifyAuthenticating = MutableStateFlow(false)
    val isSpotifyAuthenticating = _isSpotifyAuthenticating.asStateFlow()

    private val _spotifySearchResults = MutableStateFlow<List<Song>>(emptyList())
    val spotifySearchResults = _spotifySearchResults.asStateFlow()

    private val _isSpotifySearching = MutableStateFlow(false)
    val isSpotifySearching = _isSpotifySearching.asStateFlow()

    // Real-time suggestions as the user types
    val searchSuggestions = combine(_allSongs, _searchQuery) { songs, query ->
        if (query.isBlank()) {
            emptyList()
        } else {
            val suggestions = mutableListOf<String>()
            songs.forEach { song ->
                if (song.title.contains(query, ignoreCase = true) && !suggestions.contains(song.title)) {
                    suggestions.add(song.title)
                }
                if (song.artist.contains(query, ignoreCase = true) && !suggestions.contains(song.artist)) {
                    suggestions.add(song.artist)
                }
                if (song.album.contains(query, ignoreCase = true) && !suggestions.contains(song.album)) {
                    suggestions.add(song.album)
                }
            }
            suggestions.take(6)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Computed categories for Local Music
    val albumsFlow = _allSongs.map { songs ->
        songs.groupBy { it.album }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val artistsFlow = _allSongs.map { songs ->
        songs.groupBy { it.artist }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val foldersFlow = _allSongs.map { songs ->
        songs.groupBy { it.folder }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    // Combined local search results matching user query
    val searchResults = combine(_allSongs, _searchQuery) { songs, query ->
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

    // Unified filtered and sorted results (Local and Spotify)
    val filteredAndSortedSearchResults = combine(
        searchResults,
        _spotifySearchResults,
        searchFilterType,
        searchSortOrder,
        _searchQuery
    ) { localResults, spotifyResults, filter, sort, query ->
        // Merge lists depending on whether Spotify results are loaded
        val combined = if (spotifyAuthToken.value != null && query.isNotBlank()) {
            localResults + spotifyResults
        } else {
            localResults
        }

        // Apply visual type-filtering
        var filtered = when (filter) {
            "SONG" -> combined
            "ALBUM" -> {
                // Return unique tracks representing distinct matching albums
                combined.distinctBy { it.album }
            }
            "ARTIST" -> {
                // Return unique tracks representing distinct matching artists
                combined.distinctBy { it.artist }
            }
            else -> combined
        }

        // Apply Alphabetical Ordering
        filtered = if (sort == "ASC") {
            filtered.sortedBy { it.title.lowercase() }
        } else {
            filtered.sortedByDescending { it.title.lowercase() }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Automatically append newly started tracks to Recently Played and update play statistics
        PlaybackManager.onSongChangedCallback = { song ->
            viewModelScope.launch {
                repository.addToRecentlyPlayed(song)
                repository.updateSongPlayStats(song)
            }
        }

        // Automatically trigger Spotify web queries as search input changes
        viewModelScope.launch {
            _searchQuery.collect { query ->
                if (_spotifyAuthToken.value != null && query.isNotBlank()) {
                    searchSpotify(query)
                }
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setQuery(query: String) {
        _searchQuery.value = query
    }

    fun scanMusic(context: Context) {
        viewModelScope.launch {
            _isScanning.value = true
            val scanned = repository.scanDeviceSongs(context)
            _allSongs.value = scanned
            _isScanning.value = false
        }
    }

    // === Playlist Controls ===

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun createSmartPlaylist(name: String, smartType: String, paramDaysLimit: String?) {
        viewModelScope.launch {
            repository.createSmartPlaylist(name, smartType, paramDaysLimit)
        }
    }

    fun renamePlaylist(id: Long, name: String) {
        viewModelScope.launch {
            repository.renamePlaylist(id, name)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, song)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> {
        return playlists.flatMapLatest { pList ->
            val playlist = pList.find { it.id == playlistId }
            if (playlist != null && playlist.isSmart) {
                combine(
                    _allSongs,
                    recentSongs,
                    songStatsList
                ) { songs, recents, statsList ->
                    val statsMap = statsList.associateBy { it.songId }
                    when (playlist.smartType) {
                        "recently_played" -> {
                            recents
                        }
                        "most_played" -> {
                            // Sort all accessible tracks by play counts descending
                            songs.filter { (statsMap[it.id]?.playCount ?: 0) > 0 }
                                .sortedByDescending { statsMap[it.id]?.playCount ?: 0 }
                        }
                        "added_range" -> {
                            // Filter by date added cutoff range days
                            val daysRange = playlist.smartParam?.toLongOrNull() ?: 30L
                            val cutoff = System.currentTimeMillis() - daysRange * 24 * 60 * 60 * 1000L
                            songs.filter { song ->
                                val date = statsMap[song.id]?.dateAdded ?: System.currentTimeMillis()
                                date >= cutoff
                            }
                        }
                        else -> emptyList()
                    }
                }
            } else {
                repository.getSongsForPlaylist(playlistId)
            }
        }
    }

    // === M3U Playlist Import & Export ===

    fun exportM3U(songs: List<Song>): String {
        val sb = StringBuilder()
        sb.append("#EXTM3U\n")
        songs.forEach { song ->
            sb.append("#EXTINF:${song.duration / 1000},${song.artist} - ${song.title}\n")
            sb.append("${song.uriString}\n")
        }
        return sb.toString()
    }

    fun importM3U(playlistName: String, m3uContent: String, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val lines = m3uContent.lineSequence()
            val parsedQueries = mutableListOf<String>()
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    parsedQueries.add(trimmed)
                } else if (trimmed.startsWith("#EXTINF:")) {
                    val label = trimmed.substringAfter(",", "")
                    if (label.isNotEmpty()) {
                        parsedQueries.add(label)
                    }
                }
            }

            // Match parsed lines against available cached local physical/demo songs
            val available = _allSongs.value
            val matchedSongs = mutableListOf<Song>()
            parsedQueries.distinct().forEach { item ->
                val match = available.find { song ->
                    song.uriString == item ||
                    song.path == item ||
                    "${song.artist} - ${song.title}".equals(item, ignoreCase = true) ||
                    song.title.equals(item, ignoreCase = true)
                }
                if (match != null && !matchedSongs.contains(match)) {
                    matchedSongs.add(match)
                }
            }

            // Create standard playlist name inside Room
            val newPlaylistId = repository.createPlaylist(playlistName)
            matchedSongs.forEach { song ->
                repository.addSongToPlaylist(newPlaylistId, song)
            }
            onComplete(newPlaylistId)
        }
    }

    // === Spotify Account Authenticate Simulation ===

    fun loginToSpotify(username: String) {
        viewModelScope.launch {
            _isSpotifyAuthenticating.value = true
            kotlinx.coroutines.delay(1200) // Animated network latency look and feel
            _spotifyAuthToken.value = "mock_spotify_token_" + System.currentTimeMillis()
            _spotifyUser.value = username.ifBlank { "AcousticVibe Lover" }
            _isSpotifyAuthenticating.value = false
            // Prefetch nice tracks immediately
            searchSpotify("acoustic")
        }
    }

    fun logoutSpotify() {
        _spotifyAuthToken.value = null
        _spotifyUser.value = null
        _spotifySearchResults.value = emptyList()
    }

    fun searchSpotify(query: String) {
        if (_spotifyAuthToken.value == null || query.isBlank()) {
            _spotifySearchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSpotifySearching.value = true
            val results = com.example.data.api.SpotifyStreamingService.searchSpotifyStream(query)
            _spotifySearchResults.value = results
            _isSpotifySearching.value = false
        }
    }

    // === Favorite Controls ===

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val isFav = repository.isFavoriteOneShot(song.id)
            if (isFav) {
                repository.removeFromFavorites(song.id)
            } else {
                repository.addToFavorites(song)
            }
        }
    }

    fun isFavoriteFlow(songId: String): Flow<Boolean> {
        return repository.isFavoriteFlow(songId)
    }

    // === Player Operations ===

    fun playSong(songs: List<Song>, startIndex: Int, context: Context) {
        PlaybackManager.play(songs, startIndex, context)
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
            PlaybackManager.pause()
        } else {
            PlaybackManager.resume()
        }
    }

    fun next() = PlaybackManager.next()

    fun previous() = PlaybackManager.previous()

    fun seekTo(positionMs: Long) = PlaybackManager.seekTo(positionMs)

    fun toggleShuffle() = PlaybackManager.setShuffle(!shuffleMode.value)

    fun nextRepeatMode() {
        val current = repeatMode.value
        val nextMode = when (current) {
            com.example.service.PlaybackRepeatMode.NONE -> com.example.service.PlaybackRepeatMode.ALL
            com.example.service.PlaybackRepeatMode.ALL -> com.example.service.PlaybackRepeatMode.ONE
            com.example.service.PlaybackRepeatMode.ONE -> com.example.service.PlaybackRepeatMode.NONE
        }
        PlaybackManager.setRepeatMode(nextMode)
    }

    fun setSpeed(speed: Float) = PlaybackManager.setSpeed(speed)

    fun startSleepTimer(minutes: Int) = PlaybackManager.startSleepTimer(minutes)

    fun cancelSleepTimer() = PlaybackManager.cancelSleepTimer()

    fun setEqualizerEnabled(enabled: Boolean) = PlaybackManager.setEqualizerEnabled(enabled)

    fun setEqualizerBandLevel(band: Int, db: Int) = PlaybackManager.setEqualizerBandLevel(band, db)

    // Factory Class for dependencies injection
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                val database = MusicDatabase.getDatabase(context)
                val repository = MusicRepository(database.musicDao())
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(repository, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
