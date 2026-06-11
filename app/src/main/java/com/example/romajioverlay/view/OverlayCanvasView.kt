package com.example.romajioverlay.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.util.TypedValue
import android.view.View
import com.example.romajioverlay.utils.ThemeUtils

data class OverlayItem(
    val bounds: Rect,
    val nativeText: String,
    val romajiText: String
)

class OverlayCanvasView(context: Context) : View(context) {

    var renderMode: String = "Furigana-Style"
        set(value) {
            field = value
            invalidate()
        }

    private var items: List<OverlayItem> = emptyList()

    // Paints
    private val textPaint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    // Convert dp to px helpers
    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }

    private fun spToPx(sp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            resources.displayMetrics
        )
    }

    /**
     * Updates the active translation overlay items and triggers a redraw.
     */
    fun updateItems(newItems: List<OverlayItem>) {
        items = newItems
        postInvalidate()
    }

    /**
     * Helper to instantiate a StaticLayout to handle multi-line wrapped text drawing.
     */
    private fun createStaticLayout(text: String, width: Int): StaticLayout {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, textPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1.0f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                text, textPaint, width,
                Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isAttachedToWindow || items.isEmpty()) return

        // Retrieve the offset of this view relative to the absolute screen window
        // to calibrate getBoundsInScreen coordinates correctly (e.g. accounting for status bar)
        val location = IntArray(2)
        getLocationOnScreen(location)
        val viewOffsetX = location[0]
        val viewOffsetY = location[1]

        // 1. Resolve colors based on current system theme
        val themeColors = ThemeUtils.getThemeColors(context)
        bgPaint.color = themeColors.bgColor
        borderPaint.color = themeColors.strokeColor
        textPaint.color = themeColors.textColor

        // Dynamic padding sizes based on DP
        val cardPaddingX = dpToPx(8f)
        val cardPaddingY = dpToPx(4f)
        val cornerRadius = dpToPx(6f)
        borderPaint.strokeWidth = dpToPx(1f)

        // Select text size depending on style mode
        if (renderMode == "Furigana-Style") {
            textPaint.textSize = spToPx(11f)
        } else {
            textPaint.textSize = spToPx(14f)
        }

        for (item in items) {
            try {
                if (item.romajiText.trim().isEmpty()) continue

                // Align the bounding box with the local canvas system
                val bounds = Rect(item.bounds)
                bounds.offset(-viewOffsetX, -viewOffsetY)

                if (bounds.isEmpty || bounds.width() <= 0) continue

                // Compute layout width from the bubble bounds, with padding inset
                val layoutWidth = (bounds.width() - cardPaddingX * 2).toInt()
                if (layoutWidth <= 0) continue

                val staticLayout = createStaticLayout(item.romajiText, layoutWidth)

                if (renderMode == "Furigana-Style") {
                    // Furigana-Style: Draw a compact card aligned to the bubble's width,
                    // positioned directly above it (or below if no room above).
                    val cardHeight = staticLayout.height + (cardPaddingY * 2)

                    // Try to place above bubble
                    var cardTop = bounds.top - cardHeight - dpToPx(2f)

                    // If it falls off the top of screen, place it below bubble instead
                    if (cardTop < 0f) {
                        cardTop = bounds.bottom.toFloat() + dpToPx(2f)
                    }

                    val rectF = RectF(
                        bounds.left.toFloat(),
                        cardTop,
                        bounds.right.toFloat(),
                        cardTop + cardHeight
                    )

                    // Draw translucent card background
                    canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, bgPaint)
                    // Draw card stroke
                    canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint)

                    // Draw translation text using StaticLayout
                    canvas.save()
                    canvas.translate(bounds.left + cardPaddingX, cardTop + cardPaddingY)
                    staticLayout.draw(canvas)
                    canvas.restore()

                } else {
                    // Overlay-Style: Cover the native text completely
                    val rectF = RectF(
                        bounds.left.toFloat(),
                        bounds.top.toFloat(),
                        bounds.right.toFloat(),
                        bounds.bottom.toFloat()
                    )

                    // Draw translucent background covering the bubble
                    canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, bgPaint)
                    // Draw stroke
                    canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint)

                    // Draw centered Romaji text (wrapping lines inside bubble bounds)
                    val textX = bounds.left + cardPaddingX
                    val textY = bounds.top + (bounds.height() - staticLayout.height) / 2f

                    canvas.save()
                    canvas.translate(textX, textY)
                    staticLayout.draw(canvas)
                    canvas.restore()
                }
            } catch (e: Exception) {
                // Protect the accessibility service from being killed by draw errors
                e.printStackTrace()
            }
        }
    }
}
