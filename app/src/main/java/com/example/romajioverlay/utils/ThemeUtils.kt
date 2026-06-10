package com.example.romajioverlay.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color

data class ThemeColors(
    val textColor: Int,
    val bgColor: Int,
    val strokeColor: Int
)

object ThemeUtils {

    /**
     * Resolves the theme colors (text, background, and stroke) based on the system theme mode.
     *
     * Dark mode displays white text on a translucent dark gray background.
     * Light mode displays dark charcoal text on a translucent light gray background.
     */
    fun getThemeColors(context: Context): ThemeColors {
        val uiMode = context.resources.configuration.uiMode
        val isDarkMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        return if (isDarkMode) {
            ThemeColors(
                textColor = Color.WHITE,
                bgColor = Color.argb(215, 30, 30, 30),      // 84% dark gray
                strokeColor = Color.argb(60, 255, 255, 255)  // 24% white border
            )
        } else {
            ThemeColors(
                textColor = Color.argb(255, 18, 18, 18),    // Charcoal black
                bgColor = Color.argb(215, 240, 240, 240),    // 84% light gray
                strokeColor = Color.argb(60, 0, 0, 0)        // 24% black border
            )
        }
    }
}
