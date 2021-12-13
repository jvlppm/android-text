package com.jvlppm.text.extensions

import android.content.Context
import android.content.res.Resources
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

private val styleAttributeRx = Pattern.compile(".* style=[\"']([^\"']+?)[\"']").toRegex()

private fun decodeTag(name: String, style: (Text)->Text): Pair<Pattern, (MatchResult, Int)->Text> {
    val openRx = "<$name[\\s>]"
    val closeRx = "<\\/$name>"

    val openClosePattern = Pattern.compile("(?=$openRx)(?:(?=.*?$openRx(?!.*?\\1)(.*$closeRx(?!.*\\2).*))(?=.*?$closeRx(?!.*?\\2)(?<second>.*)).)+?.*?(?=\\1)((?!$openRx).)*(?=\\2\$)")

    val outerTagRx = Pattern.compile("<$name( style=(?<quote>['\"])(((?!\\k<quote>).)*)\\k<quote>)?\\s*>(?<content>.*)<\\/$name>").toRegex()

    return openClosePattern to { match, _ ->
        outerTagRx.matchEntire(match.groupValues[0])?.let {
            style(Text(it.groupValues[5].trim(), style = it.groupValues[3]).decodeStyleTagsInternal())
        } ?: run {
            // Fallback
            val openTagContent = match.groupValues[0].takeWhile { it != '>' }
            val content = match.groupValues[0].dropWhile { it != '>' }.drop(1).dropLastWhile { it != '<' }.dropLast(1).trim()
            val styleContent = styleAttributeRx.matchEntire(openTagContent)?.let {
                it.groupValues[1]
            }
            style(Text(content, style = styleContent).decodeStyleTagsInternal())
        }
    }
}

private fun Text.decodeStyleTagsInternal(): Text {
    val breakLineBetweenTags = listOf("p", "div")
    val breakTagsRx = breakLineBetweenTags.joinToString("|") { "\\b$it\\b" }
    return this.replaceAll(
        Pattern.compile("<br/?>") to { _, _ -> Text.lineBreak },
        decodeTag("u") { it.withStyle("underline") },
        decodeTag("s") { it.withStyle("strike") },
        decodeTag("strike") { it.withStyle("strike") },
        decodeTag("i") { it.withStyle("italic") },
        decodeTag("b") { it.withStyle("bold") },
        decodeTag("strong") { it.withStyle("strong") },
        decodeTag("small") { it.withStyle("scale:0.66") },
        decodeTag("big") { it.withStyle("scale:1.33") },

        decodeTag("p") { it },
        decodeTag("div") { it },

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
    ).trim()
}

fun Text.decodeStyleTags() = this.replaceAll(
    Pattern.compile("[\\s]+") to { _, _ -> Text.space },
).decodeStyleTagsInternal()

fun Text.trim() = this.replaceAll(
    Pattern.compile("^[\\s]+") to { _, _ -> Text.empty },
    Pattern.compile("[\\s]+$") to { _, _ -> Text.empty },
)
