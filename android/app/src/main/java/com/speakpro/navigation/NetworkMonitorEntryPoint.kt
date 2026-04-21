package com.speakpro.navigation

import com.speakpro.core.network.NetworkMonitor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** 给 Composable 拿 NetworkMonitor 的快捷通道（不想为一个单例写 @HiltViewModel）。 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NetworkMonitorEntryPoint {
    fun networkMonitor(): NetworkMonitor
}
