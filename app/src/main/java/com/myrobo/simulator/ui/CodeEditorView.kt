package com.myrobo.simulator.ui

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import java.util.regex.Pattern

class CodeEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private var isHighlighting = false

    // Patterns
    private val keywordPattern = Pattern.compile(
        "\\b(void|setup|loop|pinMode|digitalWrite|digitalRead|analogWrite|analogRead|delay|millis|Serial|println|print|begin|int|float|bool|boolean|char|String|if|else|for|while|return)\\b"
    )
    private val constantPattern = Pattern.compile(
        "\\b(HIGH|LOW|INPUT|OUTPUT|INPUT_PULLUP|true|false)\\b"
    )
    private val numberPattern = Pattern.compile(
        "\\b\\d+\\b"
    )
    private val commentPattern = Pattern.compile(
        "//.*"
    )

    init {
        // Monospace font & dark IDE style
        typeface = android.graphics.Typeface.MONOSPACE
        setBackgroundColor(Color.parseColor("#121824"))
        setTextColor(Color.parseColor("#E0E6ED"))
        setHintTextColor(Color.parseColor("#546E7A"))
        textSize = 14f

        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                if (isHighlighting || editable == null) return
                applySyntaxHighlight(editable)
            }
        })
    }

    private fun applySyntaxHighlight(editable: Editable) {
        isHighlighting = true
        try {
            // Remove existing syntax spans
            val existingSpans = editable.getSpans(0, editable.length, ForegroundColorSpan::class.java)
            for (span in existingSpans) {
                editable.removeSpan(span)
            }

            // Highlight Constants (HIGH, LOW, OUTPUT) -> Green
            val constMatcher = constantPattern.matcher(editable)
            while (constMatcher.find()) {
                editable.setSpan(
                    ForegroundColorSpan(Color.parseColor("#00E676")),
                    constMatcher.start(),
                    constMatcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            // Highlight Keywords (void, pinMode, if, delay) -> Magenta/Cyan
            val kwMatcher = keywordPattern.matcher(editable)
            while (kwMatcher.find()) {
                editable.setSpan(
                    ForegroundColorSpan(Color.parseColor("#FF4081")),
                    kwMatcher.start(),
                    kwMatcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            // Highlight Numbers -> Yellow
            val numMatcher = numberPattern.matcher(editable)
            while (numMatcher.find()) {
                editable.setSpan(
                    ForegroundColorSpan(Color.parseColor("#FFD54F")),
                    numMatcher.start(),
                    numMatcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            // Highlight Comments -> Grey
            val commentMatcher = commentPattern.matcher(editable)
            while (commentMatcher.find()) {
                editable.setSpan(
                    ForegroundColorSpan(Color.parseColor("#78909C")),
                    commentMatcher.start(),
                    commentMatcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

        } finally {
            isHighlighting = false
        }
    }
}
