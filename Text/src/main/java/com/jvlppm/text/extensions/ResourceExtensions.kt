package com.jvlppm.text.extensions

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import java.util.regex.Pattern

private val androidResourcePattern = Pattern.compile("\\??(([^:]*):)?(([^/]*?)/)?(.*)").toRegex()

internal data class AndroidResource(
    val id: Int,
    val isAttr: Boolean,
)

internal fun String.decodeAndroidColor(context: Context): Int? =
    context.decodeReference(this, "color")?.let { reference ->
        try {
            if (reference.isAttr) {
                context.getValueFromAttr(reference.id)?.let {
                    ContextCompat.getColor(context, it)
                }
            }
            else {
                context.resources.getColor(reference.id, context.theme)
            }
        } catch (t: Throwable) {
            null
        }
    }


internal fun Context.decodeResourceId(value: String, defType: String): Int?
    = decodeReference(value, defType)?.let { reference ->
        try {
            if (reference.isAttr) {
                getValueFromAttr(reference.id)
            }
            else {
                reference.id
            }
        } catch (t: Throwable) {
            null
        }
    }


internal fun Context.decodeReference(value: String, defType: String): AndroidResource? {
    val match = androidResourcePattern.matchEntire(value) ?: return null

    val isAttr = value.startsWith("?")
    val packageName = match.groupValues[2].valueOrNull()
    val defType = match.groupValues[4].valueOrNull() ?: if (isAttr) "attr" else defType
    val valueStr = match.groupValues[5]

    val id = valueStr.toIntOrNull() ?: getIdentifier(this, packageName, defType, valueStr)
    return id?.let {
        AndroidResource(it, isAttr)
    }
}
private fun getIdentifier(context: Context, packageName: String?, defType: String, valueStr: String): Int? {
    val possiblePackages = packageName?.let { listOf(it) } ?: listOf(context.packageName, "android")

    return possiblePackages.firstNotNullOfOrNull { searchPackageName ->
        context.resources.getIdentifier(valueStr, defType, searchPackageName)
            .takeIf { it != 0 }
    }
}
internal fun Context.getValueFromAttr(@AttrRes colorAttr: Int): Int? {
    val resolvedAttr = resolveThemeAttr(colorAttr)
    return resolvedAttr?.let {
        // resourceId is used if it's a ColorStateList, and data if it's a color reference or a hex color
        if (resolvedAttr.resourceId != 0) resolvedAttr.resourceId else resolvedAttr.data
    }
}

private fun Context.resolveThemeAttr(@AttrRes attrRes: Int): TypedValue? {
    val typedValue = TypedValue()
    val didLoad = theme.resolveAttribute(attrRes, typedValue, true)
    return typedValue.takeIf { didLoad }
}