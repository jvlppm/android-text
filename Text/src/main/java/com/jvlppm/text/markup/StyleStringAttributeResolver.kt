package com.jvlppm.text.markup

import android.content.Context

interface StyleStringAttributeResolver {
    companion object {
        @JvmName("create")
        operator fun invoke(resolver: suspend SequenceScope<Any>.(context: Context, value: String?)->Unit) = object: StyleStringAttributeResolver {
            override fun resolveMarkupForStyleString(context: Context, value: String?) = sequence { resolver(context, value) }
        }
        @JvmName("createNonNullable")
        operator fun invoke(resolver: suspend SequenceScope<Any>.(context: Context, value: String)->Unit) = object: StyleStringAttributeResolver {
            override fun resolveMarkupForStyleString(context: Context, value: String?) = sequence {
                if (value != null) {
                    resolver(context, value)
                }
            }
        }

        @JvmName("createNullableDouble")
        operator fun invoke(
            resolver: suspend SequenceScope<Any>.(context: Context, value: Double?)->Unit,
        ) = invoke { context, value: String? ->
            this.resolver(context, value?.toDoubleOrNull())
        }

        @JvmName("createNonNullableDouble")
        operator fun invoke(
            resolver: suspend SequenceScope<Any>.(context: Context, value: Double)->Unit,
        ) = invoke { context, value: String? ->
            value?.toDoubleOrNull()?.let {
                this.resolver(context, it)
            }
        }

        @JvmName("createNullableFloat")
        operator fun invoke(
            resolver: suspend SequenceScope<Any>.(context: Context, value: Float?)->Unit,
        ) = invoke { context, value: String? ->
            this.resolver(context, value?.toFloatOrNull())
        }

        @JvmName("createNonNullableFloat")
        operator fun invoke(
            resolver: suspend SequenceScope<Any>.(context: Context, value: Float)->Unit,
        ) = invoke { context, value: String? ->
            value?.toFloatOrNull()?.let {
                this.resolver(context, it)
            }
        }

        @JvmName("createNullableInt")
        operator fun invoke(
            resolver: suspend SequenceScope<Any>.(context: Context, value: Int?)->Unit,
        ) = invoke { context, value: String? ->
            this.resolver(context, value?.toIntOrNull())
        }

        @JvmName("createNonNullableInt")
        operator fun invoke(
            resolver: suspend SequenceScope<Any>.(context: Context, value: Int)->Unit,
        ) = invoke { context, value: String? ->
            value?.toIntOrNull()?.let {
                this.resolver(context, it)
            }
        }
    }

    fun resolveMarkupForStyleString(context: Context, value: String?): Sequence<Any>
}
