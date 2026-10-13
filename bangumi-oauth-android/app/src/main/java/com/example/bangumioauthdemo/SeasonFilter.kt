package com.example.bangumioauthdemo

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

/**
 * Defense in depth against accidental non-anime and off-season results.
 * The Bangumi `/v0/subjects` endpoint supports type, cat, year and month, but
 * search/cache snapshots can contain unexpected entries.
 *
 * Anime type = 2; anime cat 1=TV, 2=OVA, 3=Movie, 5=WEB, 0=Other.
 * Null category = all animation formats (not all subject types).
 */
object SeasonFilter {
    const val ANIME = 2
    val categoryOptions = listOf(
        null to "全部动画",
        1 to "TV",
        2 to "OVA",
        3 to "剧场版",
        5 to "WEB",
        0 to "其他"
    )

    fun quarterMonths(quarter: Int): IntRange {
        require(quarter in 1..4)
        return ((quarter - 1) * 3 + 1)..(quarter * 3)
    }

    /** Only classify known, unambiguous anime platform labels. */
    fun inferredCategory(platform: String): Int? {
        val key = platform.trim().lowercase()
        if (key.isBlank()) return null
        return when {
            key == "tv" || key.startsWith("tv ") || key.startsWith("tv(") ||
                key.startsWith("テレビ") -> 1
            key == "ova" || key == "oad" || key.startsWith("ova ") ||
                key.startsWith("oad ") -> 2
            key == "movie" || key == "映画" || key.contains("剧场版") ||
                key.contains("劇場版") -> 3
            key == "web" || key == "ona" || key.startsWith("web ") ||
                key.startsWith("ona ") -> 5
            else -> null
        }
    }

    fun accepted(
        subject: AnimeCard,
        year: Int,
        quarter: Int,
        animeCategory: Int?
    ): Boolean {
        if (subject.id <= 0 || subject.category != ANIME) return false
        // Official /v0/subjects filters by `cat` but its Subject schema does not expose cat.
        // Validate a returned `cat` if provided; otherwise cross-check recognized platforms.
        if (animeCategory != null) {
            val responseCat = subject.subCategory ?: inferredCategory(subject.platform)
            if (responseCat != null && responseCat != animeCategory) return false
        }
        val parsed = try {
            LocalDate.parse(subject.date.take(10))
        } catch (_: DateTimeParseException) {
            return false
        } catch (_: IndexOutOfBoundsException) {
            return false
        }
        return parsed.year == year && parsed.monthValue in quarterMonths(quarter)
    }

    fun apply(
        entries: List<AnimeCard>,
        year: Int,
        quarter: Int,
        animeCategory: Int?
    ): List<AnimeCard> =
        entries.asSequence()
            .filter { accepted(it, year, quarter, animeCategory) }
            .distinctBy { it.id }
            .sortedBy { it.date }
            .toList()
}
