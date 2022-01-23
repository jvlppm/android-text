package com.jvlppm.text.bindings

import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.jvlppm.text.Text
import com.jvlppm.text.extensions.asSpannable


@BindingAdapter("android:text")
fun TextView.setText(text: Text?) {
    val spannableString = text?.asSpannable(context)

    val hasLinks = !spannableString?.getSpans(
        0, spannableString.length,
        ClickableSpan::class.java
    ).isNullOrEmpty()

    this.text = spannableString

    if (hasLinks) {
        movementMethod = LinkMovementMethod.getInstance()
    }
}

@BindingAdapter("android:visibility")
fun setVisibility(view: TextView, text: Text?) {
    val context = view.context
    view.visibility = when (text?.toString(context).isNullOrBlank()) {
        true -> View.GONE
        false -> View.VISIBLE
    }
}