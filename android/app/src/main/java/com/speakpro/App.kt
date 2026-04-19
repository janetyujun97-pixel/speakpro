package com.speakpro

import android.app.Application
import com.speakpro.core.audio.AudioFileManager
import com.speakpro.core.network.ApiService
import com.speakpro.core.offline.OfflineUploadQueue
import com.speakpro.core.storage.TokenManager
import com.speakpro.data.models.BaselineRequest
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent

/**
 * SpeakPro 应用入口，启用 Hilt 依赖注入
 */
@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenManager.init(this)
        AudioFileManager.init(this)
        AudioFileManager.initPersistentCache(this)
        setupOfflineUploadQueue()
    }

    /**
     * PR5 follow-up —— 给 OfflineUploadQueue 注入默认 uploader。
     * 当前只处理 baseline 录音；后续可根据 task 上的元数据扩展到其他流程。
     */
    private fun setupOfflineUploadQueue() {
        val entry = EntryPointAccessors.fromApplication(this, AppQueueEntryPoint::class.java)
        val queue = entry.offlineQueue()
        val api = entry.apiService()
        queue.uploader = { task ->
            if (task.audioFilename.isEmpty()) {
                false
            } else {
                try {
                    val resp = api.postBaseline(
                        BaselineRequest(
                            sessionId = task.sessionId,
                            audioUrl = task.audioFilename,
                            transcript = null,
                        ),
                    )
                    resp.code == 0
                } catch (_: Exception) {
                    false
                }
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppQueueEntryPoint {
    fun offlineQueue(): OfflineUploadQueue
    fun apiService(): ApiService
}
