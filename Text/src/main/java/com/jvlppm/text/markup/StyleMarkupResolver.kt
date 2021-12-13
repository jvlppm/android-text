package com.jvlppm.text.markup

import android.content.Context

fun interface StyleMarkupResolver {
    fun resolveMarkupForStyle(context: Context, style: Any): Sequence<Any>
}

