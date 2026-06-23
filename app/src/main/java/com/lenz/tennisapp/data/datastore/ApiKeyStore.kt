package com.lenz.tennisapp.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.lenz.tennisapp.BuildConfig
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
        val KEY_TENNIS_EXPIRED = booleanPreferencesKey("tennis_key_expired")
        val KEY_ODDSBLAZ = stringPreferencesKey("oddsblaz_key")
        val KEY_PREDICTIONS_JSON = stringPreferencesKey("predictions_json")
        val KEY_PREDICTIONS_DATE = stringPreferencesKey("predictions_date")

        const val DEFAULT_TENNIS_KEY = "f0f0e5e1da68afd8f29e7bdc62bdf556de4a23e67d48813d91f55f85b14c4987"
    }

    val tennisApiKey: Flow<String> = context.dataStore.data
        .map { it[KEY_TENNIS_API] ?: DEFAULT_TENNIS_KEY }

    val tennisKeyExpired: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_TENNIS_EXPIRED] ?: false }

    val oddsBlazKey: Flow<String> = context.dataStore.data
        .map { it[KEY_ODDSBLAZ] ?: BuildConfig.ODDSBLAZ_KEY }

    suspend fun setTennisKey(key: String) = context.dataStore.edit {
        it[KEY_TENNIS_API] = key
        it[KEY_TENNIS_EXPIRED] = false
    }

    suspend fun setTennisKeyExpired(expired: Boolean) = context.dataStore.edit {
        it[KEY_TENNIS_EXPIRED] = expired
    }

    suspend fun setOddsBlazKey(key: String) = context.dataStore.edit {
        it[KEY_ODDSBLAZ] = key
    }

    val predictionsJson: Flow<String> = context.dataStore.data.map { it[KEY_PREDICTIONS_JSON] ?: "" }
    val predictionsDate: Flow<String> = context.dataStore.data.map { it[KEY_PREDICTIONS_DATE] ?: "" }

    suspend fun savePredictions(json: String, date: String) = context.dataStore.edit {
        it[KEY_PREDICTIONS_JSON] = json
        it[KEY_PREDICTIONS_DATE] = date
    }
}
