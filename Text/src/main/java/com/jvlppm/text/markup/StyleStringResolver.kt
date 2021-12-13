package com.jvlppm.text.markup

import android.content.Context
import com.jvlppm.text.extensions.valueOrNull
import java.util.regex.Pattern

class StyleStringResolver(
    private val resolvers: MutableMap<String, StyleStringAttributeResolver> = mutableMapOf()
) : StyleMarkupResolver {
    operator fun set(key: String, markupResolver: StyleStringAttributeResolver) {
        resolvers[key] = markupResolver
    }

    operator fun set(key: String, markupResolver: (context: Context) -> Any?) {
        resolvers[key] = StyleStringAttributeResolver { context, _: String? ->
            markupResolver(context)?.let {
                yield(it)
            }
        }
    }

    @JvmName("setNullableStringValueResolver")
    operator fun set(key: String, markupResolver: (context: Context, value: String?) -> Any?) {
        resolvers[key] = StyleStringAttributeResolver { context, value: String? ->
            markupResolver(context, value)?.let {
                yield(it)
            }
        }
    }
    @JvmName("setNonNullableStringValueResolver")
    operator fun set(key: String, markupResolver: (context: Context, value: String) -> Any?) {
        resolvers[key] = StyleStringAttributeResolver { context, value: String ->
            markupResolver(context, value)?.let {
                yield(it)
            }
        }
    }

    @JvmName("setNullableIntValueResolver")
    operator fun set(key: String, markupResolver: (context: Context, value: Int?) -> Any?) {
        resolvers[key] = StyleStringAttributeResolver { context, value: Int? ->
            markupResolver(context, value)?.let {
                yield(it)
            }
        }
    }
    @JvmName("setNonNullableIntValueResolver")
    operator fun set(key: String, markupResolver: (context: Context, value: Int) -> Any?) {
        resolvers[key] = StyleStringAttributeResolver { context, value: Int ->
            markupResolver(context, value)?.let {
                yield(it)
            }
        }
    }

    @JvmName("setNullableFloatValueResolver")
    operator fun set(key: String, markupResolver: (context: Context, value: Float?) -> Any?) {
        resolvers[key] = StyleStringAttributeResolver { context, value: Float? ->
            markupResolver(context, value)?.let {
                yield(it)
            }
        }
    }
    @JvmName("setNonNullableFloatValueResolver")
    operator fun set(key: String, markupResolver: (context: Context, value: Float) -> Any?) {
        resolvers[key] = StyleStringAttributeResolver { context, value: Float ->
            markupResolver(context, value)?.let {
                yield(it)
            }
        }
    }

    @JvmName("setNullableDoubleValueResolver")
    operator fun set(key: String, markupResolver: (context: Context, value: Double?) -> Any?) {
        resolvers[key] = StyleStringAttributeResolver { context, value: Double? ->
            markupResolver(context, value)?.let {
                yield(it)
            }
        }
    }
    @JvmName("setNonNullableDoubleValueResolver")
    operator fun set(key: String, markupResolver: (context: Context, value: Double) -> Any?) {
        resolvers[key] = StyleStringAttributeResolver { context, value: Double ->
            markupResolver(context, value)?.let {
                yield(it)
            }
        }
    }

    fun remove(key: String): StyleStringAttributeResolver? {
        return resolvers.remove(key)
    }

    fun clear() {
        resolvers.clear()
    }

    fun copy() = StyleStringResolver(resolvers.toMutableMap())

    private val stringStyleRx = Pattern.compile("(?<Key>[^:;]+)(:(?<Value>[^;]*))?").toRegex()

    override fun resolveMarkupForStyle(context: Context, style: Any) = sequence {
        if (style is String && resolvers.any()) {
            stringStyleRx.findAll(style).forEach { match ->
                val key = match.groups[1]?.value?.trim()?.valueOrNull() ?: return@forEach
                val value = match.groups[3]?.value?.trim()?.valueOrNull()

                resolvers[key]?.let {
                    yieldAll(it.resolveMarkupForStyleString(context, value))
                }
            }
        }
    }
}
