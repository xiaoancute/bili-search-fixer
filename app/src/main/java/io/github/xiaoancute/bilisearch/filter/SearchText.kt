package io.github.xiaoancute.bilisearch.filter

object SearchText {

    private val EM_TAG = Regex("</?em[^>]*>", RegexOption.IGNORE_CASE)

    fun strip(raw: String): String = EM_TAG.replace(raw, "")

    fun highlightCount(raw: String): Int = EM_TAG.findAll(raw).count()
}
