package com.speakpro.core.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.speakpro.data.models.NotebookPhrase
import com.speakpro.data.models.NotebookWord
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 错题本 / 生词本的本地镜像（PR5 follow-up）。
 *
 * 用 JSON 持久化代替 Room —— 跟 iOS 对齐：数据量有限、全量覆盖场景简单，
 * 不值得引入 Room schema 管理成本。
 *
 * 写入时机：网络 fetch 成功后由 NotebookViewModel 调 [saveWords] / [savePhrases]
 * 读取时机：首屏未拿到网络数据时 + 离线回退
 */
@Singleton
class NotebookCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val gson = Gson()

    private val dir: File by lazy {
        File(context.filesDir, "notebook").also { if (!it.exists()) it.mkdirs() }
    }
    private val wordsFile: File get() = File(dir, "words.json")
    private val phrasesFile: File get() = File(dir, "phrases.json")

    // ========== Save ==========

    fun saveWords(words: List<NotebookWord>) {
        writeJson(wordsFile, words)
    }

    fun savePhrases(phrases: List<NotebookPhrase>) {
        writeJson(phrasesFile, phrases)
    }

    // ========== Load ==========

    fun loadWords(): List<NotebookWord> {
        return readJson(wordsFile, object : TypeToken<List<NotebookWord>>() {}.type)
            ?: emptyList()
    }

    fun loadPhrases(): List<NotebookPhrase> {
        return readJson(phrasesFile, object : TypeToken<List<NotebookPhrase>>() {}.type)
            ?: emptyList()
    }

    val hasCachedWords: Boolean get() = wordsFile.exists()

    // ========== Clear ==========

    /** 用户登出时调 */
    fun clearAll() {
        wordsFile.delete()
        phrasesFile.delete()
    }

    // ========== Internal ==========

    private fun writeJson(file: File, value: Any) {
        try {
            file.writeText(gson.toJson(value))
        } catch (e: Exception) {
            android.util.Log.w("NotebookCache", "write failed: ${e.message}")
        }
    }

    private fun <T> readJson(file: File, type: java.lang.reflect.Type): T? {
        return try {
            if (!file.exists()) null
            else gson.fromJson(file.readText(), type)
        } catch (e: Exception) {
            android.util.Log.w("NotebookCache", "read failed: ${e.message}")
            null
        }
    }
}
