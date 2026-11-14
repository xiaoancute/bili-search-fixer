package io.github.xiaoancute.bilisearch.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueryTokenizerTest {

    @Test
    fun normalizesFullWidthAndPunctuation() {
        assertEquals("原神攻略", QueryTokenizer.normalize("  原神／攻略  "))
        assertEquals("fatestaynight", QueryTokenizer.normalize("Fate／Stay Night"))
    }

    @Test
    fun expandsChineseIntoBigrams() {
        val tokens = QueryTokenizer.tokens("命运石之门")
        assertTrue(tokens.contains("命运石之门"))
        assertTrue(tokens.contains("命运"))
        assertTrue(tokens.contains("运石"))
        assertTrue(tokens.contains("石之"))
        assertTrue(tokens.contains("之门"))
    }

    @Test
    fun splitsLatinOnWordBoundaries() {
        val tokens = QueryTokenizer.tokens("bad apple")
        assertTrue(tokens.contains("bad"))
        assertTrue(tokens.contains("apple"))
        assertTrue(tokens.contains("badapple"))
    }
}
