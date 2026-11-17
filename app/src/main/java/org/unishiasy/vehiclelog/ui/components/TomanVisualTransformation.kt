package org.unishiasy.vehiclelog.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import org.unishiasy.vehiclelog.domain.normalizeInputDigits

internal class TomanVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val result = formatTomanLiveDisplay(text.text)
        return TransformedText(
            text = AnnotatedString(result.display),
            offsetMapping = ArrayOffsetMapping(result.origToTrans, result.transToOrig),
        )
    }
}

internal data class TomanTransformed(
    val display: String,
    val origToTrans: IntArray,
    val transToOrig: IntArray,
)

internal fun formatTomanLiveDisplay(raw: String): TomanTransformed {
    if (raw.isEmpty()) {
        return TomanTransformed(
            display = "",
            origToTrans = intArrayOf(0),
            transToOrig = intArrayOf(0),
        )
    }

    val normalizedChars = CharArray(raw.length) { idx ->
        when (val ch = raw[idx]) {
            in '0'..'9' -> ('۰'.code + (ch.code - '0'.code)).toChar()
            in '\u0660'..'\u0669' -> ('۰'.code + (ch.code - '\u0660'.code)).toChar()
            '.' -> '٫'
            else -> ch
        }
    }
    val normalized = String(normalizedChars)

    val asciiNormalized = raw.normalizeInputDigits().replace('٫', '.')
    val canGroup = ',' !in raw && '٬' !in raw && asciiNumericPattern.matches(asciiNormalized)
    if (!canGroup) {
        val identity = IntArray(raw.length + 1) { it }
        return TomanTransformed(display = normalized, origToTrans = identity, transToOrig = identity)
    }

    val decimalIdx = normalized.indexOf('٫')
    val intEnd = if (decimalIdx >= 0) decimalIdx else normalized.length
    val separatorCount = if (intEnd > 3) (intEnd - 1) / 3 else 0
    if (separatorCount == 0) {
        val identity = IntArray(raw.length + 1) { it }
        return TomanTransformed(display = normalized, origToTrans = identity, transToOrig = identity)
    }

    val formattedLen = normalized.length + separatorCount
    val sb = StringBuilder(formattedLen)
    val origToTrans = IntArray(raw.length + 1)
    val transToOrig = IntArray(formattedLen + 1)

    for (i in 0 until intEnd) {
        if (i > 0 && (intEnd - i) % 3 == 0) {
            transToOrig[sb.length] = i
            sb.append('٬')
        }
        origToTrans[i] = sb.length
        transToOrig[sb.length] = i
        sb.append(normalized[i])
    }
    for (i in intEnd until normalized.length) {
        origToTrans[i] = sb.length
        transToOrig[sb.length] = i
        sb.append(normalized[i])
    }
    origToTrans[raw.length] = sb.length
    transToOrig[sb.length] = raw.length

    return TomanTransformed(
        display = sb.toString(),
        origToTrans = origToTrans,
        transToOrig = transToOrig,
    )
}

private val asciiNumericPattern = Regex("""\d+(?:\.\d*)?""")

private class ArrayOffsetMapping(
    private val origToTrans: IntArray,
    private val transToOrig: IntArray,
) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        val bounded = offset.coerceIn(0, origToTrans.lastIndex)
        return origToTrans[bounded]
    }

    override fun transformedToOriginal(offset: Int): Int {
        val bounded = offset.coerceIn(0, transToOrig.lastIndex)
        return transToOrig[bounded]
    }
}
