package com.jvlppm.text

import android.content.Context
import androidx.startup.Initializer

internal class TextManagerInitializer : Initializer<TextManager> {
    override fun create(context: Context): TextManager {
        TextManager.initialize(context)
        return TextManager.instance
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf()
    }
}
