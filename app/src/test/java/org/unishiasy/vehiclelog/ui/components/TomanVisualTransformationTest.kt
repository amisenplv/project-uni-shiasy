package org.unishiasy.vehiclelog.ui.components

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TomanVisualTransformationTest {
    private val transformation = TomanVisualTransformation()

    @Test
    fun formatsNumericTomanWithSeparatorsAndPreservesDecimalsAndEdgeInputs() {
        assertEquals("", transformation.filter(AnnotatedString("")).text.text)
        assertEquals("۱۲٬۰۰۰", transformation.filter(AnnotatedString("12000")).text.text)
        assertEquals("۲۵۰٬۰۰۰٫۵", transformation.filter(AnnotatedString("250000.5")).text.text)
        assertEquals("۱۲٬۰۰۰٫۱", transformation.filter(AnnotatedString("١٢٬۰۰۰٫۱")).text.text)
        assertEquals("۱۲٫۰۰", transformation.filter(AnnotatedString("۱۲٫۰۰")).text.text)
        assertEquals("invalid", transformation.filter(AnnotatedString("invalid")).text.text)
        // Very long digit string must not overflow Long/Int
        assertEquals(
            "۹۹٬۹۹۹٬۹۹۹٬۹۹۹٬۹۹۹٬۹۹۹٬۹۹۹٬۹۹۹٬۹۹۹",
            transformation.filter(AnnotatedString("99999999999999999999999999")).text.text,
        )
    }

    @Test
    fun offsetMappingRemainsBoundedMonotonicAndInvertibleAcrossAllIndices() {
        val samples = listOf(
            "",
            "1",
            "123",
            "1234",
            "123456",
            "1234567.8",
            "١٢٣٤٥٦٧",
            "۱۲٬۰۰۰٫۱",
            "۱۲٫۰۰",
            "invalid",
        )
        for (raw in samples) {
            val transformed = transformation.filter(AnnotatedString(raw))
            val display = transformed.text.text
            val mapping = transformed.offsetMapping

            var prevOrigToTrans = -1
            for (orig in 0..raw.length) {
                val mapped = mapping.originalToTransformed(orig)
                assertTrue("$raw orig=$orig -> $mapped out of range", mapped in 0..display.length)
                assertTrue("$raw origToTrans not monotonic at $orig", mapped >= prevOrigToTrans)
                assertEquals("$raw roundtrip orig=$orig", orig, mapping.transformedToOriginal(mapped))
                prevOrigToTrans = mapped
            }

            var prevTransToOrig = -1
            for (trans in 0..display.length) {
                val mapped = mapping.transformedToOriginal(trans)
                assertTrue("$raw trans=$trans -> $mapped out of range", mapped in 0..raw.length)
                assertTrue("$raw transToOrig not monotonic at $trans", mapped >= prevTransToOrig)
                prevTransToOrig = mapped
            }
        }
    }
}
