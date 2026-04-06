package com.speakpro.core.network

/**
 * API 端点路径常量
 *
 * 路径不包含 base URL 前缀，由 Retrofit 自动拼接。
 * 涉及 Go 服务的端点使用 [ApiClient.goRetrofit]，其余使用 [ApiClient.nestRetrofit]。
 */
object Endpoints {

    // ============================
    // NestJS CRUD 服务端点
    // ============================

    /** 认证相关 */
    object Auth {
        const val LOGIN = "auth/login"
        const val REGISTER = "auth/register"
        const val REFRESH = "auth/refresh"
    }

    /** 练习相关 */
    object Practice {
        const val START = "practice/start"
        const val AUDIO = "practice/audio"
        const val SESSIONS = "practice/sessions"
        const val STATS = "practice/stats"
    }

    /** 题库相关 */
    object Questions {
        const val LIST = "questions"
        const val CREATE = "questions"
        const val IMPORT = "questions/import"
    }

    /** 作业相关 */
    object Assignments {
        const val LIST = "assignments"

        fun detail(id: String) = "assignments/$id"
        fun submit(id: String) = "assignments/$id/submit"
    }

    // ============================
    // Go AI 服务端点
    // ============================

    /** 评测相关（Go 服务） */
    object Assessment {
        const val EVALUATE = "assessment/evaluate"
        const val FULL_EVALUATE = "assessment/full-evaluate"
        const val FEEDBACK = "assessment/feedback"
    }

    /** TTS 语音合成（Go 服务） */
    object Tts {
        const val SYNTHESIZE = "tts/synthesize"
    }

    /** WebSocket 对话（Go 服务） */
    object Conversation {

        /** NestJS 基地址（用于拼接 WS URL） */
        private const val WS_BASE_DEBUG = "ws://10.0.2.2:8081/api/v1"
        private const val WS_BASE_RELEASE = "wss://api.speakpro.com/api/v1"

        /**
         * 返回完整的 WebSocket 连接地址
         *
         * @param sessionId 会话 ID
         */
        fun wsConnect(sessionId: String): String {
            val base = if (BuildConfig.DEBUG) WS_BASE_DEBUG else WS_BASE_RELEASE
            return "$base/conversation/ws/$sessionId"
        }
    }
}
