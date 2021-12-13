package com.jvlppm.text

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import java.util.regex.Pattern


abstract class Text {
    companion object {
        operator fun invoke(text: String, style: Any? = null): Text =
            Static(text, style = style)

        operator fun invoke(@StringRes text: Int, style: Any? = null): Text =
            FromResource(text, style = style)

        fun spaced(vararg parts: Text?): Text = Separated(space, null, *parts)
        fun concatenate(vararg parts: Text?): Text = Separated(null, null, *parts)
        fun joining(separator: Text?, vararg parts: Text?): Text = Separated(separator, null, *parts)
        fun lines(vararg parts: Text?): Text = Separated(null, style = null, *parts.take(parts.size - 1).map {
            it?.replaceAll(Pattern.compile("$") to { _, _ -> lineBreak })
        }.toTypedArray(), parts.lastOrNull())

        val empty = Text("")
        val lineBreak = Text("\n")
        val space = Text(" ")
    }

    operator fun plus(text: Text): Text {
        return concatenate(this, text)
    }

    fun quoted(): Text {
        return this.replaceAll(Pattern.compile(".+") to { match, _ -> Text("\"${match.value}\"") })
    }

    fun terminated(): Text {
        return this.replaceAll(Pattern.compile("(?<=\\w)(?<=[^\\.,!\\?])\\s*\$", Pattern.MULTILINE) to { _, _ -> Text(".") })
    }

    fun withStyle(style: Any?): Text {
        return when {
            style == null -> this
            this is Separated && this.style == null -> Separated(separator, style, *parts)
            else -> Separated(null, style, this)
        }
    }

    fun styleMatches(pattern: Pattern, getStyle: (text: String) -> Any?): Text {
        return TextWrapper.StyleOverlay(this) { _, text ->
            val matches = pattern.toRegex().findAll(text)
            matches.mapNotNull { match ->
                getStyle(match.value)?.let { format ->
                    RangeStyle(match.range, format)
                }
            }
        }
    }

    fun styleOccurrences(subtext: Text, vararg styles: Any): Text {
        return TextWrapper.StyleOverlay(this) { context, text ->
            val matches = Pattern.compile("\\b${subtext.toString(context)}\\b").toRegex().findAll(text)
            matches.flatMap { match ->
                styles.map { style -> RangeStyle(match.range, style) } + listOfNotNull(
                    subtext.style?.let { RangeStyle(match.range, it) }
                )
            }
        }
    }

    fun styleOccurrences(getPattern: (context: Context)->Pattern, vararg styles: Any): Text {
        return TextWrapper.StyleOverlay(this) { context, text ->
            val matches = getPattern(context).toRegex().findAll(text)
            matches.flatMap { match ->
                styles.map { style -> RangeStyle(match.range, style) }
            }
        }
    }

    fun formatString(vararg args: Any): Text {
        return replaceAll(
            Pattern.compile("%[^a-z]*[0-9a-z]+") to { match, index ->
                args.getOrNull(index)?.let { argument ->
                    if (argument is Pair<*,*>)
                        Text(String.format(match.value, argument.first), style = argument.second)
                    else
                        Text(String.format(match.value, argument))
                } ?: Text(match.value)
            }
        )
    }

    @JvmName("replaceAllByPattern")
    fun replaceAll(vararg slots: Pair<Pattern, (MatchResult, Int)->Text>): Text =
        TextWrapper.ReplaceSlots(this, *slots.map { ReplacementSlot(
            regex = { _ -> it.first.toRegex() },
            replacement = it.second,
            replaceAll = true,
        ) }.toTypedArray())

    @JvmName("replaceAllByString")
    fun replaceAll(vararg slots: Pair<String, Text>): Text =
        TextWrapper.ReplaceSlots(this, *slots.map { ReplacementSlot(
            regex = { _ -> Pattern.compile(Regex.escape(it.first)).toRegex() },
            replacement = { _, _ -> it.second },
            replaceAll = true,
        ) }.toTypedArray())

    @JvmName("replaceByPattern")
    fun replaceOnce(vararg slots: Pair<Pattern, (MatchResult)->Text>): Text =
        TextWrapper.ReplaceSlots(this, *slots.map { ReplacementSlot(
            regex = { _ -> it.first.toRegex() },
            replacement = { match, _ -> it.second(match) },
            replaceAll = false,
        ) }.toTypedArray())

    @JvmName("replaceByString")
    fun replaceOnce(vararg slots: Pair<String, Text>): Text =
        TextWrapper.ReplaceSlots(this, *slots.map { ReplacementSlot(
            regex = { _ -> Pattern.compile(Regex.escape(it.first)).toRegex() },
            replacement = { _, _ -> it.second },
            replaceAll = false,
        ) }.toTypedArray())

