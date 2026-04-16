package com.Fch.vl.viewmodel

import android.app.Application
import android.view.Surface
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class VlPlayer(application: Application) : AndroidViewModel(application) {

    public var playingButtonState = 0

    private var currentPlayer: ExoPlayer? = null
    private var currentSurface: Surface? = null
    private var videoSize: String = "0x0"
    var onVideoSizeChanged: ((String) -> Unit)? = null

    fun playingVideo(path: String) {
        // 释放旧的播放器
        currentPlayer?.release()
        // 创建新播放器
        val newPlayer = ExoPlayer.Builder(getApplication()).build().apply {
            addListener(object : Player.Listener {
                override fun onVideoSizeChanged(size: androidx.media3.common.VideoSize) {
                    val sizeStr = "${size.width}x${size.height}"
                    this@`VlPlayer`.videoSize = sizeStr
                    onVideoSizeChanged?.invoke(sizeStr)
                }
            })
            // 如果有 Surface，立即绑定
            currentSurface?.let { setVideoSurface(it) }
        }
        currentPlayer = newPlayer

        // 设置媒体并播放
        val mediaItem = MediaItem.fromUri(path)
        newPlayer.setMediaItem(mediaItem)
        newPlayer.prepare()
        newPlayer.play()
    }

    fun pauseVideo() {
        currentPlayer?.pause()
    }

    fun resumeVideo() {
        currentPlayer?.play()
    }

    fun isPlaying(): Boolean = currentPlayer?.isPlaying == true

    fun setSurface(surface: Surface?) {
        // 如果传入的是 null，显式清除播放器的渲染目标
        if (surface == null) {
            currentPlayer?.setVideoSurface(null)
            currentSurface = null
        } else {
            // 如果传入的是新 Surface，且与当前不同，则更新
            if (currentSurface != surface) {
                currentSurface = surface
                currentPlayer?.setVideoSurface(surface)
                if (currentPlayer?.playbackState == Player.STATE_READY) {
                    // 轻轻挪动 1ms 往往能强迫画面刷新
                    // currentPlayer?.seekTo(currentPlayer!!.currentPosition)
                }
            }
        }
    }

    fun getVideoSize(): String = videoSize

    fun releaseVideo() {
        currentPlayer?.release()
        currentPlayer = null
    }

    fun getDuration(): Long = currentPlayer?.duration?.takeIf { it > 0 } ?: 0L
    fun getCurrentPosition(): Long = currentPlayer?.currentPosition ?: 0L
    fun setPosition(position: Long) {
        currentPlayer?.seekTo(position)
    }

    fun setSpeed(speed: Float) {
        currentPlayer?.setPlaybackSpeed(speed.coerceIn(0.1f, 8.0f))
    }

    override fun onCleared() {
        super.onCleared()
        currentPlayer?.release()
        currentPlayer = null
    }
}
