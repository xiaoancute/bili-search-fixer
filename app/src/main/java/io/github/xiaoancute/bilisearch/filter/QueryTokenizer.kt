package io.github.xiaoancute.bilisearch.filter

import java.text.Normalizer

/**
 * Chinese has no spaces, so a query is expanded into the full normalized
 * string plus adjacent character bigrams. Latin text is split on word
 * boundaries. This is deliberately dependency-free and cheap enough to run
 * on every result page.
 */
object QueryTokenizer {

    private val CJK_RUN = Regex("[\\u3400-\\u4dbf\\u4e00-\\u9fff\\uf900-\\ufaff]+")
    private val LATIN_WORD = Regex("[a-z0-9]+(?:[._+-][a-z0-9]+)*")
    private val NOISE = Regex("[\\s\\p{P}\\p{S}]+")

    fun normalize(raw: String): String =
        Normalizer.normalize(raw, Normalizer.Form.NFKC)
            .lowercase()
            .replace(NOISE, "")

    fun tokens(raw: String): List<String> {
        val normalized = Normalizer.normalize(raw, Normalizer.Form.NFKC).lowercase()
        val result = LinkedHashSet<String>()

        val full = normalize(raw)
        if (full.isNotEmpty()) result += full

        CJK_RUN.findAll(normalized).forEach { match ->
            val run = match.value
            if (run.length == 1) {
                result += run
            } else {
                for (index in 0 until run.length - 1) {
                    result += run.substring(index, index + 2)
                }
            }
        }

        LATIN_WORD.findAll(normalized).forEach { match ->
            if (match.value.length >= 2) result += match.value
        }

        return result.toList()
    }
}
