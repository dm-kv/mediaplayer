package ru.netology.mediaplayer.ui

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.OkHttpClient
import ru.netology.mediaplayer.adapter.TracksAdapter
import ru.netology.mediaplayer.dto.Album
import ru.netology.mediaplayer.repository.AlbumRepository
import ru.netology.mediaplayer.viewmodel.MediaUiState
import ru.netology.mediaplayer.viewmodel.MediaViewModel
import ru.netology.mediaplayer.R

class AppActivity : AppCompatActivity() {

    private lateinit var viewModel: MediaViewModel
    private lateinit var adapter: TracksAdapter

    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var seekBar: SeekBar

    @SuppressLint("WrongConstant")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_app)

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val repository = AlbumRepository(okHttpClient)
        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MediaViewModel(repository, applicationContext) as T
                }
            }
        )[MediaViewModel::class.java]

        findViewById<View>(R.id.main).setOnApplyWindowInsetsListener { v, insets ->
            val s = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(s.left, s.top, s.right, s.bottom)
            insets
        }

        setupRecyclerView()
        observeUiState()

        btnPrev = findViewById(R.id.btn_prev)
        btnNext = findViewById(R.id.btn_next)
        seekBar = findViewById(R.id.seek_bar)

        btnPrev.setOnClickListener {
            viewModel.previousTrack()
        }
        btnNext.setOnClickListener {
            viewModel.nextTrack()
        }

        findViewById<Button>(R.id.play_global).setOnClickListener {
            viewModel.playPause()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewModel.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                viewModel.setUserSeeking(true)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                viewModel.setUserSeeking(false)
            }
        })
    }

    private fun setupRecyclerView() {
        adapter = TracksAdapter(
            onTrackClick = { index -> viewModel.playTrack(index) }
        )
        val recyclerView = findViewById<RecyclerView>(R.id.tracks_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%d:%02d", minutes, secs)
    }

    private fun observeUiState() {
        viewModel.uiState.observe(this) { state ->
            Log.d("MEDIA_DEBUG", "OBSERVE: state=$state")

            when (state) {
                is MediaUiState.Loading -> {
                    Log.d("MEDIA_DEBUG", "Loading: showing progress")
                    findViewById<ProgressBar>(R.id.progress).visibility = View.VISIBLE
                    findViewById<TextView>(R.id.error_text).visibility = View.GONE
                }
                is MediaUiState.Loaded -> {
                    Log.d("MEDIA_DEBUG", "Loaded: tracks count = ${state.album.tracks.size}")
                    findViewById<ProgressBar>(R.id.progress).visibility = View.GONE
                    findViewById<TextView>(R.id.error_text).visibility = View.GONE
                    adapter.setData(state.album.tracks, currentIndex = 0, isPlaying = false)
                    setupAlbumInfo(state.album)
                }
                is MediaUiState.Playing -> {
                    Log.d("MEDIA_DEBUG", "Playing: tracks count = ${state.tracks.size}, index = ${state.currentIndex}")
                    findViewById<ProgressBar>(R.id.progress).visibility = View.GONE
                    findViewById<TextView>(R.id.error_text).visibility = View.GONE
                    adapter.setData(state.tracks, state.currentIndex, state.isPlaying)
                    findViewById<TextView>(R.id.album_title).text = state.albumTitle
                    findViewById<TextView>(R.id.artist_name).text = state.artist
                    val playBtn = findViewById<Button>(R.id.play_global)
                    playBtn.text = if (state.isPlaying) "⏸" else "▶"
                    findViewById<TextView>(R.id.time_current).text = formatTime(state.currentSeconds)
                    findViewById<TextView>(R.id.time_remaining).text = formatTime(state.remainingSeconds)
                    seekBar.progress = state.progressPercent
                }
                is MediaUiState.Error -> {
                    Log.e("MEDIA_DEBUG", "Error: ${state.message}")
                    findViewById<ProgressBar>(R.id.progress).visibility = View.GONE
                    findViewById<TextView>(R.id.error_text).apply {
                        text = state.message
                        visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun setupAlbumInfo(album: Album) {
        findViewById<TextView>(R.id.album_title).text = album.title
        findViewById<TextView>(R.id.artist_name).text = album.artist
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}