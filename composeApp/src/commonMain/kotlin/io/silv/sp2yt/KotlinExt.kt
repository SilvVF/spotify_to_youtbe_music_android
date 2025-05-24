package io.silv.sp2yt

import kotlin.time.Clock
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
fun epochSeconds() = Clock.System.now().epochSeconds

fun String.levenshteinDistance(other: String): Int {
    val s1 = this
    val s2 = other

    val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

    for (i in 0..s1.length) dp[i][0] = i
    for (j in 0..s2.length) dp[0][j] = j

    for (i in 1..s1.length) {
        for (j in 1..s2.length) {
            val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
            dp[i][j] = minOf(
                dp[i - 1][j] + 1,
                dp[i][j - 1] + 1,
                dp[i - 1][j - 1] + cost
            )
        }
    }

    return dp[s1.length][s2.length]
}

fun String.removeHtmlTags(): String {
    val regex = Regex("<[^>]+>")
    var result = this
    val tags = regex.findAll(this).map { it.value }
    for (tag in tags) {
        result = result.replace(tag, "")
    }
    return result
}

fun String.similarity(other: String, ignoreCase: Boolean = false): Double {
    var s1 = this
    var s2 = other
    if (ignoreCase) {
        s1 = s1.lowercase()
        s2 = s2.lowercase()
    }
    val distance = s1.levenshteinDistance(s2)
    val maxLength = maxOf(s1.length, s2.length)
    return if (maxLength == 0) 1.0 else (1.0 - distance.toDouble() / maxLength)
}