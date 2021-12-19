package com.jvlppm.text.extensions

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import com.jvlppm.text.Text
import com.jvlppm.text.TextManager
import com.jvlppm.text.markup.StyleMarkupResolver
import java.util.regex.Pattern

internal fun String.valueOrNull() = this.takeUnless { isNullOrBlank() }

fun Text.asSpannable(context: Context, styleMarkupResolver: StyleMarkupResolver? = null): SpannableString {
    val markupRepository = styleMarkupResolver ?: TextManager.instance.config.globalResolver
    val spannableString = SpannableString(toString(context))
    getStyleRanges(context).forEach {
        for (markup in markupRepository.resolveMarkupForStyle(context, it.style)) {
            spannableString.setSpan(
                markup, it.range.first, it.range.first + (it.range.last - it.range.first + 1),
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }
    }
    return spannableString
}

private val propertiesAttributeRx = Pattern.compile("\\b(\\w+)=(((?!['\"])[^ ]+)|(?<quote>[\"\'])(((?!\\k<quote>).)*)\\k<quote>)").toRegex()

private fun getFindTagPattern(): String {
    val capturingOpenRx = "<(\\w+)( [^>]*)?>"
    val increaseLevel = "<\\1[\\s>]"
    val decreaseLevel = "<\\/\\1>"
    val closeAnyRx = "<\\/\\w+>"
    return "(?=$capturingOpenRx)(?:(?=.*?$increaseLevel(?!.*?\\3)(.*$closeAnyRx(?!.*\\4).*))(?=.*?$decreaseLevel(?!.*?\\4)(?<second>.*)).)+?.*?(?=\\3)((?!$increaseLevel).)*(?=\\4\$)"
}

private fun decodeTag(style: (String)->Any?): Pair<Pattern, (MatchResult, Int)->Text> {
    val openClosePattern = Pattern.compile(getFindTagPattern(), Pattern.DOTALL)
    val outerTagRx = Pattern.compile("<(\\w+)( [^>]*)?>(?<content>.*)<\\/\\1>").toRegex()

    fun getStyle(tag: String, tagStyle: Sequence<Pair<String,String>>) = listOfNotNull(
        style(tag),
    ) + tagStyle.map { if (it.first == "style") it.second else "${it.first}:${it.second}" }

    return openClosePattern to { match, _ ->
        val tagName = match.groupValues[1]
        val tagProperties = match.groupValues[2]
        val properties = propertiesAttributeRx.findAll(tagProperties).map { m -> m.groupValues[1] to (m.groupValues[3].valueOrNull() ?: m.groupValues[5]) }

        val content = outerTagRx.matchEntire(match.groupValues[0])?.let { outerMatch ->
            outerMatch.groupValues[3].trim()
        } ?: run {
            match.groupValues[0].dropWhile { it != '>' }.drop(1).dropLastWhile { it != '<' }.dropLast(1).trim()
        }
        Text(content, style = getStyle(tag = tagName, tagStyle = properties)).decodeStyleTagsInternal()
    }
}

private fun Text.decodeStyleTagsInternal(): Text {
    return this.replaceAll(
        decodeTag { tagName ->
           when (tagName) {
                "u" -> "underline"
                "s" -> "strike"
                "i" -> "italic"
                "b" -> "bold"
                "small" -> "scale:0.66"
                "big" -> "scale:1.33"
                else -> tagName
           }
        },
    )
}

private fun Text.decodeLineBreakTags(): Text {
    val breakLineBetweenTags = listOf("p", "div")
    val breakTagsRx = breakLineBetweenTags.joinToString("|") { "\\b$it\\b" }
    return this.replaceAll(
        Pattern.compile("[\\s]+") to { _, _ -> Text.space },
    ).replaceAll(
        Pattern.compile("<br/?>") to { _, _ -> Text.lineBreak },
        // Line break before first block tag
        Pattern.compile("(?<!<\\/($breakTagsRx)>)(?<=[a-z-9<>])\\s*(?=<($breakTagsRx))") to { match, _ ->
            Text.concatenate(
                Text("\n"),
                Text("\n", style = "scale:0.5").takeIf { match.groupValues[2] == "p" }
            )
        },
        // Line break after block tags
        Pattern.compile("(?<=<\\/($breakTagsRx)>)(\\s*)") to { match, _ ->
            Text.concatenate(
                Text("\n"),
                Text("\n", style = "scale:0.5").takeIf { match.groupValues[1] == "p" }
            )
        },
    )
}

fun Text.decodeStyleTags(decodeLineBreaks: Boolean = false) = this.let {
    if (decodeLineBreaks) {
        it.decodeLineBreakTags()
    }
    else it
}.decodeStyleTagsInternal()

fun Text.trim() = this.replaceAll(
    Pattern.compile("^[\\s]+") to { _, _ -> Text.empty },
    Pattern.compile("[\\s]+$") to { _, _ -> Text.empty },
)

