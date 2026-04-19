package com.speakpro.core.offline

import android.content.Context
import com.google.gson.Gson
import com.speakpro.core.network.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 离线上传任务 —— 录音 / 评测请求在断网时入队，联网后自动重试。
 *
 * 文件名存在 Documents（外部层面是 Android `filesDir`）；JSON 队列也写到同目录的
 * `offline_upload_queue.json`。Uploader 闭包由调用方注入（上传 + 回传评测）。
 */
data class OfflineUploadTask(
    val id: String = UUID.randomUUID().toString(),
    val audioFilename: String,
    val sessionId: String? = null,
    val referenceText: String? = null,
    val examType: String? = null,
    val section: String? = null,
    val createdAt: Long = Date().time,
    var retries: Int = 0,
    var status: Status = Status.PENDING,
) {
    enum class Status { PENDING, UPLOADING, FAILED }
}

/**
 * 单例队列；Hilt 注入。
 */
@Singleton
class OfflineUploadQueue @Inject constructor(
    @ApplicationContext private val context: Context,
    networkMonitor: NetworkMonitor,
) {
    private val gson = Gson()

    private val storage: File by lazy {
        File(context.filesDir, "offline_upload_queue.json")
    }

    private val _tasks = MutableStateFlow<List<OfflineUploadTask>>(emptyList())
    val tasks: StateFlow<List<OfflineUploadTask>> = _tasks.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    /** 上传器 —— 调用方在 App.onCreate 注入；未注入时 drain 为 no-op */
    var uploader: (suspend (OfflineUploadTask) -> Boolean)? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        load()
        // 监听联网：一旦 connected=true 且当前有 pending 任务，自动 drain
        scope.launch {
            networkMonitor.isConnected.collectLatest { connected ->
                if (connected && _tasks.value.any { it.status != OfflineUploadTask.Status.FAILED }) {
                    drain()
                }
            }
        }
    }

    // ========== Public ==========

    fun enqueue(task: OfflineUploadTask) {
        _tasks.value = _tasks.value + task
        persist()
    }

    fun remove(id: String) {
        _tasks.value = _tasks.value.filter { it.id != id }
        persist()
    }

    val pendingCount: Int
        get() = _tasks.value.count { it.status != OfflineUploadTask.Status.FAILED }

    fun pendingBytes(): Long =
        _tasks.value
            .filter { it.status != OfflineUploadTask.Status.FAILED }
            .sumOf { resolveFile(it.audioFilename)?.length() ?: 0L }

    fun resolveFile(filename: String): File? {
        val f = File(context.filesDir, filename)
        return if (f.exists()) f else null
    }

    suspend fun drain() {
        val up = uploader ?: return
        if (_isProcessing.value) return
        _isProcessing.value = true
        try {
            val snapshot = _tasks.value.toMutableList()
            for (i in snapshot.indices) {
                if (snapshot[i].status == OfflineUploadTask.Status.FAILED) continue
                snapshot[i] = snapshot[i].copy(status = OfflineUploadTask.Status.UPLOADING)
                _tasks.value = snapshot.toList()
                persist()

                val ok = runCatching { up(snapshot[i]) }.getOrDefault(false)
                if (ok) {
                    snapshot[i] = snapshot[i].copy(status = OfflineUploadTask.Status.PENDING) // placeholder
                    _tasks.value = snapshot.filter { it.id != snapshot[i].id }
                } else {
                    val t = snapshot[i]
                    val retries = t.retries + 1
                    snapshot[i] = t.copy(
                        retries = retries,
                        status = if (retries >= 5)
                            OfflineUploadTask.Status.FAILED
                        else OfflineUploadTask.Status.PENDING,
                    )
                    _tasks.value = snapshot.toList()
                }
                persist()
            }
        } finally {
            _isProcessing.value = false
        }
    }

    // ========== Persistence ==========

    private fun persist() {
        try {
            storage.writeText(gson.toJson(_tasks.value))
        } catch (e: Exception) {
            // 写失败不抛，日志即可
            android.util.Log.w("OfflineUploadQueue", "persist failed: ${e.message}")
        }
    }

    private fun load() {
        try {
            if (!storage.exists()) return
            val arr = gson.fromJson(storage.readText(), Array<OfflineUploadTask>::class.java)
            _tasks.value = arr.map {
                // 启动时把 UPLOADING 态回退（避免崩溃遗留）
                if (it.status == OfflineUploadTask.Status.UPLOADING)
                    it.copy(status = OfflineUploadTask.Status.PENDING)
                else it
            }
        } catch (e: Exception) {
            android.util.Log.w("OfflineUploadQueue", "load failed: ${e.message}")
            _tasks.value = emptyList()
        }
    }
}
