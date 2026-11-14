package io.github.xiaoancute.bilisearch

/**
 * The three user-facing strength levels. The stored number is an
 * implementation detail; the UI only presents named modes with explanations.
 */
internal enum class FilterStrength(val threshold: Int) {
    LOOSE(30),
    STANDARD(50),
    STRICT(70);

    companion object {
        fun fromThreshold(value: Int): FilterStrength = when {
            value <= 35 -> LOOSE
            value <= 65 -> STANDARD
            else -> STRICT
        }
    }
}
