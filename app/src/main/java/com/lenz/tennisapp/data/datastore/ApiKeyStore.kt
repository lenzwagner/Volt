package com.lenz.tennisapp.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "api_keys")

@Singleton
class ApiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_TENNIS_API = stringPreferencesKey("tennis_api_key")
        val KEY_ODDS_API = stringPreferencesKey("odds_api_key")
        val KEY_TENNIS_EXPIRED = booleanPreferencesKey("tennis_key_expired")
        val KEY_ODDS_EXPIRED = booleanPreferencesKey("odds_key_expired")
        val KEY_ODDS_REQUESTS_REMAINING = intPreferencesKey("odds_requests_remaining")

        // Default keys (user-provided; change in Settings when expired)
        const val DEFAULT_TENNIS_KEY = "83f1ca0d81403233614126fb77fe4d1dd9f7e28c878e6d64e700e6e4a9d38202"
        const val DEFAULT_ODDS_KEY = "f521a4027f371eb92416e22b61d42064"
    }

    val tennisApiKey: Flow<String> = context.dataStore.data
        .map { it[KEY_TENNIS_API] ?: DEFAULT_TENNIS_KEY }

    val oddsApiKey: Flow<String> = context.dataStore.data
        .map { it[KEY_ODDS_API] ?: DEFAULT_ODDS_KEY }

    val tennisKeyExpired: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_TENNIS_EXPIRED] ?: false }

    val oddsKeyExpired: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_ODDS_EXPIRED] ?: false }

    val oddsRequestsRemaining: Flow<Int> = context.dataStore.data
        .map { it[KEY_ODDS_REQUESTS_REMAINING] ?: 500 }

    suspend fun setTennisKey(key: String) = context.dataStore.edit {
        it[KEY_TENNIS_API] = key
        it[KEY_TENNIS_EXPIRED] = false  // Reset expired flag on new key
    }

    suspend fun setOddsKey(key: String) = context.dataStore.edit {
        it[KEY_ODDS_API] = key
        it[KEY_ODDS_EXPIRED] = false
    }

    suspend fun setTennisKeyExpired(expired: Boolean) = context.dataStore.edit {
        it[KEY_TENNIS_EXPIRED] = expired
    }

    suspend fun setOddsKeyExpired(expired: Boolean) = context.dataStore.edit {
        it[KEY_ODDS_EXPIRED] = expired
    }

    suspend fun setOddsRequestsRemaining(remaining: Int) = context.dataStore.edit {
        it[KEY_ODDS_REQUESTS_REMAINING] = remaining
    }
}
