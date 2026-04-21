package com.speakpro.navigation

import com.speakpro.core.network.ApiService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 给无 @HiltViewModel 的 Composable 一个获取 ApiService 的快捷通道
 * （例如 Onboarding gate 只需要发一次请求，不值得建独立 VM）。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppApiEntryPoint {
    fun apiService(): ApiService
}
