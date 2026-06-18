package com.lenz.tennisapp.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Central player registry that maps API-Tennis numeric IDs to Elo/Ranking entries.
 *
 * apiKey      → numeric string from API-Tennis (e.g. "1980")
 * lastName    → normalized last name used for fuzzy matching (e.g. "zverev")
 * firstInitial→ first letter of first name (e.g. "a") for disambiguation
 * eloKey      → FK into elo_ratings.playerKey (e.g. "ta_alexander_zverev")
 * rankingKey  → FK into player_rankings.playerKey (e.g. "lt_zverev_alexander")
 */
@Entity(
    tableName = "players",
    indices = [Index("lastName"), Index("apiKey", unique = true)]
)
data class PlayerEntity(
    @PrimaryKey val apiKey: String,
    val displayName: String,        // original name from API-Tennis ("A. Zverev")
    val lastName: String,           // normalized ("zverev")
    val firstInitial: String,       // normalized ("a")
    val tour: String? = null,       // "ATP" | "WTA" | null if unknown
    val eloKey: String? = null,     // ta_alexander_zverev or null if not matched
    val rankingKey: String? = null, // lt_zverev_alexander or null if not matched
    val updatedAt: Long = System.currentTimeMillis()
)
