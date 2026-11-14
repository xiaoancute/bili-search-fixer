package io.github.xiaoancute.bilisearch.filter

import java.text.Normalizer

object QueryRuleParser {

    private val WHITESPACE = Regex("\\s+")

    fun parse(rawQuery: String): SearchRule {
        val terms = Normalizer.normalize(rawQuery, Normalizer.Form.NFKC)
            .trim()
            .split(WHITESPACE)
            .filter { it.isNotEmpty() }

        val titleTerms = LinkedHashSet<String>()
        val excludedTerms = LinkedHashSet<String>()
        val requiredTags = LinkedHashSet<String>()
        val allowedAuthors = LinkedHashSet<String>()
        var hasModifiers = false

        terms.forEach { term ->
            when (term.first()) {
                '-' -> {
                    val value = QueryTokenizer.normalize(term.substring(1))
                    if (value.isNotEmpty()) {
                        excludedTerms += value
                        hasModifiers = true
                    }
                }
                '#' -> {
                    val value = QueryTokenizer.normalize(term.substring(1))
                    if (value.isNotEmpty()) {
                        requiredTags += value
                        hasModifiers = true
                    }
                }
                '@' -> {
                    val value = QueryTokenizer.normalize(term.substring(1))
                    if (value.isNotEmpty()) {
                        allowedAuthors += value
                        hasModifiers = true
                    }
                }
                else -> {
                    val value = QueryTokenizer.normalize(term)
                    if (value.isNotEmpty()) titleTerms += value
                }
            }
        }

        return SearchRule(
            rawQuery = rawQuery,
            cleanQuery = titleTerms.joinToString(" "),
            requiredTitleTerms = titleTerms.toList(),
            excludedTitleTerms = excludedTerms.toList(),
            requiredTags = requiredTags.toList(),
            allowedAuthors = allowedAuthors.toList(),
            hasModifiers = hasModifiers
        )
    }
}
