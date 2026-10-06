package ru.netology.mediaplayer.dto

data class Album(
    val title: String,
    val artist: String,
    val cover: String?,
    val tracks: List<Track>,
)

data class Track(
    val id: Int,
    val file: String,
    val title: String? = null,
    val durationMs: Long = 0L,
)