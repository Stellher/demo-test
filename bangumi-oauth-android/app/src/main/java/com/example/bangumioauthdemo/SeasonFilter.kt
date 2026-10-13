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

    fun accepted(
        subject: AnimeCard,
        year: Int,
        quarter: Int,
        animeCategory: Int?
    ): Boolean {
        if (subject.id <= 0 || subject.category != ANIME) return false
        if (animeCategory != null && subject.subCategory != animeCategory) return false
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
