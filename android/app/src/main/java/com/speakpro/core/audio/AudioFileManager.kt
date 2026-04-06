package com.speakpro.core.audio

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 本地音频文件管理器
 *
 * 负责在 app cacheDir 下管理临时录音 / TTS 缓存文件，
 * 提供 WAV 文件创建、清理过期文件等能力。
 *
 * 必须在 Application.onCreate 中调用 [init] 完成初始化。
 */
object AudioFileManager {

    private const val DIR_NAME = "speakpro_audio"

    private lateinit var cacheDir: File

    /**
     * 初始化（需在 Application.onCreate 中调用）
     */
    fun init(context: Context) {
        cacheDir = File(context.cacheDir, DIR_NAME).also { dir ->
            if (!dir.exists()) dir.mkdirs()
        }
    }

    /** 录音 / TTS 临时目录 */
    val directory: File
        get() = cacheDir

    // ========================================================
    // 文件创建
    // ========================================================

    /**
     * 在临时目录下创建一个唯一文件名的临时文件
     *
     * @param extension 文件后缀（不含点），如 "wav"、"mp3"
     * @return 创建好的空 [File]
     */
    fun createTempFile(extension: String): File {
        val filename = "${UUID.randomUUID()}.$extension"
        return File(cacheDir, filename).also { file ->
            if (!file.exists()) file.createNewFile()
        }
    }

    /**
     * 将 PCM 裸数据写成带标准 44 字节 RIFF 头的 WAV 文件
     *
     * @param pcmData     16-bit signed little-endian PCM 数据
     * @param sampleRate  采样率（默认 16000）
     * @param channels    声道数（默认 1 = mono）
     * @param bitsPerSample 位深度（默认 16）
     * @return 写好的 WAV 文件
     */
    fun createWavFile(
        pcmData: ByteArray,
        sampleRate: Int = 16000,
        channels: Int = 1,
        bitsPerSample: Int = 16,
    ): File {
        val wavFile = createTempFile("wav")
        writeWavFile(wavFile, pcmData, sampleRate, channels, bitsPerSample)
        return wavFile
    }

    // ========================================================
    // 文件信息
    // ========================================================

    /**
     * 获取文件大小（字节）
     */
    fun getFileSize(file: File): Long = if (file.exists()) file.length() else 0L

    // ========================================================
    // 清理
    // ========================================================

    /**
     * 删除指定文件
     */
    fun deleteFile(file: File) {
        if (file.exists()) file.delete()
    }

    /**
     * 清理超过指定天数的旧文件
     *
     * @param maxAgeDays 最大保留天数，默认 7 天
     */
    fun cleanOldFiles(maxAgeDays: Int = 7) {
        if (!::cacheDir.isInitialized) return
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(maxAgeDays.toLong())
        cacheDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }

    /**
     * 清空整个临时目录
     */
    fun clearAll() {
        if (!::cacheDir.isInitialized) return
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    // ========================================================
    // WAV 文件写入
    // ========================================================

    /**
     * 写入 WAV 文件（44 字节标准 RIFF header + PCM data）
     */
    internal fun writeWavFile(
        file: File,
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
    ) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcmData.size
        val fileSize = 36 + dataSize // RIFF chunk size = file size - 8

        FileOutputStream(file).use { fos ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

            // RIFF header
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(fileSize)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))

            // fmt sub-chunk
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16)                     // sub-chunk 1 size (PCM)
            header.putShort(1)                    // audio format = PCM
            header.putShort(channels.toShort())
            header.putInt(sampleRate)
            header.putInt(byteRate)
            header.putShort(blockAlign.toShort())
            header.putShort(bitsPerSample.toShort())

            // data sub-chunk
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(dataSize)

            fos.write(header.array())
            fos.write(pcmData)
        }
    }

    /**
     * 在已有 WAV 文件末尾追加 PCM 数据，并更新 header 中的大小字段。
     * 用于边录边写的场景。
     */
    internal fun appendPcmToWavFile(file: File, pcmChunk: ByteArray) {
        if (!file.exists() || pcmChunk.isEmpty()) return

        // 追加 PCM 数据
        FileOutputStream(file, true).use { fos ->
            fos.write(pcmChunk)
        }

        // 更新 header 中的 RIFF chunk size 和 data chunk size
        RandomAccessFile(file, "rw").use { raf ->
            val totalLength = raf.length()
            val riffSize = (totalLength - 8).toInt()
            val dataSize = (totalLength - 44).toInt()

            // RIFF chunk size at offset 4
            raf.seek(4)
            raf.write(intToLittleEndianBytes(riffSize))

            // data chunk size at offset 40
            raf.seek(40)
            raf.write(intToLittleEndianBytes(dataSize))
        }
    }

    private fun intToLittleEndianBytes(value: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
}
