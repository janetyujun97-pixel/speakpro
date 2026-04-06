package com.speakpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.speakpro.designsystem.theme.SpeakProTheme
import com.speakpro.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint

/**
 * 单 Activity 架构入口，承载所有 Compose 导航
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpeakProTheme {
                AppNavigation()
            }
        }
    }
}
