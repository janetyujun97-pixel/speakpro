package com.speakpro.core.network

import com.speakpro.core.storage.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp 拦截器，自动在请求头注入 Bearer Token
 */
class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // 登录接口无需 Token
        if (original.url.encodedPath.contains("auth/login")) {
            return chain.proceed(original)
        }

        val token = TokenManager.accessToken
        if (token.isNullOrBlank()) {
            return chain.proceed(original)
        }

        val request = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(request)
    }
}
