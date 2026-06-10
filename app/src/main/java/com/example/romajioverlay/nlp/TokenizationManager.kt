package com.example.romajioverlay.nlp

import android.util.LruCache
import com.atilika.kuromoji.ipadic.Tokenizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

object TokenizationManager {

    private var tokenizer: Tokenizer? = null

    @Volatile
    var isReady = false
        private set

    // Thread-safe translation cache
    private val translationCache = LruCache<String, String>(256)

    /**
     * Initializes the Kuromoji tokenizer asynchronously on a background thread.
     */
    fun initTokenizerAsync(scope: CoroutineScope): Deferred<Boolean> {
        return scope.async(Dispatchers.IO) {
            if (tokenizer != null) return@async true
            try {
                // Instantiating the builder loads the IPADIC dictionary (expensive operation)
                tokenizer = Tokenizer.Builder().build()
                isReady = true
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Translates Japanese text to Romaji, utilizing the LruCache on cache hit,
     * or executing the tokenization and translation on Dispatchers.Default.
     */
    suspend fun tokenizeAndTranslate(text: String): String = withContext(Dispatchers.Default) {
        if (text.trim().isEmpty()) return@withContext ""

        // Check cache (LruCache is thread-safe)
        synchronized(translationCache) {
            val cachedValue = translationCache.get(text)
            if (cachedValue != null) {
                return@withContext cachedValue
            }
        }

        val currentTokenizer = tokenizer
        if (currentTokenizer == null || !isReady) {
            // Tokenizer not ready yet, return fallback or empty
            return@withContext ""
        }

        try {
            // Run tokenizer
            val tokens = currentTokenizer.tokenize(text)
            val romajiText = RomajiTranslator.translateTokens(tokens)

            // Store in cache
            synchronized(translationCache) {
                translationCache.put(text, romajiText)
            }

            romajiText
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Clears the translation cache.
     */
    fun clearCache() {
        synchronized(translationCache) {
            translationCache.evictAll()
        }
    }
}
