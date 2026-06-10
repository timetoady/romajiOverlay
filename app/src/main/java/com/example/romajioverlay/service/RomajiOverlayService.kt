package com.example.romajioverlay.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.romajioverlay.nlp.TokenizationManager
import com.example.romajioverlay.view.OverlayCanvasView
import com.example.romajioverlay.view.OverlayItem
import kotlinx.coroutines.*

class RomajiOverlayService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayCanvasView: OverlayCanvasView
    private lateinit var prefs: SharedPreferences

    // Coroutine Scope bound to service lifecycle
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Tracks current layout analysis job to support debouncing
    private var analysisJob: Job? = null

    // Regex to detect Japanese text (Hiragana, Katakana, Kanji)
    private val japaneseRegex = Regex("[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FAF]")

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "render_mode") {
            val newMode = sharedPreferences.getString("render_mode", "Furigana-Style") ?: "Furigana-Style"
            overlayCanvasView.renderMode = newMode
            triggerLayoutReprocess()
        }
    }

    // Temporary storage for node data extracted synchronously on the UI thread
    private data class PendingNode(val text: String, val bounds: Rect)

    override fun onCreate() {
        super.onCreate()
        
        // 1. Initialize tokenizer asynchronously in background
        TokenizationManager.initTokenizerAsync(serviceScope)

        // 2. Initialize SharedPreferences and configure overlay view
        prefs = getSharedPreferences("RomajiOverlayPrefs", Context.MODE_PRIVATE)
        overlayCanvasView = OverlayCanvasView(this)
        overlayCanvasView.renderMode = prefs.getString("render_mode", "Furigana-Style") ?: "Furigana-Style"

        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Create fullscreen translucent layout parameters for TYPE_ACCESSIBILITY_OVERLAY
        val layoutParams = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
        }

        // Add full-screen overlay view to window manager
        windowManager.addView(overlayCanvasView, layoutParams)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString()
        
        // Target app: Google Messages
        if (packageName != "com.google.android.apps.messaging") {
            // Cancel current analysis job and clear overlays immediately when switching apps
            analysisJob?.cancel()
            overlayCanvasView.updateItems(emptyList())
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                // Clear overlays immediately during scroll, and schedule reprocess after scroll finishes
                overlayCanvasView.updateItems(emptyList())
                triggerLayoutReprocess()
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                triggerLayoutReprocess()
            }
        }
    }

    override fun onInterrupt() {
        overlayCanvasView.updateItems(emptyList())
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // Cancel all active coroutines
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        
        // Remove overlay view from WindowManager safely
        if (::windowManager.isInitialized && ::overlayCanvasView.isInitialized) {
            try {
                windowManager.removeView(overlayCanvasView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Triggers layout reprocessing with debouncing.
     */
    private fun triggerLayoutReprocess() {
        analysisJob?.cancel() // Cancel preceding analysis job
        analysisJob = serviceScope.launch {
            delay(100) // Debounce layout changes by 100ms
            processActiveWindow()
        }
    }

    /**
     * Traverses the active window and maps target Japanese nodes to overlay items.
     */
    private suspend fun processActiveWindow() {
        val root = rootInActiveWindow ?: return
        val pendingNodes = mutableListOf<PendingNode>()

        // 1. Synchronously traverse tree and pull out layout data on main thread
        traverseNode(root, pendingNodes)
        root.recycle() // Recycle root node immediately

        if (pendingNodes.isEmpty()) {
            overlayCanvasView.updateItems(emptyList())
            return
        }

        // 2. Perform tokenization and translation off the main thread
        val overlayItems = mutableListOf<OverlayItem>()
        for (pending in pendingNodes) {
            // TokenizationManager handles threading context switches internally (uses Dispatchers.Default)
            val romaji = TokenizationManager.tokenizeAndTranslate(pending.text)
            if (romaji.isNotEmpty()) {
                overlayItems.add(OverlayItem(pending.bounds, pending.text, romaji))
            }
        }

        // 3. Update overlay items on main thread
        overlayCanvasView.updateItems(overlayItems)
    }

    /**
     * Recursively traverses nodes, captures texts containing Japanese,
     * and copies bounds. Recycles child nodes immediately to optimize memory.
     */
    private fun traverseNode(node: AccessibilityNodeInfo?, targetList: MutableList<PendingNode>) {
        if (node == null) return

        val text = node.text?.toString()
        if (!text.isNullOrEmpty() && japaneseRegex.containsMatchIn(text)) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            if (!bounds.isEmpty) {
                // Clone bounds coordinates immediately
                targetList.add(PendingNode(text, bounds))
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            traverseNode(child, targetList)
            child?.recycle() // Clean up child node reference
        }
    }
}
