# android-text

Create and style a text from android ViewModel, without depending on a context.

It can be easily converted to a SpannableString, providing a context, without worrying about ranges.

## Simple Usage

`com.jvlppm.text.Text` is a class that describes a text to be displayed

Your viewModel can create a Text using either a `String` or a `@StringRes Int`,
without a reference to context.

```kotlin
val staticText = Text("Simple Text")
val resourceText = Text(R.string.my_resource_text, style = "bold")
val concatenatedText = staticText + Text.space + resourceText
```

This `Text` can be manipulated / formatted by your view model,
with text replacement methods and other facilities.

The `Text` can be then displayed by using this extension method provided by this library.

```kotlin
import com.jvlppm.text.bindings.setText

/**
 * This method will resolve the Text and construct a `SpannableString` that represents it,
 * and it will attach it to your `TextView`.
 */
@BindingAdapter("android:text")
fun TextView.setText(text: Text?)
```

And since this method is declared as a `BindingAdapter`, you can also use `android:text` on your xml

```xml
<TextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="@{viewModel.concatenatedText}" />
```

You can also create the SpannableString / String and apply it manually.

```kotlin
import com.jvlppm.text.extensions

/**
 * This method creates a SpannableString with the corresponding text and styles applied.
 */
fun Text.asSpannable(context: Context): SpannableString

/**
 * This method resolves the Text into a simple String with no formatting information.
 */
fun Text.toString(context: Context): String
```

---

## Creating Text

A `Text` can be created with a `String` or a `@StringRes Int`, and it also accepts a style object that can be attached to your Text

```kotlin
val staticText = Text("Sample")
val resourceText = Text(R.string.my_resource_text)

val addingText = staticText + Text.space + resourceText

// Note that the final string will only be resolved later when the UI invoke toString(context)

val concatenated = Text.concatenated(
    staticText,
    Text.space,
    resourceText
)

val spaced = Text.spaced(
    staticText,
    resourceText
)

val multiline = Text.lines(
    Text("First line"),
    Text("Second line", style = null),
)
```

A text can also be formatted in the same way `String.format` works

```kotlin
val text = Text("You %s %d times")
    .formatString("clicked", 5)

val text = Text(R.string.hello_user)
    .formatString("Friend")

val text = Text("Hello %s, you clicked %d times")
    .formatString(
        "Friend" to "color:green",
        5 to "scale:3;bold"
    )
```

A text can also be manipulated with a replace method

```kotlin
val text = Text("Good morning")
    .replaceAll("morning" to Text("night"))

val text = Text("%s %s")
    .replaceOnce(
        "%s" to Text("Good"),
        "%s" to Text("morning"),
    )
```

---

## Styling Text

A `Text` can hold a style object that represents how it should be styled, this can be a `String` or any other object.

```kotlin
val text1 = Text("Static text", style = "bold")
val text2 = Text(R.string.my_sample_text, style = myCustomStyleObject)
```

Later when the `Text` gets displayed it will then try to decode your style object using `com.jvlppm.text.TextManager`.

`TextManager` is responsible to reading the style object, `"bold"` in this example, and converting it to a `StyleSpan(Typeface.BOLD)`
which will be used by your interface.

`TextManager` already has a few style decoders by default, those can be extended and configured.


### Default Styles

```kotlin
"underline" // Resolves to a UnderlineSpan()
"strike" // Resolves to a StrikethroughSpan()
"italic" // Resolves to a StyleSpan(Typeface.ITALIC)
"bold" // Resolves to a StyleSpan(Typeface.BOLD)
"strong" // Resolves to a StyleSpan(Typeface.BOLD)
"font:*" // Resolves to a MetricAffectingSpan that changes the typeface
"font-family:*" // Resolves to a TypefaceSpan
"color:*" // Resolves to a ForegroundColorSpan
"scale:*" // Resolves to a RelativeSizeSpan
```

Some styles accept parameters, like `scale` for example, and can be used as

```kotlin
Text("Scale sample", style = "scale:1.5")
```

There is also a default style resolver that converts a `()->Unit` into a `ClickableSpan`

```kotlin
Text("My clickable string", style = { onClickText() } )
```

### Custom Styles

A custom style can be registered with

```kotlin
TextManager.styles["test"] = { context -> UnderlineSpan() }
```

A style that accepts a parameter can be registered with

```kotlin
// This will replace the default styles
TextManager.styles["scale"] = { context, value: Float -> RelativeSizeSpan(value) }
TextManager.styles["font"] = { context, value: String -> MyCustomFontSpan(value) }
```

### Colors

A color style can be constructed in a few ways

```kotlin
// Using a direct color
Text("Red string", style = "color:${R.color.red}")
Text("Red string", style = "color:red") // same as above

// Using a theme attr
Text("Theme colored string", style = "color:?${android.R.attr.textColorPrimary}")
Text("Theme colored string", style = "color:?textColorPrimary") // same as above
```

A question mark indicates it is an attr value, and the value itself can be an Int or a name,

this behavior can be replaced by your custom attribute reader.

### Fonts

The default font attribute reader uses the same syntax as the colors described above

```kotlin
Text("Red string", style = "font:${R.font.poppins_black}")
Text("Red string", style = "font:poppins_black")
```


There is also an extension that replaces simple html tags to a string with an attached style,
example:

```kotlin
import com.jvlppm.text.extensions.decodeStyleTags

// Creates a text that styles the word "string" with "bold" and "styles" with "italic"
val text = Text("My simple <b>string</b> with <i>styles</i>")
    .decodeStyleTags()
```

---

## Setup

*Step 1.* Add the JitPack repository to your build file

Add jitpack.io as a repository on your settings.gradle

```
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

Older projects had the repositories defined inside build.gradle instead

```
buildscript {
    repositories {
        jcenter()
        // DO NOT ADD IT HERE!!!
    }
    ...
}

allprojects {
    repositories {
        mavenLocal()
        jcenter()
        // ADD IT HERE
        maven { url "https://jitpack.io" }
    }
}
```

*Step 2.* Add the dependency to your app's build.gradle
Check the releases page on github to see what is the latest version available

```
dependencies {
	implementation 'com.github.jvlppm:android-text:v0.5'
}
```
