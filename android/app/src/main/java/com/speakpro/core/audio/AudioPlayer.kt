package com.speakpro.core.audio

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

/**
 * 音频播放管理器 — 基于 Android [MediaPlayer]
 *
 * 支持播放本地文件和内存数据（ByteArray），自动检测 MP3 / WAV 格式。
 * 通过 [StateFlow] 暴露播放状态、进度和时长。
 *
 * 使用方式：
 * ```kotlin
 * val player = AudioPlayer()
 * player.play("/path/to/file.wav")
 * // 或
 * player.play(ttsBytes, "mp3")
 * ```
 */
class AudioPlayer {

    companion object {
        private const val TAG = "AudioPlayer"

        /** 进度更新间隔（毫秒） */
        private const val PROGRESS_UPDATE_INTERVAL = 100L
    }

    // ===================== 状态 =====================

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /** 当前播放位置（毫秒） */
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    /** 音频总时长（毫秒） */
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    // ===================== 内部变量 =====================

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    /** 内存数据播放时创建的临时文件，播放结束后清理 */
    private var tempFile: File? = null

    // ===================== 播放 =====================

    /**
     * 播放本地文件
     *
     * @param filePath 音频文件的绝对路径
     */
    fun play(filePath: String) {
        stop()

        try {
            val mp = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
            }
            startPlayback(mp)
        } catch (e: Exception) {
            Log.e(TAG, "播放文件失败: ${e.message}")
            resetState()
        }
    }

    /**
     * 播放内存中的音频数据
     *
     * 先写入临时文件，再用 MediaPlayer 播放。
     * 临时文件在 [stop] 或下次 [play] 时自动清理。
     *
     * @param data   音频原始字节（MP3 或 WAV 完整文件内容）
     * @param format 格式后缀，如 "mp3" / "wav"
     */
    fun play(data: ByteArray, format: String = "mp3") {
        stop()

        try {
            // 写入临时文件
            val file = AudioFileManager.createTempFile(format)
            FileOutputStream(file).use { fos ->
                fos.write(data)
            }
            tempFile = file

            val mp = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
            }
            startPlayback(mp)
        } catch (e: Exception) {
            Log.e(TAG, "播放内存数据失败: ${e.message}")
            resetState()
        }
    }

    /**
     * 启动 MediaPlayer 播放并注册回调
     */
    private fun startPlayback(mp: MediaPlayer) {
        mediaPlayer = mp
        _duration.value = mp.duration.toLong()
        _currentPosition.value = 0L

        mp.setOnCompletionListener {
            Log.d(TAG, "播放完成")
            resetState()
        }

        mp.setOnErrorListener { _, what, extra ->
            Log.e(TAG, "MediaPlayer 错误: what=$what, extra=$extra")
            resetState()
            true
        }

        mp.start()
        _isPlaying.value = true
        startProgressTimer()
    }

    // ===================== 控制 =====================

    /**
     * 暂停播放
     */
    fun pause() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                _isPlaying.value = false
                stopProgressTimer()
            }
        }
    }

    /**
     * 恢复播放（在暂停状态下调用）
     */
    fun resume() {
        mediaPlayer?.let { mp ->
            if (!mp.isPlaying) {
                mp.start()
                _isPlaying.value = true
                startProgressTimer()
            }
        }
    }

    /**
     * 跳转到指定位置
     *
     * @param positionMs 目标位置（毫秒）
     */
    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { mp ->
            mp.seekTo(positionMs.toInt())
            _currentPosition.value = positionMs
        }
    }

    /**
     * 停止播放并释放 MediaPlayer 资源
     */
    fun stop() {
        stopProgressTimer()
        mediaPlayer?.apply {
            try {
                if (isPlaying) stop()
            } catch (_: IllegalStateException) {
                // MediaPlayer 可能已处于非法状态，忽略
            }
            release()
        }
        mediaPlayer = null
        resetState()
        cleanupTempFile()
    }

    // ===================== 进度定时器 =====================

    private fun startProgressTimer() {
        stopProgressTimer()
        progressRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let { mp ->
                    try {
                        if (mp.isPlaying) {
                            _currentPosition.value = mp.currentPosition.toLong()
                        }
                    } catch (_: IllegalStateException) {
                        // 忽略
                    }
                }
                handler.postDelayed(this, PROGRESS_UPDATE_INTERVAL)
            }
        }
        handler.post(progressRunnable!!)
    }

    private fun stopProgressTimer() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = null
    }

    // ===================== 内部辅助 =====================

    private fun resetState() {
        _isPlaying.value = false
        _currentPosition.value = 0L
    }

    private fun cleanupTempFile() {
        tempFile?.let { file ->
            if (file.exists()) file.delete()
        }
        tempFile = null
    }

    /**
     * 释放所有资源（Activity/Fragment onDestroy 时调用）
     */
    fun release() {
        stop()
    }
}
