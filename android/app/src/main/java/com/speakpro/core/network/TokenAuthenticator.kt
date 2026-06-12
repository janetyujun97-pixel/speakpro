package com.speakpro.core.network

import com.speakpro.core.storage.TokenManager
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OkHttp Authenticator：access token 过期(401)时自动用 refresh token 换新并重试。
 *
 * - 单飞：synchronized 保证并发 401 只触发一次刷新，其余请求直接复用新 token
 * - 服务端 /auth/refresh 每次轮换出新一对 token（滑动续期），
 *   只要 refresh token 有效期内用过 App 就保持登录
 * - refresh token 也失效（180 天未使用 / 账号被禁用）→ 清除本地 token，
 *   请求按 401 返回，由 UI 引导重新登录
 */
object TokenAuthenticator : Authenticator {

    private const val REFRESH_URL = "https://learnpark.cn:8443/api/v1/auth/refresh"

    /** 刷新专用裸客户端：不挂任何拦截器，避免递归触发认证 */
    private val refreshClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // 刷新接口自身返回 401（refresh token 失效）→ 放弃
        if (response.request.url.encodedPath.endsWith("/auth/refresh")) return null

        // 同一请求只重试一次，避免死循环
        if (response.priorResponse != null) return null

        val failedAuthHeader = response.request.header("Authorization")

        synchronized(this) {
            val current = TokenManager.accessToken
            // 别的线程已刷新过（当前 token 和失败请求用的不一样）→ 直接用新 token 重试
            if (!current.isNullOrBlank() && "Bearer $current" != failedAuthHeader) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $current")
                    .build()
            }

            val refresh = TokenManager.refreshToken
            if (refresh.isNullOrBlank()) return null

            val newAccess = requestNewTokens(refresh) ?: run {
                // refresh 失效：清掉 token，让上层感知未登录
                TokenManager.clearTokens()
                return null
            }

            return response.request.newBuilder()
                .header("Authorization", "Bearer $newAccess")
                .build()
        }
    }

    /** 同步调用 /auth/refresh，成功则保存新一对 token 并返回新 accessToken */
    private fun requestNewTokens(refreshToken: String): String? {
        return try {
            val body = JSONObject().put("refreshToken", refreshToken).toString()
                .toRequestBody("application/json".toMediaType())
            val req = Request.Builder().url(REFRESH_URL).post(body).build()

            refreshClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val json = JSONObject(resp.body?.string() ?: return null)
                if (json.optInt("code", -1) != 0) return null
                val data = json.optJSONObject("data") ?: return null
                val access = data.optString("accessToken").takeIf { it.isNotBlank() } ?: return null
                val newRefresh = data.optString("refreshToken").takeIf { it.isNotBlank() } ?: refreshToken
                TokenManager.saveTokens(access, newRefresh)
                access
            }
        } catch (_: Exception) {
            null // 网络异常等：不清 token，下次再试
        }
    }
}
