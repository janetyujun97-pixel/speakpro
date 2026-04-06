package com.speakpro.core.network

import com.speakpro.data.models.ApiResponse
import com.speakpro.data.models.HomeworkAssignment
import com.speakpro.data.models.LoginRequest
import com.speakpro.data.models.LoginResponse
import com.speakpro.data.models.PracticeStats
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit API 接口 — 对应 NestJS 后端
 */
interface ApiService {

    // ── Auth ──────────────────────────────────────

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<LoginResponse>

    // ── Practice ──────────────────────────────────

    @GET("practice/stats")
    suspend fun getPracticeStats(): ApiResponse<PracticeStats>

    // ── Assignments (Homework) ────────────────────

    @GET("assignments")
    suspend fun getAssignments(
        @Query("status") status: String? = null,
    ): ApiResponse<List<HomeworkAssignment>>
}
