package com.speakpro.core.network

import com.speakpro.data.models.ApiResponse
import com.speakpro.data.models.AppleSignInRequest
import com.speakpro.data.models.AuthTokenResponse
import com.speakpro.data.models.BaselineRequest
import com.speakpro.data.models.BaselineResponse
import com.speakpro.data.models.HomeworkAssignment
import com.speakpro.data.models.LoginRequest
import com.speakpro.data.models.LoginResponse
import com.speakpro.data.models.OnboardingProfile
import com.speakpro.data.models.OnboardingStatusResponse
import com.speakpro.data.models.PracticeStats
import com.speakpro.data.models.RegisterPhoneRequest
import com.speakpro.data.models.RequestResetRequest
import com.speakpro.data.models.ResetPasswordRequest
import com.speakpro.data.models.ResetPasswordResponse
import com.speakpro.data.models.SendOtpRequest
import com.speakpro.data.models.SendOtpResponse
import com.speakpro.data.models.UpdateProfileRequest
import com.speakpro.data.models.VerifyOtpRequest
import com.speakpro.data.models.VerifyOtpResponse
import com.speakpro.data.models.WechatSignInRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit API 接口 — 对应 NestJS 后端
 */
interface ApiService {

    // ── Auth v1（既有邮箱密码） ──

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<LoginResponse>

    // ── Auth v2（PR2a 新增） ──

    @POST("auth/send-otp")
    suspend fun sendOtp(@Body body: SendOtpRequest): ApiResponse<SendOtpResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): ApiResponse<VerifyOtpResponse>

    @POST("auth/register-phone")
    suspend fun registerWithPhone(@Body body: RegisterPhoneRequest): ApiResponse<AuthTokenResponse>

    @POST("auth/request-reset")
    suspend fun requestReset(@Body body: RequestResetRequest): ApiResponse<SendOtpResponse>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): ApiResponse<ResetPasswordResponse>

    @POST("auth/apple")
    suspend fun signInWithApple(@Body body: AppleSignInRequest): ApiResponse<AuthTokenResponse>

    @POST("auth/wechat")
    suspend fun signInWithWechat(@Body body: WechatSignInRequest): ApiResponse<AuthTokenResponse>

    // ── Onboarding ──

    @GET("onboarding/status")
    suspend fun getOnboardingStatus(): ApiResponse<OnboardingStatusResponse>

    @PATCH("onboarding/profile")
    suspend fun patchOnboardingProfile(
        @Body body: UpdateProfileRequest,
    ): ApiResponse<OnboardingProfile>

    @POST("onboarding/baseline")
    suspend fun postBaseline(@Body body: BaselineRequest): ApiResponse<BaselineResponse>

    @POST("onboarding/finalize")
    suspend fun finalizeOnboarding(
        @Body body: Map<String, String> = emptyMap(),
    ): ApiResponse<OnboardingStatusResponse>

    // ── Practice ──

    @GET("practice/stats")
    suspend fun getPracticeStats(): ApiResponse<PracticeStats>

    // ── Assignments (Homework) ──

    @GET("assignments")
    suspend fun getAssignments(
        @Query("status") status: String? = null,
    ): ApiResponse<List<HomeworkAssignment>>
}
