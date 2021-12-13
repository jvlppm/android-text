package com.jvlppm.text.extensions

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import java.util.regex.Pattern
import kotlin.math.roundToInt

private val colorHexPattern = Pattern.compile("#([\\da-f]{2})([\\da-f]{2})([\\da-f]{2})([\\da-f]{2})?", Pattern.CASE_INSENSITIVE).toRegex()
private val rgbPattern = Pattern.compile("rgb\\((\\d+),(\\d+),(\\d+)\\)").toRegex()
private val rgbaPattern = Pattern.compile("rgba\\((\\d+),(\\d+),(\\d+),([\\d.]+)\\)").toRegex()

fun String.decodeColor(context: Context?, preferRgba: Boolean = true): Int? {
    return decodeHexColor(preferRgba)
        ?: decodeRgbColor()
        ?: decodeRgbaColor()
        ?: context?.let { this.decodeAndroidColor(it) }
}

private val String.withoutSpaces: String get() = this.filter { !it.isWhitespace() }

private fun String.decodeHexColor(preferRgba: Boolean): Int? {
    val match = colorHexPattern.matchEntire(this) ?: return null
    val c1 = match.groupValues[1].toInt(radix = 16).coerceAtMost(255)
    val c2 = match.groupValues[2].toInt(radix = 16).coerceAtMost(255)
    val c3 = match.groupValues[3].toInt(radix = 16).coerceAtMost(255)
    val c4 = match.groupValues[4].valueOrNull()?.toInt(radix = 16)?.coerceAtMost(255) ?: 255

    return if (preferRgba)
        Color.argb(c4, c1, c2, c3)
    else
        Color.argb(c1, c2, c3, c4)
}

private fun String.decodeRgbColor(): Int? {
    val match = rgbPattern.matchEntire(this.withoutSpaces) ?: return null
    val r = match.groupValues[1].toInt(radix = 16).coerceAtMost(255)
    val g = match.groupValues[2].toInt(radix = 16).coerceAtMost(255)
    val b = match.groupValues[3].toInt(radix = 16).coerceAtMost(255)

    return Color.rgb(r, g, b)
}

private fun String.decodeRgbaColor(): Int? {
    val match = rgbaPattern.matchEntire(this.withoutSpaces) ?: return null
    val r = match.groupValues[1].toInt(radix = 16).coerceAtMost(255)
    val g = match.groupValues[2].toInt(radix = 16).coerceAtMost(255)
    val b = match.groupValues[3].toInt(radix = 16).coerceAtMost(255)
    val a = match.groupValues[4].toFloatOrNull()?.let { (it * 255).roundToInt() }?.coerceIn(0, 255) ?: return null
    return Color.argb(a, r, g, b)
}
