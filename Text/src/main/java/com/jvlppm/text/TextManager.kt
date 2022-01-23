package com.jvlppm.text

import android.content.Context
import android.graphics.Typeface
import android.text.style.*
import com.jvlppm.text.extensions.decodeColor
import com.jvlppm.text.extensions.decodeResourceId
import com.jvlppm.text.markup.CombinedMarkupResolver
import com.jvlppm.text.markup.StyleStringResolver
import com.jvlppm.text.spans.FontSpan
import java.lang.Exception
import java.lang.ref.WeakReference

class TextManager private constructor() {
    private var contextReference: WeakReference<Context>? = null

    val context: Context get() = contextReference?.get() ?: throw Exception("TextManager must be initialized")

    val markupResolver = CombinedMarkupResolver(
        stringResolver = StyleStringResolver().apply {
            this["underline"] = { _ -> UnderlineSpan() }
            this["strike"] = { _ -> StrikethroughSpan() }
            this["italic"] = { _ -> StyleSpan(Typeface.ITALIC) }
            this["bold"] = { _ -> StyleSpan(Typeface.BOLD) }
            this["strong"] = { _ -> StyleSpan(Typeface.BOLD) }
            this["font"] = { context, value: String ->
                context.decodeResourceId(value, "font")?.let {
                    FontSpan(context, it)
                }
            }
            this["font-family"] = { _, value: String ->
                TypefaceSpan(value)
            }
            this["color"] = { context, value: String ->
                value.decodeColor(context)?.let {
                    ForegroundColorSpan(it)
                }
            }
            this["scale"] = { _, value: Float -> RelativeSizeSpan(value) }
        }
    ).apply {
        registerClickableSpan<()-> Any?> { it.invoke() }
    }

    companion object {
        val instance = TextManager()

        fun initialize(context: Context) {
            instance.contextReference = WeakReference(context)
        }

        val styles get() = instance.markupResolver.stringResolver

        inline fun <reified T> registerClickableSpan(crossinline invoke: (T)->Unit)
            = instance.markupResolver.registerClickableSpan(invoke)
    }
}
