package com.example.data

import java.io.Serializable

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uriString: String,
    val path: String,
    val isFavorite: Boolean = false,
    val albumArtUri: String? = null,
    val folder: String = ""
) : Serializable
