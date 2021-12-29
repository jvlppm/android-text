package com.jvlppm.text.extensions

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import java.util.regex.Pattern
import kotlin.math.roundToInt

private val colorShortHexPattern = Pattern.compile("#([\\da-f])([\\da-f])([\\da-f])", Pattern.CASE_INSENSITIVE).toRegex()
private val colorHexPattern = Pattern.compile("#([\\da-f]{2})([\\da-f]{2})([\\da-f]{2})([\\da-f]{2})?", Pattern.CASE_INSENSITIVE).toRegex()
private val rgbPattern = Pattern.compile("rgb\\((\\d+),(\\d+),(\\d+)\\)").toRegex()
private val rgbaPattern = Pattern.compile("rgba\\((\\d+),(\\d+),(\\d+),([\\d.]+)\\)").toRegex()

fun @receiver:ColorInt Int.toColorHex(preferRgba: Boolean = true): String {
    return if (preferRgba) {
        val alpha = Color.alpha(this)
        String.format("#%06X%02X", 0xFFFFFF and this, alpha)
    }
    else {
        String.format("#%08X", this)
    }
}

fun String.decodeColor(context: Context?, preferRgba: Boolean = true): Int? {
    return decodeHexColor(preferRgba)
        ?: decodeShortHexColor()
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

private fun String.decodeShortHexColor(): Int? {
    val match = colorShortHexPattern.matchEntire(this) ?: return null

    val d1 = match.groupValues[1]
    val d2 = match.groupValues[2]
    val d3 = match.groupValues[3]

    val red = "$d1$d1".toInt(radix = 16).coerceAtMost(255)
    val green = "$d2$d2".toInt(radix = 16).coerceAtMost(255)
    val blue = "$d3$d3".toInt(radix = 16).coerceAtMost(255)

    return Color.rgb(red, green, blue)
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
