package com.example.data.api

import com.example.data.Song
import com.squareup.moshi.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface JamendoApi {
    @GET("v3.0/tracks/")
    suspend fun searchTracks(
        @Query("client_id") clientId: String = "56efcee2",
        @Query("format") format: String = "json",
        @Query("search") query: String,
        @Query("limit") limit: Int = 30
    ): JamendoResponse
}

data class JamendoResponse(
    val results: List<JamendoTrack>
)

data class JamendoTrack(
    val id: String,
    val name: String,
    val duration: Long, // in seconds
    @Json(name = "artist_name") val artistName: String,
    @Json(name = "album_name") val albumName: String,
    val image: String?,
    val audio: String
) {
    fun toSong(): Song {
        return Song(
            id = "spotify_$id", // Prefixed to distinguish it from local/demo songs
            title = name,
            artist = artistName,
            album = albumName,
            duration = duration * 1000, // convert to ms
            uriString = audio,
            path = audio,
            albumArtUri = image ?: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&auto=format&fit=crop",
            folder = "Spotify"
        )
    }
}

object SpotifyStreamingService {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.jamendo.com/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val api: JamendoApi = retrofit.create(JamendoApi::class.java)

    suspend fun searchSpotifyStream(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val response = api.searchTracks(query = query)
            response.results.map { it.toSong() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
