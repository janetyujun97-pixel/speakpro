package com.speakpro.features.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speakpro.core.network.ApiService
import com.speakpro.core.storage.NotebookCache
import com.speakpro.data.models.NotebookFilter
import com.speakpro.data.models.NotebookPhrase
import com.speakpro.data.models.NotebookWord
import com.speakpro.data.models.ReviewWordRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class NotebookViewModel @Inject constructor(
    private val apiService: ApiService,
    private val cache: NotebookCache,
) : ViewModel() {

    private val _filter = MutableStateFlow(NotebookFilter.DUE)
    val filter: StateFlow<NotebookFilter> = _filter.asStateFlow()

    /** 所有单词的缓存；UI 侧按 filter 过滤，避免每次切筛选都打一次 API */
    private val _allWords = MutableStateFlow<List<NotebookWord>>(emptyList())

    private val _words = MutableStateFlow<List<NotebookWord>>(emptyList())
    val words: StateFlow<List<NotebookWord>> = _words.asStateFlow()

    private val _phrases = MutableStateFlow<List<NotebookPhrase>>(emptyList())
    val phrases: StateFlow<List<NotebookPhrase>> = _phrases.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _reviewingWord = MutableStateFlow<NotebookWord?>(null)
    val reviewingWord: StateFlow<NotebookWord?> = _reviewingWord.asStateFlow()

    val dueCount: Int
        get() = _allWords.value.count {
            it.masteredAt == null && isDue(it.nextReviewAt)
        }

    fun loadAll() {
        viewModelScope.launch {
            // 本地缓存先上（弱网 / 离线首屏）
            val cachedWords = cache.loadWords()
            val cachedPhrases = cache.loadPhrases()
            val hadCache = cachedWords.isNotEmpty() || cachedPhrases.isNotEmpty()
            if (hadCache) {
                _allWords.value = cachedWords
                _phrases.value = cachedPhrases
                applyFilter()
            }

            _isLoading.value = true
            _errorMessage.value = null
            try {
                val wordsResp = apiService.getNotebookWords(NotebookFilter.ALL.value)
                val phrasesResp = apiService.getNotebookPhrases()
                val words = wordsResp.data ?: emptyList()
                val phrases = phrasesResp.data ?: emptyList()
                _allWords.value = words
                _phrases.value = phrases
                applyFilter()
                // 回写缓存
                cache.saveWords(words)
                cache.savePhrases(phrases)
            } catch (e: Exception) {
                // 联网失败 + 无缓存才报错
                if (!hadCache) _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setFilter(f: NotebookFilter) {
        _filter.value = f
        applyFilter()
    }

    fun startReview(word: NotebookWord) { _reviewingWord.value = word }
    fun dismissReview() { _reviewingWord.value = null }

    fun submitReview(word: NotebookWord, quality: Int) {
        viewModelScope.launch {
            try {
                val resp = apiService.reviewNotebookWord(word.id, ReviewWordRequest(quality))
                val updated = resp.data ?: return@launch
                _allWords.value = _allWords.value.map { if (it.id == updated.id) updated else it }
                applyFilter()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                dismissReview()
            }
        }
    }

    fun deleteWord(word: NotebookWord) {
        viewModelScope.launch {
            try {
                apiService.deleteNotebookWord(word.id)
                _allWords.value = _allWords.value.filter { it.id != word.id }
                applyFilter()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    private fun applyFilter() {
        val all = _allWords.value
        _words.value = when (_filter.value) {
            NotebookFilter.ALL -> all
            NotebookFilter.MASTERED -> all.filter { it.masteredAt != null }
                .sortedByDescending { it.masteredAt }
            NotebookFilter.DUE -> all
                .filter { it.masteredAt == null && isDue(it.nextReviewAt) }
                .sortedBy { it.nextReviewAt }
        }
    }

    private fun isDue(nextReviewAt: String?): Boolean {
        if (nextReviewAt == null) return true
        return try {
            Instant.parse(nextReviewAt).isBefore(Instant.now().plusSeconds(1))
        } catch (_: Exception) {
            true
        }
    }
}
