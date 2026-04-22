package com.speakpro.core.network

import com.google.gson.GsonBuilder
import com.speakpro.core.storage.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// ApiResponse 定义在 com.speakpro.data.models.ApiModels.kt

/**
 * Retrofit 网络客户端单例
 *
 * 提供两个 Retrofit 实例：
 * - [nestRetrofit] → NestJS CRUD 服务 (端口 3000)
 * - [goRetrofit]   → Go AI 服务 (端口 8081)
 *
 * 自动在请求头中附加 JWT Bearer Token。
 */
object ApiClient {

    // ========== Base URLs ==========

    /** NestJS 服务基地址（FRP 内网穿透） */
    private const val NEST_BASE_URL = "http://frp6.ccszxc.site:26074/api/v1/"

    /** Go AI 服务基地址（FRP 内网穿透） */
    private const val GO_BASE_URL = "http://frp6.ccszxc.site:26074/api/v1/"

    // ========== 超时配置 ==========

    /** 默认超时 30 秒 */
    private const val DEFAULT_TIMEOUT_SEC = 30L

    /** full-evaluate 等 AI 密集型接口超时 120 秒 */
    private const val LONG_TIMEOUT_SEC = 120L

    // ========== Gson ==========

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    // ========== OkHttp 拦截器 ==========

    /** JWT 认证拦截器 */
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()

        // 自动附加 Authorization header
        TokenManager.accessToken?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        requestBuilder.addHeader("Content-Type", "application/json")
        chain.proceed(requestBuilder.build())
    }

    /** 超时动态调整拦截器：对 full-evaluate 使用更长超时 */
    private val timeoutInterceptor = Interceptor { chain ->
        val request = chain.request()
        val url = request.url.toString()

        if (url.contains("full-evaluate")) {
            chain.withConnectTimeout(LONG_TIMEOUT_SEC.toInt(), TimeUnit.SECONDS)
                .withReadTimeout(LONG_TIMEOUT_SEC.toInt(), TimeUnit.SECONDS)
                .withWriteTimeout(LONG_TIMEOUT_SEC.toInt(), TimeUnit.SECONDS)
                .proceed(request)
        } else {
            chain.proceed(request)
        }
    }

    /** 日志拦截器（仅 Debug 构建启用） */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    // ========== OkHttpClient ==========

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(timeoutInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    // ========== Retrofit 实例 ==========

    /** NestJS CRUD 服务 Retrofit 实例 */
    val nestRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(NEST_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /** Go AI 服务 Retrofit 实例 */
    val goRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(GO_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    // ========== 便捷方法 ==========

    /**
     * 创建 NestJS 服务的 API 接口实例
     */
    inline fun <reified T> createNestService(): T {
        return nestRetrofit.create(T::class.java)
    }

    /**
     * 创建 Go AI 服务的 API 接口实例
     */
    inline fun <reified T> createGoService(): T {
        return goRetrofit.create(T::class.java)
    }
}

/**
 * BuildConfig 占位（实际由 Gradle 生成）
 * 在正式项目中删除此对象，改用 gradle 生成的 BuildConfig
 */
internal object BuildConfig {
    const val DEBUG = true
}
