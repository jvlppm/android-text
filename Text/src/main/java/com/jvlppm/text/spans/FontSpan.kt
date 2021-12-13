package com.jvlppm.text.spans

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.util.LruCache
import androidx.core.content.res.ResourcesCompat

class FontSpan(context: Context, fontId: Int) : MetricAffectingSpan() {
    private var mTypeface: Typeface?
    override fun updateMeasureState(p: TextPaint) {
        p.typeface = mTypeface

        // Note: This flag is required for proper typeface rendering
        p.flags = p.flags or Paint.SUBPIXEL_TEXT_FLAG
    }

    override fun updateDrawState(tp: TextPaint) {
        tp.typeface = mTypeface
        tp.flags = tp.flags or Paint.SUBPIXEL_TEXT_FLAG
    }

    companion object {
        /** An `LruCache` for previously loaded typefaces.  */
        private val sTypefaceCache: LruCache<Int, Typeface> = LruCache<Int, Typeface>(12)
    }

    /**
     * Load the [Typeface] and apply to a [android.text.Spannable].
     */
    init {
        mTypeface = sTypefaceCache.get(fontId)
        if (mTypeface == null) {
            mTypeface = ResourcesCompat.getFont(context, fontId)

            // Cache the loaded Typeface
            sTypefaceCache.put(fontId, mTypeface)
        }
    }
}