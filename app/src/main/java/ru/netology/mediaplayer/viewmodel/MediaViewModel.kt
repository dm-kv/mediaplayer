package ru.netology.mediaplayer.viewmodel

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.netology.mediaplayer.dto.Album
import ru.netology.mediaplayer.dto.Track
import ru.netology.mediaplayer.repository.AlbumRepository

class MediaViewModel(
    private val repository: AlbumRepository,
    private val context: Context
) : ViewModel() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentTrackIndex = 0
    private var isPlaying = false
    private var album: Album? = null
    private var isUserSeeking = false
    private var isSwitchingTrack = false

    val uiState = MutableLiveData<MediaUiState>(MediaUiState.Loading)

    init {
        loadAlbum()
    }

    private fun loadAlbum() = viewModelScope.launch {
        uiState.value = MediaUiState.Loading
        try {
            album = repository.getAlbum()
            uiState.value = MediaUiState.Loaded(album!!)
        } catch (e: Exception) {
            Log.e("MEDIA_DEBUG", "loadAlbum FAILED", e)
            uiState.value = MediaUiState.Error(e.message ?: "Unknown error")
        }
    }

    fun playPause() {
        album ?: return
        if (isPlaying) pause() else play()
    }

    fun playTrack(index: Int) {
        album ?: return
        val tracks = album!!.tracks
        if (index < 0 || index >= tracks.size) return

        Log.d("MEDIA_DEBUG", "playTrack: index=$index, currentTrackIndex=$currentTrackIndex")

        if (index == currentTrackIndex) {
            playPause()
        } else {
            currentTrackIndex = index
            stopPlayback()
            play()
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
    }

    private fun play() {
        val track = album?.tracks?.getOrNull(currentTrackIndex) ?: return
        val url = repository.trackUrl(track.file)

        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, Uri.parse(url))
            prepareAsync()
        }.also { mp ->
            mp.setOnPreparedListener {
                it.start()
                isPlaying = true
                isSwitchingTrack = false
                updateState()
                startProgressTracking()
            }
            mp.setOnCompletionListener {
                nextTrack()
            }
            mp.setOnErrorListener { _, what, extra ->
                Log.e("MEDIA_DEBUG", "MediaPlayer error: what=$what extra=$extra on track $currentTrackIndex")
                isSwitchingTrack = false
                true
            }
        }
    }

    private fun pause() {
        mediaPlayer?.pause()
        isPlaying = false
        updateState()
    }

    fun previousTrack() {
        if (isSwitchingTrack) return
        album ?: return
        val tracks = album!!.tracks
        if (tracks.isEmpty()) return

        isSwitchingTrack = true
        currentTrackIndex = if (currentTrackIndex == 0) tracks.size - 1 else currentTrackIndex - 1
        stopPlayback()
        play()
    }

    fun nextTrack() {
        if (isSwitchingTrack) return
        if (album?.tracks?.isEmpty() == true) return

        isSwitchingTrack = true
        currentTrackIndex = (currentTrackIndex + 1) % (album?.tracks?.size ?: 1)
        stopPlayback()
        play()
    }

    fun seekTo(percent: Int) {
        val mp = mediaPlayer ?: return
        val duration = mp.duration
        if (duration <= 0) return
        val positionMs = (duration * percent / 100)
        mp.seekTo(positionMs)
        Log.d("MEDIA_DEBUG", "seekTo: $percent% -> $positionMs ms")
    }

    fun setUserSeeking(isSeeking: Boolean) {
        isUserSeeking = isSeeking
    }

    private fun startProgressTracking() = viewModelScope.launch {
        while (isPlaying && mediaPlayer != null) {
            delay(500)
            if (!isUserSeeking) {
                updateState()
            }
        }
    }

    private fun updateState() {
        val tracks = album?.tracks ?: emptyList()
        val current = tracks.getOrNull(currentTrackIndex)
        val mp = mediaPlayer
        val duration = mp?.duration ?: 0          // мс
        val position = mp?.currentPosition ?: 0   // мс

        val totalSeconds = if (duration > 0) (duration / 1000) else 0
        val currentSeconds = if (position > 0) (position / 1000) else 0
        val remainingSeconds = if (totalSeconds > currentSeconds) (totalSeconds - currentSeconds) else 0

        val percent = if (duration > 0) (position * 100 / duration) else 0
        val currentTitle = current?.title ?: "Трек ${current?.id}"

        uiState.value = MediaUiState.Playing(
            albumTitle = album?.title.orEmpty(),
            artist = album?.artist.orEmpty(),
            coverUrl = album?.cover,
            tracks = tracks,
            currentIndex = currentTrackIndex,
            isPlaying = isPlaying,
            currentTitle = currentTitle,
            progressPercent = percent,
            currentSeconds = currentSeconds,
            totalSeconds = totalSeconds,
            remainingSeconds = remainingSeconds
        )
    }
}

sealed class MediaUiState {
    object Loading : MediaUiState()
    data class Loaded(val album: Album) : MediaUiState()
    data class Playing(
        val albumTitle: String,
        val artist: String,
        val coverUrl: String?,
        val tracks: List<Track>,
        val currentIndex: Int,
        val isPlaying: Boolean,
        val currentTitle: String,
        val progressPercent: Int = 0,
        val currentSeconds: Int = 0,
        val totalSeconds: Int = 0,
        val remainingSeconds: Int = 0
    ) : MediaUiState()
    data class Error(val message: String) : MediaUiState()
}