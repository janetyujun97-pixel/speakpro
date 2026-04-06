package com.speakpro.core.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 录音管理器 — 基于 Android [AudioRecord] API
 *
 * 直接录制 16kHz / 16-bit / Mono PCM 数据（Android 硬件原生支持，无需重采样），
 * 支持波形可视化和流式推送到 WebSocket。
 *
 * 使用方式：
 * ```kotlin
 * val recorder = AudioRecorder(context)
 * recorder.onAudioBuffer = { pcmChunk -> webSocket.sendAudioChunk(pcmChunk) }
 * recorder.startRecording()
 * // ...
 * val wavFile = recorder.stopRecording()
 * ```
 */
class AudioRecorder(private val context: Context) {

    companion object {
        private const val TAG = "AudioRecorder"

        /** 讯飞要求：16kHz, 16-bit, Mono */
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        /** 波形可视化最大采样点数 */
        private const val MAX_WAVEFORM_SAMPLES = 200
    }

    // ===================== 状态 =====================

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _waveformData = MutableStateFlow<List<Float>>(emptyList())
    val waveformData: StateFlow<List<Float>> = _waveformData.asStateFlow()

    private val _permissionDenied = MutableStateFlow(false)
    val permissionDenied: StateFlow<Boolean> = _permissionDenied.asStateFlow()

    // ===================== 内部变量 =====================

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var scope: CoroutineScope? = null

    /** 收集所有 PCM 字节，停止时写入 WAV 文件 */
    private val pcmBuffer = mutableListOf<ByteArray>()

    /** 波形采样点缓冲 */
    private val waveformSamples = mutableListOf<Float>()

    /** 录音文件输出 */
    private var outputFile: File? = null

    /**
     * 流式音频数据回调
     *
     * 每次从 AudioRecord 读取到一帧 PCM 数据后调用，
     * 外部可用于向 WebSocket 发送 audio_chunk。
     */
    var onAudioBuffer: ((ByteArray) -> Unit)? = null

    // ===================== 权限检查 =====================

    /**
     * 检查是否已授予录音权限
     */
    fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

    // ===================== 开始录音 =====================

    /**
     * 开始录音
     *
     * @throws SecurityException 如果未授予麦克风权限
     * @throws IllegalStateException 如果 AudioRecord 初始化失败
     */
    fun startRecording() {
        if (_isRecording.value) {
            Log.w(TAG, "已经在录音中，忽略重复调用")
            return
        }

        // 权限检查
        if (!hasRecordPermission()) {
            _permissionDenied.value = true
            throw SecurityException("麦克风权限被拒绝，请在设置中开启")
        }

        // 计算最小缓冲区大小
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            throw IllegalStateException("AudioRecord 不支持当前音频参数")
        }

        // 使用 2 倍最小缓冲区，保证流畅读取
        val bufferSize = max(minBufferSize * 2, SAMPLE_RATE * 2) // 至少 1 秒的缓冲

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize,
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IllegalStateException("AudioRecord 初始化失败")
        }

        audioRecord = recorder
        pcmBuffer.clear()
        waveformSamples.clear()
        _waveformData.value = emptyList()
        outputFile = AudioFileManager.createTempFile("wav")

        recorder.startRecording()
        _isRecording.value = true

        // 在后台协程中持续读取音频数据
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        recordingJob = scope?.launch {
            readAudioLoop(recorder, minBufferSize)
        }

        Log.i(TAG, "录音已开始 (sampleRate=$SAMPLE_RATE, bufferSize=$bufferSize)")
    }

    // ===================== 音频读取循环 =====================

    private suspend fun readAudioLoop(recorder: AudioRecord, readSize: Int) {
        val buffer = ByteArray(readSize)

        while (_isRecording.value) {
            val bytesRead = recorder.read(buffer, 0, readSize)
            if (bytesRead > 0) {
                val chunk = buffer.copyOf(bytesRead)

                // 收集到总 PCM 缓冲
                pcmBuffer.add(chunk)

                // 计算波形振幅
                val amplitude = calculateAmplitude(chunk)
                synchronized(waveformSamples) {
                    waveformSamples.add(amplitude)
                    if (waveformSamples.size > MAX_WAVEFORM_SAMPLES) {
                        waveformSamples.removeAt(0)
                    }
                }

                // 更新 StateFlow（切到主线程安全）
                withContext(Dispatchers.Main) {
                    _waveformData.value = synchronized(waveformSamples) {
                        waveformSamples.toList()
                    }
                }

                // 流式回调
                onAudioBuffer?.invoke(chunk)
            }
        }
    }

    // ===================== 停止录音 =====================

    /**
     * 停止录音，将收集到的 PCM 数据写入 WAV 文件
     *
     * @return 录音 WAV 文件，如果录音数据为空则返回 null
     */
    fun stopRecording(): File? {
        if (!_isRecording.value) return null

        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null
        scope?.cancel()
        scope = null

        audioRecord?.apply {
            try {
                stop()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "停止录音异常: ${e.message}")
            }
            release()
        }
        audioRecord = null

        // 合并所有 PCM 数据
        val totalSize = pcmBuffer.sumOf { it.size }
        if (totalSize == 0) {
            Log.w(TAG, "录音数据为空")
            return null
        }

        val allPcmData = ByteArray(totalSize)
        var offset = 0
        for (chunk in pcmBuffer) {
            System.arraycopy(chunk, 0, allPcmData, offset, chunk.size)
            offset += chunk.size
        }
        pcmBuffer.clear()

        // 写入 WAV 文件
        val file = outputFile ?: return null
        AudioFileManager.writeWavFile(file, allPcmData, SAMPLE_RATE, 1, 16)

        Log.i(TAG, "录音已停止，文件大小: ${file.length()} 字节，路径: ${file.absolutePath}")
        return file
    }

    // ===================== 释放资源 =====================

    /**
     * 释放所有资源（Activity/Fragment onDestroy 时调用）
     */
    fun release() {
        if (_isRecording.value) {
            stopRecording()
        }
        onAudioBuffer = null
    }

    // ===================== 波形计算 =====================

    /**
     * 从 16-bit PCM 数据计算平均振幅（归一化到 0.0 ~ 1.0）
     */
    private fun calculateAmplitude(pcmData: ByteArray): Float {
        if (pcmData.size < 2) return 0f

        val sampleCount = pcmData.size / 2
        var sum = 0L

        for (i in 0 until sampleCount) {
            // 16-bit little-endian PCM
            val low = pcmData[i * 2].toInt() and 0xFF
            val high = pcmData[i * 2 + 1].toInt()
            val sample = (high shl 8) or low
            sum += abs(sample).toLong()
        }

        val avg = sum.toFloat() / max(sampleCount, 1)
        // 归一化到 0.0 ~ 1.0
        return min(avg / 32768f, 1f)
    }
}
