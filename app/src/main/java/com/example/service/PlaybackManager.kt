package com.example.service

import android.content.Context
import android.media.audiofx.Equalizer
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlaybackRepeatMode {
    NONE, ONE, ALL
}

object PlaybackManager {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    private var exoPlayer: ExoPlayer? = null
    private var nativeEqualizer: Equalizer? = null

    // State flows
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _progressMs = MutableStateFlow(0L)
    val progressMs = _progressMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs = _durationMs.asStateFlow()

    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode = _shuffleMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(PlaybackRepeatMode.ALL)
    val repeatMode = _repeatMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed = _playbackSpeed.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes = _sleepTimerMinutes.asStateFlow()

    private val _equalizerEnabled = MutableStateFlow(false)
    val equalizerEnabled = _equalizerEnabled.asStateFlow()

    private val _equalizerBands = MutableStateFlow(listOf(0, 0, 0, 0, 0)) //dB (-15 to 15)
    val equalizerBands = _equalizerBands.asStateFlow()

    private var originalPlaylist: List<Song> = emptyList()
    private var playingQueue: List<Song> = emptyList()
    private var currentSongIndex: Int = -1

    var onSongChangedCallback: ((Song) -> Unit)? = null

    fun getOrInitPlayer(context: Context): ExoPlayer {
        if (exoPlayer == null) {
            val player = ExoPlayer.Builder(context.applicationContext)
                .setAudioAttributes(androidx.media3.common.AudioAttributes.DEFAULT, true)
                .build()
            
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _durationMs.value = player.duration.coerceAtLeast(0L)
                        startProgressPolling()
                        setupEqualizer(player.audioSessionId)
                    } else if (playbackState == Player.STATE_ENDED) {
                        handlePlaybackEnded()
                    }
                    com.example.widget.MusicWidgetProvider.updateAllWidgets(context.applicationContext)
                }

