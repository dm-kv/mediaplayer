package ru.netology.mediaplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.netology.mediaplayer.dto.Track
import ru.netology.mediaplayer.R

class TracksAdapter(
    private val onTrackClick: (Int) -> Unit
) : RecyclerView.Adapter<TracksAdapter.TrackViewHolder>() {

    private var tracks: List<Track> = emptyList()
    private var currentTrackIndex: Int = -1
    private var isPlaying: Boolean = false

    fun setData(tracks: List<Track>, currentIndex: Int, isPlaying: Boolean) {
        this.tracks = tracks
        this.currentTrackIndex = currentIndex
        this.isPlaying = isPlaying
        notifyDataSetChanged()
    }

    inner class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.track_title)
        private val durationText: TextView = itemView.findViewById(R.id.track_duration)
        private val playPauseIcon: ImageView = itemView.findViewById(R.id.track_play_pause)

        fun bind(position: Int) {
            val track = tracks[position]
            titleText.text = track.title ?: "Трек ${track.id}"
            durationText.text = formatDuration(track.durationMs)

            if (position == currentTrackIndex && isPlaying) {
                playPauseIcon.setImageResource(R.drawable.ic_pause)
            } else {
                playPauseIcon.setImageResource(R.drawable.ic_play)
            }

            itemView.setOnClickListener {
                onTrackClick(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track, parent, false)
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = tracks.size

    private fun formatDuration(ms: Long): String {
        if (ms <= 0) return "00:00"
        val totalSeconds = (ms / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return if (minutes > 0) {
            String.format("%d:%02d", minutes, seconds)
        } else {
            String.format("%d", seconds)
        }
    }
}