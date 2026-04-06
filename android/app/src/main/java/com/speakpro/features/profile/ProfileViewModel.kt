package com.speakpro.features.profile

import androidx.lifecycle.ViewModel
import com.speakpro.core.storage.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * 个人中心视图模型
 */
@HiltViewModel
class ProfileViewModel @Inject constructor() : ViewModel() {

    private val _userName = MutableStateFlow(TokenManager.userName ?: "同学")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow(TokenManager.userEmail ?: "")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userRole = MutableStateFlow(TokenManager.userRole ?: "student")
    val userRole: StateFlow<String> = _userRole.asStateFlow()
}
