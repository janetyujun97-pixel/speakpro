package com.speakpro

import android.app.Application
import com.speakpro.core.audio.AudioFileManager
import com.speakpro.core.storage.TokenManager
import dagger.hilt.android.HiltAndroidApp

/**
 * SpeakPro 应用入口，启用 Hilt 依赖注入
 */
@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenManager.init(this)
        AudioFileManager.init(this)
    }
}
