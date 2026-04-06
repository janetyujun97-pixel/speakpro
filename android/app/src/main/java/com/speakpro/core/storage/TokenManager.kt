package com.speakpro.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Token 管理器
 *
 * 使用 [EncryptedSharedPreferences] 安全存储 JWT Token，
 * 使用普通 [SharedPreferences] 存储用户基本信息。
 *
 * 必须在 Application.onCreate 中调用 [init] 完成初始化。
 */
object TokenManager {

    private const val ENCRYPTED_PREFS_NAME = "speakpro_secure_prefs"
    private const val USER_PREFS_NAME = "speakpro_user_prefs"

    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"

    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_ROLE = "user_role"

    private lateinit var encryptedPrefs: SharedPreferences
    private lateinit var userPrefs: SharedPreferences

    /**
     * 初始化（需在 Application.onCreate 中调用）
     */
    fun init(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

        userPrefs = context.getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ========== Token 操作 ==========

    /** 访问令牌 */
    var accessToken: String?
        get() = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = encryptedPrefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    /** 刷新令牌 */
    var refreshToken: String?
        get() = encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) = encryptedPrefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()

    /**
     * 保存登录后的双 Token
     */
    fun saveTokens(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    /**
     * 清除所有 Token
     */
    fun clearTokens() {
        encryptedPrefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }

    /** 是否已登录（持有有效 accessToken） */
    val isLoggedIn: Boolean
        get() = !accessToken.isNullOrBlank()

    // ========== 用户信息 ==========

    /** 用户 ID */
    var userId: String?
        get() = userPrefs.getString(KEY_USER_ID, null)
        set(value) = userPrefs.edit().putString(KEY_USER_ID, value).apply()

    /** 用户名 */
    var userName: String?
        get() = userPrefs.getString(KEY_USER_NAME, null)
        set(value) = userPrefs.edit().putString(KEY_USER_NAME, value).apply()

    /** 用户邮箱 */
    var userEmail: String?
        get() = userPrefs.getString(KEY_USER_EMAIL, null)
        set(value) = userPrefs.edit().putString(KEY_USER_EMAIL, value).apply()

    /** 用户角色 (student / teacher) */
    var userRole: String?
        get() = userPrefs.getString(KEY_USER_ROLE, null)
        set(value) = userPrefs.edit().putString(KEY_USER_ROLE, value).apply()

    /**
     * 保存用户基本信息
     */
    fun saveUserInfo(id: String, name: String, email: String, role: String) {
        userPrefs.edit()
            .putString(KEY_USER_ID, id)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_ROLE, role)
            .apply()
    }

    /**
     * 清除所有数据（退出登录时调用）
     */
    fun clearAll() {
        clearTokens()
        userPrefs.edit().clear().apply()
    }
}
