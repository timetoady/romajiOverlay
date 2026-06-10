package com.example.romajioverlay.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
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
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (items.isEmpty()) return

        // 1. Resolve colors based on current system theme
        val themeColors = ThemeUtils.getThemeColors(context)
        bgPaint.color = themeColors.bgColor
        borderPaint.color = themeColors.strokeColor
        textPaint.color = themeColors.textColor

        // Dynamic padding sizes based on DP
        val cardPaddingX = dpToPx(6f)
        val cardPaddingY = dpToPx(4f)
        val cornerRadius = dpToPx(6f)
        borderPaint.strokeWidth = dpToPx(1f)

        // Select text size depending on style mode
        if (renderMode == "Furigana-Style") {
            textPaint.textSize = spToPx(11f)
        } else {
            textPaint.textSize = spToPx(14f)
        }

        val fontMetrics = textPaint.fontMetrics
        val textHeight = fontMetrics.bottom - fontMetrics.top
        val textAscentOffset = -fontMetrics.ascent

        for (item in items) {
            val bounds = item.bounds
            if (bounds.isEmpty) continue

            val textWidth = textPaint.measureText(item.romajiText)

            if (renderMode == "Furigana-Style") {
                // Calculate size of the Furigana tag
                val cardWidth = textWidth + (cardPaddingX * 2)
                val cardHeight = textHeight + (cardPaddingY * 2)

                // Try to place above bubble
                var cardTop = bounds.top - cardHeight - dpToPx(4f)
                
                // If it falls off the top of screen, place it below bubble instead
                if (cardTop < dpToPx(48f)) { // 48dp approximate status bar offset
                    cardTop = bounds.bottom.toFloat() + dpToPx(4f)
                }

                val cardLeft = bounds.centerX() - (cardWidth / 2)
                val cardRight = cardLeft + cardWidth
                val cardBottom = cardTop + cardHeight

                val rectF = RectF(cardLeft, cardTop, cardRight, cardBottom)

                // Draw translucent card background
                canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, bgPaint)
                // Draw card stroke
                canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint)

                // Draw translation text centered in card
                val textX = cardLeft + cardPaddingX
                val textY = cardTop + cardPaddingY + textAscentOffset
                canvas.drawText(item.romajiText, textX, textY, textPaint)

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

                // Draw centered Romaji text
                val textX = bounds.left + (bounds.width() - textWidth) / 2
                val textY = bounds.top + (bounds.height() - textHeight) / 2 + textAscentOffset
                canvas.drawText(item.romajiText, textX, textY, textPaint)
            }
        }
    }
}
