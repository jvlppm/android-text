package com.jvlppm.text.sample

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.jvlppm.text.Text
import com.jvlppm.text.extensions.decodeStyleTags
import java.util.regex.Pattern

class MainViewModel : ViewModel() {
    // Simple Text Samples
    val staticText get() = Text("Static text sample")
    val resourceText get() = Text(R.string.sample_resource_text)
    val concatenateText get() = Text.concatenate(
        Text("Concatenated "),
        Text("text")
    )
    val spacedText get() = Text.spaced(
        Text("Spaced"),
        Text("text")
    )
    val joinedText get() = Text.joining(separator = Text(", "),
        Text("First"),
        Text("second"),
        Text("third")
    )
    val multiLineText get() = Text.lines(
        Text("First line"),
        Text("second line"),
    ).terminated()

    // Text Styling Samples
    val simpleStyle get() = Text("Simple text with style", style = "bold")
    val concatenatedStyle get() = Text.joining(separator = Text.space,
        Text("Sample with"),
        Text("stylized", style = listOf("strike", "scale:2", "italic")),
        Text("word"),
    )
    val styleOccurrences get() = Text("Text with style overlay")
        .styleOccurrences(Text("style"), "scale:1.5")
    val styleWithTags get() = Text("Simple <i>text</i> with <u>basic</u> tags")
        .decodeStyleTags()
    val colorSamples get() = Text.lines(
        Text("Text with color by reference", style = "color:${R.color.teal_700}"),
        Text("Text with color by name", style = "color:purple_500"),
        Text("Text with color from attr")
            .styleOccurrences(Text("color"), "color:?${android.R.attr.textColorPrimary}")
            .styleOccurrences(Text("attr"), "color:?android:textColorPrimary")
    )
    val fontSamples get() = Text.lines(
        Text("Text with font by reference", style = "font:${R.font.poppins_black}"),
        Text("Text with font by name", style = "font:poppins_italic"),
        Text("Text with font by family", style = "font-family:monospace"),
    )

    // Format Text Samples
    val stringFormat get() = Text(R.string.sample_format_text)
        .formatString(
            "String",
            "and formatting" to "color:red"
        )
    val replaceSlots get() = Text(R.string.sample_format_text)
        .replaceOnce(
            "%s" to Text("sample"),
            "%s" to Text("slots", style = "bold"),
        )

    val replaceVariable get() = Text("Replace {sample} and {sample}")
        .replaceAll(
            "{sample}" to Text("variable", style = "color:red"),
        )

    val replaceByPattern get() = Text("Simple text with pattern replacement")
        .replaceAll(Pattern.compile("\\b[\\w]{4}\\b") to { match -> Text(match.value.uppercase(), style = "color:red") })

    private val _clickCount = MutableLiveData(0)

    val clickSample = Transformations.map(_clickCount) { clickCount ->
        Text("You clicked %d times", style = "scale:2")
            .formatString(clickCount to "color:?attr/colorPrimary;bold;scale:2")
            .styleOccurrences(Text("clicked", "underline"), { _clickCount.value = _clickCount.value!! + 1 })
    }
}