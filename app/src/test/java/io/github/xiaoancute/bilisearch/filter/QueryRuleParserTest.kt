package io.github.xiaoancute.bilisearch.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueryRuleParserTest {

    @Test
    fun parsesAllModifiers() {
        val rule = QueryRuleParser.parse("摄影 -入门 #ai @影视飓风")

        assertEquals("摄影", rule.cleanQuery)
        assertEquals(listOf("摄影"), rule.requiredTitleTerms)
        assertEquals(listOf("入门"), rule.excludedTitleTerms)
        assertEquals(listOf("ai"), rule.requiredTags)
        assertEquals(listOf("影视飓风"), rule.allowedAuthors)
        assertTrue(rule.hasModifiers)
        assertTrue(rule.strictTitle)
    }

    @Test
    fun keepsPlainQueryUntouched() {
        val rule = QueryRuleParser.parse("余华 活着")

        assertEquals("余华 活着", rule.cleanQuery)
        assertEquals(listOf("余华", "活着"), rule.requiredTitleTerms)
        assertTrue(rule.excludedTitleTerms.isEmpty())
        assertFalse(rule.hasModifiers)
        assertFalse(rule.strictTitle)
    }

    @Test
    fun handlesFullWidthPrefixes() {
        val rule = QueryRuleParser.parse("摄影 －入门 ＃ai ＠影视飓风")

        assertEquals(listOf("入门"), rule.excludedTitleTerms)
        assertEquals(listOf("ai"), rule.requiredTags)
        assertEquals(listOf("影视飓风"), rule.allowedAuthors)
    }

    @Test
    fun ignoresBarePrefixes() {
        val rule = QueryRuleParser.parse("原神 - # @")

        assertEquals("原神", rule.cleanQuery)
        assertFalse(rule.hasModifiers)
    }

    @Test
    fun supportsMultipleExclusions() {
        val rule = QueryRuleParser.parse("Flutter -测试 -入门")

        assertEquals(listOf("测试", "入门"), rule.excludedTitleTerms)
        assertEquals("flutter", rule.cleanQuery)
    }
}
