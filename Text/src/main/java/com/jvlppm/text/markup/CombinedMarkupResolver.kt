package com.jvlppm.text.markup

import android.content.Context
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class CombinedMarkupResolver(
    var stringResolver: StyleStringResolver,
    private val resolvers: MutableList<StyleMarkupResolver> = mutableListOf()
): StyleMarkupResolver {

    fun add(resolver: StyleMarkupResolver) {
        resolvers.add(resolver)
    }

    fun remove(resolver: StyleMarkupResolver): Boolean {
        return resolvers.remove(resolver)
    }

    fun clear() {
        stringResolver.clear()
        resolvers.clear()
    }

    inline fun <reified T> registerClickableSpan(crossinline invoke: (T)->Unit) = add { _, style ->
        sequence {
            if (style is T) {
                yield(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        invoke(style)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = ds.linkColor
                        ds.isUnderlineText = false
                    }
                })
            }
        }
    }

    override fun resolveMarkupForStyle(context: Context, style: Any) = sequence {
        when (style) {
            is List<*> -> {
                style.forEach { s ->
                    if (s != null) {
                        yieldMarkupsForStyle(context, s)
                    }
                }
            }
            else -> {
                yieldMarkupsForStyle(context, style)
            }
        }
    }

    private suspend fun SequenceScope<Any>.yieldMarkupsForStyle(
        context: Context,
        s: Any
    ) {
        yieldAll(stringResolver.resolveMarkupForStyle(context, s))

        resolvers.forEach {
            yieldAll(it.resolveMarkupForStyle(context, s))
        }
    }
}