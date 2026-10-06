package ru.netology.mediaplayer.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import kotlinx.coroutines.withContext
import ru.netology.mediaplayer.dto.Album
import com.google.gson.Gson

class AlbumRepository(private val okHttpClient: OkHttpClient) {
    private val baseUrl = "https://raw.githubusercontent.com/netology-code/andad-homeworks/master/09_multimedia/data/"
    private val albumJsonUrl = "$baseUrl/album.json"

    suspend fun getAlbum(): Album = withContext(Dispatchers.IO) {
        val request = okhttp3.Request.Builder()
            .url(albumJsonUrl)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IllegalStateException("Failed to load album: ${response.code}")
        }
        val json = response.body?.string() ?: throw IllegalStateException("Empty response")

        Log.d("MEDIA_DEBUG", "Raw JSON: $json")

        val gson = Gson()
        var album = gson.fromJson(json, Album::class.java)

        val durationsMs = listOf(
            150_000L,
            185_000L,
            210_000L,
            195_000L,
            170_000L,
            225_000L,
            200_000L,
            165_000L,
            230_000L,
            180_000L,
            240_000L,
            205_000L,
            190_000L,
            250_000L,
            215_000L,
        )

        album = album.copy(
            tracks = album.tracks.mapIndexed { index, track ->
                val duration = if (index < durationsMs.size) {
                    durationsMs[index]
                } else {
                    180_000L
                }
                track.copy(durationMs = duration)
            }
        )

        Log.d(
            "MEDIA_DEBUG",
            "Final durations: ${album.tracks.joinToString { "${it.id}:${it.durationMs}" }}"
        )
        album
    }

    fun trackUrl(fileName: String): String = baseUrl + fileName
}