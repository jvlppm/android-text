package com.jvlppm.text

data class RangeStyle(val range: IntRange, val style: Any) {
    fun offset(delta: Int, size: Int = 0) = copy(
        range = IntRange(range.first + delta, range.last + delta + size),
        style = style,
    )
}