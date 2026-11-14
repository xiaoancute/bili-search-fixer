package io.github.xiaoancute.bilisearch.filter

data class SearchItem(
    val title: String,
    val tag: String? = null,
    val description: String? = null,
    val author: String? = null,
    val mid: Long? = null,
    val bvid: String? = null,
    val goto: String? = null,
    val cardType: String? = null,
    val cardGoto: String? = null,
    val play: Long? = null,
    val pubdate: Long? = null,
    val highlightCount: Int = 0
) {
    fun isAd(): Boolean {
        val parts = listOfNotNull(goto, cardType, cardGoto)
            .flatMap { it.lowercase().split('_', '-', ' ') }
        return parts.any { it == "ad" || it == "ads" || it == "advert" || it == "advertisement" }
    }
}