    private data class ReplacementSlot(
        val regex: (context: Context) -> Regex?,
        val replacement: (MatchResult, Int)->Text,
        val replaceAll: Boolean,
    )

    fun getStyleRanges(context: Context, offset: Int = 0)
        = getStyleRanges(context).map { it.offset(offset) }

    protected open fun getStyleRanges(context: Context) = sequence {
        val text = toString(context)
        if (text.isNotEmpty()) {
            style?.let {
                yield(
                    RangeStyle(
                        range = IntRange(0, text.length - 1),
                        style = it
                    )
                )
            }
        }
    }

    abstract val style: Any?

    abstract fun toString(context: Context): String

    override fun toString(): String {
        return toString(TextManager.instance.context)
    }

    class Static(val text: String, override val style: Any? = null): Text() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Static

            if (text != other.text) return false
            if (style != other.style) return false

            return true
        }

        override fun hashCode(): Int {
            var result = text.hashCode()
            result = 31 * result + (style?.hashCode() ?: 0)
            return result
        }

        override fun toString(context: Context) = text
    }

    class FromResource(@StringRes val id: Int, override val style: Any? = null): Text() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as FromResource

            if (id != other.id) return false
            if (style != other.style) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id
            result = 31 * result + (style?.hashCode() ?: 0)
            return result
        }

        override fun toString(context: Context)
                = context.getString(id)
    }

    class PickPlural(@PluralsRes val id: Int, val quantity: Int, override val style: Any? = null): Text() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PickPlural

            if (id != other.id) return false
            if (quantity != other.quantity) return false
            if (style != other.style) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id
            result = 31 * result + quantity
            result = 31 * result + (style?.hashCode() ?: 0)
            return result
        }

        override fun toString(context: Context): String {
            return context.resources.getQuantityString(id, quantity)
        }
    }

    private class Separated(val separator: Text?, override val style: Any?, vararg val parts: Text?) : Text() {
        override fun toString(context: Context): String {
            val sb = StringBuilder()

            var pendingSeparator: String? = null
            for (span in parts) {
                if (span == null)
                    continue

                pendingSeparator?.let { sb.append(it) }
                sb.append(span.toString(context))
                pendingSeparator = pendingSeparator ?: separator?.toString(context)
            }

            return sb.toString()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Separated

            if (separator != other.separator) return false
            if (style != other.style) return false
            if (!parts.contentEquals(other.parts)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = separator?.hashCode() ?: 0
            result = 31 * result + (style?.hashCode() ?: 0)
            result = 31 * result + parts.contentHashCode()
            return result
        }

        override fun getStyleRanges(context: Context) = sequence {
            yieldAll(super.getStyleRanges(context))

            val separatorStyles = separator?.getStyleRanges(context, 0)?.toList()

            var partIndex = 0
            for (part in parts.filterNotNull()) {
                if (0 != partIndex) {
                    if (separatorStyles != null) {
                        yieldAll(separatorStyles.map { it.offset(partIndex) })
                        partIndex += separator?.toString(context)?.length ?: 0
                    }
                }

                yieldAll(part.getStyleRanges(context, partIndex))
                partIndex += part.toString(context).length
            }
        }
    }

    private sealed class TextWrapper(val text: Text) : Text() {
        override fun getStyleRanges(context: Context) = sequence {
            yieldAll(this@TextWrapper.text.getStyleRanges(context) + super.getStyleRanges(context))
        }

        class StyleOverlay(text: Text, val getSpans: (context: Context, text: String) -> Sequence<RangeStyle>) : TextWrapper(text) {
            override val style: Any? = null

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as StyleOverlay

                if (text != other.text) return false
                if (getSpans != other.getSpans) return false

                return true
            }

            override fun hashCode(): Int {
                var result = text.hashCode()
                result = 31 * result + getSpans.hashCode()
                return result
            }

            override fun toString(context: Context) = text.toString(context)

            override fun getStyleRanges(context: Context) = sequence {
                yieldAll(super.getStyleRanges(context))
                yieldAll(this@StyleOverlay.getSpans.invoke(context, toString(context)))
            }
        }

        class ReplaceSlots(text: Text, vararg val args: ReplacementSlot): TextWrapper(text) {
            override val style: Any? = null

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as ReplaceSlots

                if (text != other.text) return false
                if (!args.contentEquals(other.args)) return false
                if (style != other.style) return false

                return true
            }

            override fun hashCode(): Int {
                var result = text.hashCode()
                result = 31 * result + args.contentHashCode()
                result = 31 * result + (style?.hashCode() ?: 0)
                return result
            }

            override fun toString(context: Context): String {
                val baseString = this.text.toString(context)
                val finalString = java.lang.StringBuilder(baseString)

                findReplacements(baseString, context).forEach { match ->
                    finalString.replace(match.rangeBeforeReplace.first, match.rangeBeforeReplace.last + 1, match.textStrAfter)
                }

                return finalString.toString()
            }

            override fun getStyleRanges(context: Context): Sequence<RangeStyle> {
                val baseString = this.text.toString(context)
                val baseSpans = super.getStyleRanges(context)
                return sequence {
                    val replacements = findReplacements(baseString, context).toList()

                    baseSpans.forEach { baseSpan ->
                        val outside = replacements.filter { it.originalRange.first < baseSpan.range.first && it.originalRange.last > baseSpan.range.last }
                        val startedBeforeAndFinishInside = replacements.filter { it.originalRange.first < baseSpan.range.first && it.originalRange.last >= baseSpan.range.first && it.originalRange.last <= baseSpan.range.last }
                        val trimStart = startedBeforeAndFinishInside.maxByOrNull { it.originalRange.first }?.let {
                            baseSpan.range.last - it.rangeAfterReplace.first
                        } ?: 0

                        if (trimStart <= baseSpan.range.last - baseSpan.range.first && outside.isEmpty()) {
                            val before = replacements.filter { it.originalRange.last < baseSpan.range.first }
                            val offsetStartChange = before.sumOf { it.sizeChange }
                            val during = replacements.filter { it.originalRange.first >= baseSpan.range.first && it.originalRange.last <= baseSpan.range.last }
                            val offsetSizeChange = during.sumOf { it.sizeChange }

                            val startInsideAndFinishAfter = replacements.filter { it.originalRange.first >= baseSpan.range.first && it.originalRange.last > baseSpan.range.last }
                            val trimEnd = startInsideAndFinishAfter.minByOrNull { it.originalRange.first }?.let {
                                baseSpan.range.last - it.rangeAfterReplace.first + 1
                            }?.coerceAtLeast(0) ?: 0

                            yield(baseSpan.offset(offsetStartChange + trimStart, offsetSizeChange - trimStart - trimEnd))
                        }
                    }

                    replacements.forEach { replace ->
                        val matchSpans = replace.text.getStyleRanges(context, replace.rangeAfterReplace.first)
                        yieldAll(matchSpans)
                    }
                }
            }

            private data class ReplacementMatch(
                val originalRange: IntRange,
                val rangeBeforeReplace: IntRange,
                val rangeAfterReplace: IntRange,
                val sizeChange: Int,
                val textStrAfter: String,
                val text: Text,
            )

            private fun findReplacements(baseString: String, context: Context) = sequence {

                val usedReplacements = mutableListOf<IntRange>()
                val usedSlots = mutableSetOf<ReplacementSlot>()

                var matchOffset = 0
                val matches = findOrderedMatches(context, baseString)
                matches.forEach { (range, replacement, slot) ->

                    if (!usedReplacements.all { usedRange -> range.last < usedRange.first.coerceAtMost(usedRange.last) || range.first > usedRange.last } )
                        return@forEach

                    if (!slot.replaceAll && !usedSlots.add(slot))
                        return@forEach

                    usedReplacements.add(range)

                    val replacementStr = replacement.toString(context)
                    val baseRange = IntRange(range.first + matchOffset, range.last + matchOffset)
                    val sizeChange = replacementStr.length - (range.last - range.first) - 1
                    val finalRange = IntRange(range.first + matchOffset, range.last + matchOffset + (sizeChange))

                    yield ( ReplacementMatch(
                        originalRange = range,
                        rangeBeforeReplace = baseRange,
                        rangeAfterReplace = finalRange,
                        sizeChange = sizeChange,
                        textStrAfter = replacementStr,
                        text = replacement
                    ) )

                    matchOffset += sizeChange
                }
            }

            private fun findOrderedMatches(
                context: Context,
                baseString: String
            ): List<Triple<IntRange, Text, ReplacementSlot>> {
                val replacements = mutableListOf<Triple<IntRange, Text, ReplacementSlot>>()

                args.forEach { slot ->
                    val validMatches =
                        slot.regex(context)?.findAll(baseString, 0)
                    var index = 0
                    validMatches?.forEach {
                        replacements.add(
                            Triple(it.range, slot.replacement.invoke(it, index), slot)
                        )
                        index += 1
                    }
                }

                return replacements.sortedWith(compareBy({ it.first.first }, { it.first.last }))
            }
        }
    }
}