                override fun onIsPlayingChanged(isPlayingChange: Boolean) {
                    _isPlaying.value = isPlayingChange
                    if (isPlayingChange) {
                        startProgressPolling()
                    } else {
                        stopProgressPolling()
                    }
                    com.example.widget.MusicWidgetProvider.updateAllWidgets(context.applicationContext)
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val song = playingQueue.getOrNull(player.currentMediaItemIndex)
                    if (song != null) {
                        currentSongIndex = player.currentMediaItemIndex
                        _currentSong.value = song
                        onSongChangedCallback?.invoke(song)
                    }
                    com.example.widget.MusicWidgetProvider.updateAllWidgets(context.applicationContext)
                }
            })
            
            exoPlayer = player
        }
        return exoPlayer!!
    }

    private fun handlePlaybackEnded() {
        when (_repeatMode.value) {
            PlaybackRepeatMode.ONE -> {
                exoPlayer?.seekTo(0)
                exoPlayer?.play()
            }
            PlaybackRepeatMode.ALL -> {
                next()
            }
            PlaybackRepeatMode.NONE -> {
                if (currentSongIndex < playingQueue.size - 1) {
                    next()
                } else {
                    pause()
                }
            }
        }
    }

    fun play(songs: List<Song>, startIndex: Int, context: Context) {
        val player = getOrInitPlayer(context)
        originalPlaylist = songs
        currentSongIndex = startIndex

        updatePlayingQueue()

        player.clearMediaItems()
        playingQueue.forEach { song ->
            player.addMediaItem(MediaItem.fromUri(song.uriString))
        }

        player.seekTo(currentSongIndex, 0)
        player.prepare()
        player.play()
        
        _currentSong.value = playingQueue.getOrNull(currentSongIndex)
        _isPlaying.value = true
        _durationMs.value = player.duration.coerceAtLeast(0)
        startProgressPolling()

        _currentSong.value?.let { onSongChangedCallback?.invoke(it) }
        com.example.widget.MusicWidgetProvider.updateAllWidgets(context.applicationContext)
    }

    fun resume() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun next() {
        val player = exoPlayer ?: return
        if (playingQueue.isEmpty()) return

        currentSongIndex = (currentSongIndex + 1) % playingQueue.size
        player.seekTo(currentSongIndex, 0)
        player.prepare()
        player.play()
    }

    fun previous() {
        val player = exoPlayer ?: return
        if (playingQueue.isEmpty()) return

        currentSongIndex = if (currentSongIndex - 1 < 0) {
            playingQueue.size - 1
        } else {
            currentSongIndex - 1
        }
        player.seekTo(currentSongIndex, 0)
        player.prepare()
        player.play()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _progressMs.value = positionMs
    }

    fun setShuffle(enabled: Boolean) {
        _shuffleMode.value = enabled
        val current = _currentSong.value
        updatePlayingQueue()
        
        // Find current song index in new queue to prevent playback interruption
        if (current != null) {
            currentSongIndex = playingQueue.indexOfFirst { it.id == current.id }
            if (currentSongIndex == -1) currentSongIndex = 0
            exoPlayer?.let { player ->
                // Re-build media items list in player
                player.clearMediaItems()
                playingQueue.forEach { song ->
                    player.addMediaItem(MediaItem.fromUri(song.uriString))
                }
                player.seekTo(currentSongIndex, player.currentPosition)
            }
        }
    }

    fun setRepeatMode(mode: PlaybackRepeatMode) {
        _repeatMode.value = mode
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
    }

    // === System Sleep Timer ===

    fun startSleepTimer(minutes: Int) {
        _sleepTimerMinutes.value = minutes
        sleepTimerJob?.cancel()
        sleepTimerJob = scope.launch {
            var remaining = minutes
            while (remaining > 0) {
                delay(60000L) // 1 minute
                remaining--
                _sleepTimerMinutes.value = remaining
                if (remaining <= 0) {
                    pause()
                    _sleepTimerMinutes.value = null
                    break
                }
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = null
    }

    // === Sound Equalizer (Native) ===

    private fun setupEqualizer(audioSessionId: Int) {
        if (nativeEqualizer == null || nativeEqualizer?.enabled == false) {
            try {
                val eq = Equalizer(0, audioSessionId)
                eq.enabled = _equalizerEnabled.value
                val bandsCount = eq.numberOfBands.toInt()
                
                // Set existing bands level
                val levels = _equalizerBands.value
                for (i in 0 until bandsCount.coerceAtMost(5)) {
                    val midLevel = (eq.bandLevelRange[0] + eq.bandLevelRange[1]) / 2 // db level representation
                    // map dB -15 to +15 into band min/max level
                    val levelDb = levels.getOrNull(i) ?: 0
                    val mappedLevel = mapDbToBandLevel(levelDb, eq.bandLevelRange[0], eq.bandLevelRange[1])
                    eq.setBandLevel(i.toShort(), mappedLevel.toShort())
                }
                nativeEqualizer = eq
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun mapDbToBandLevel(db: Int, minLevel: Short, maxLevel: Short): Int {
        // level dB is between -15 and +15
        // scale linearly to minLevel and maxLevel (which represent hundredths of dB)
        val progress = (db + 15) / 30.0
        val range = maxLevel - minLevel
        return (minLevel + (progress * range)).toInt()
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerEnabled.value = enabled
        nativeEqualizer?.enabled = enabled
    }

    @OptIn(UnstableApi::class)
    fun setEqualizerBandLevel(band: Int, db: Int) {
        val currentList = _equalizerBands.value.toMutableList()
        if (band in currentList.indices) {
            currentList[band] = db
            _equalizerBands.value = currentList
            
            nativeEqualizer?.let { eq ->
                try {
                    val mapped = mapDbToBandLevel(db, eq.bandLevelRange[0], eq.bandLevelRange[1])
                    eq.setBandLevel(band.toShort(), mapped.toShort())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // === Queue management ===

    private fun updatePlayingQueue() {
        if (_shuffleMode.value) {
            val current = _currentSong.value
            val list = originalPlaylist.filter { it.id != current?.id }.shuffled().toMutableList()
            if (current != null) {
                list.add(0, current)
            }
            playingQueue = list
        } else {
            playingQueue = originalPlaylist
        }
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    _progressMs.value = player.currentPosition
                }
                delay(500L)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
    }
}
