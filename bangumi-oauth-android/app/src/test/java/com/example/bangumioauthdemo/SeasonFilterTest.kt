package com.example.bangumioauthdemo

import org.junit.Assert.*
import org.junit.Test

class SeasonFilterTest {
    private fun subject(id: Int, type: Int, cat: Int, date: String) =
        AnimeCard(id = id, title = "Subject $id", date = date, category = type, subCategory = cat)

    @Test
    fun onlyAnimeInSelectedQuarterAndCategory() {
        val items = listOf(
            subject(1, 2, 1, "2026-10-01"),   // TV anime in season
            subject(2, 1, 1, "2026-10-01"),   // Book: reject
            subject(3, 4, 1, "2026-10-01"),   // Game: reject
            subject(4, 6, 1, "2026-10-01"),   // Live-action: reject
            subject(5, 2, 3, "2026-10-12"),   // Film anime: reject from TV, retain all
            subject(6, 2, 1, "2026-07-13"),   // Wrong quarter: reject
            subject(7, 2, 1, "2025-10-01"),   // Wrong year: reject
            subject(8, 2, 1, ""),             // Unknown date: reject
            subject(1, 2, 1, "2026-10-01"),   // Duplicate: reject
            subject(9, 2, 1, "2026-12-31")    // Upper inclusive boundary
        )
        assertEquals(listOf(1, 9), SeasonFilter.apply(items, 2026, 4, 1).map { it.id })
        assertEquals(listOf(1, 5, 9), SeasonFilter.apply(items, 2026, 4, null).map { it.id })
        assertEquals(listOf(5), SeasonFilter.apply(items, 2026, 4, 3).map { it.id })
    }

    @Test
    fun officialSubjectResponsesWithoutCatStayVisible() {
        val noCatTv = AnimeCard(
            id = 200, title = "TV series", category = 2,
            platform = "TV", date = "2026-10-14"
        )
        val noCatUnknownPlatform = AnimeCard(
            id = 201, title = "Unknown platform", category = 2,
            date = "2026-11-01"
        )
        val noCatMovie = AnimeCard(
            id = 202, title = "Film", category = 2,
            platform = "剧场版", date = "2026-12-01"
        )
        val noCatGame = AnimeCard(
            id = 203, title = "Game", category = 4,
            date = "2026-11-01"
        )
        val candidates = listOf(noCatTv, noCatUnknownPlatform, noCatMovie, noCatGame)
        assertEquals(listOf(200, 201), SeasonFilter.apply(candidates, 2026, 4, 1).map { it.id })
        assertEquals(listOf(200, 201, 202), SeasonFilter.apply(candidates, 2026, 4, null).map { it.id })
        // Unknown platforms have no unambiguous subtype; the server-side cat filter is authoritative.
        assertEquals(listOf(201, 202), SeasonFilter.apply(candidates, 2026, 4, 3).map { it.id })
    }

    @Test
    fun monthsAreQuarterBounded() {
        assertEquals(1..3, SeasonFilter.quarterMonths(1))
        assertEquals(4..6, SeasonFilter.quarterMonths(2))
        assertEquals(7..9, SeasonFilter.quarterMonths(3))
        assertEquals(10..12, SeasonFilter.quarterMonths(4))
    }
}
