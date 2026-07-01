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
        val KEY_ODDS_API = stringPreferencesKey("odds_api_key")
        val KEY_ODDS_QUOTA_REMAINING = intPreferencesKey("odds_api_quota_remaining")
        val KEY_ODDS_LAST_SYNC_DATE = stringPreferencesKey("odds_last_sync_date")
        val KEY_PREDICTIONS_JSON = stringPreferencesKey("predictions_json")
        val KEY_PREDICTIONS_DATE = stringPreferencesKey("predictions_date")
        val KEY_SHOW_TAB_GRADIENT = booleanPreferencesKey("show_tab_gradient")
        
        val KEY_BG_GRADIENT_HEIGHT = floatPreferencesKey("bg_gradient_height")
        val KEY_BG_GRADIENT_COLOR = longPreferencesKey("bg_gradient_color")
        val KEY_BG_GRADIENT_DYNAMIC = booleanPreferencesKey("bg_gradient_dynamic")
        val KEY_BANNER_ALPHA = floatPreferencesKey("banner_alpha")
        val KEY_TAB_BAR_COLOR = longPreferencesKey("tab_bar_color")

        const val DEFAULT_TENNIS_KEY = "f0f0e5e1da68afd8f29e7bdc62bdf556de4a23e67d48813d91f55f85b14c4987"
    }

    val showTabGradient: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_SHOW_TAB_GRADIENT] ?: true }

    suspend fun setShowTabGradient(show: Boolean) = context.dataStore.edit {
        it[KEY_SHOW_TAB_GRADIENT] = show
    }

    val bgGradientHeight: Flow<Float> = context.dataStore.data
        .map { it[KEY_BG_GRADIENT_HEIGHT] ?: 1.0f }

    val bgGradientColor: Flow<Long> = context.dataStore.data
        .map { it[KEY_BG_GRADIENT_COLOR] ?: 0xFFBBDEFB.toLong() }

    val bgGradientDynamic: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_BG_GRADIENT_DYNAMIC] ?: false }

    val bannerAlpha: Flow<Float> = context.dataStore.data
        .map { it[KEY_BANNER_ALPHA] ?: 0.65f }

    suspend fun setBgGradientSettings(height: Float, color: Long, dynamic: Boolean) = context.dataStore.edit {
        it[KEY_BG_GRADIENT_HEIGHT] = height
        it[KEY_BG_GRADIENT_COLOR] = color
        it[KEY_BG_GRADIENT_DYNAMIC] = dynamic
    }

    suspend fun setBannerAlpha(alpha: Float) = context.dataStore.edit {
        it[KEY_BANNER_ALPHA] = alpha
    }

    val tabBarColor: Flow<Long> = context.dataStore.data
        .map { it[KEY_TAB_BAR_COLOR] ?: 0xFF918EF4L }

    suspend fun setTabBarColor(color: Long) = context.dataStore.edit {
        it[KEY_TAB_BAR_COLOR] = color
    }

    val tennisApiKey: Flow<String> = context.dataStore.data
        .map { it[KEY_TENNIS_API] ?: DEFAULT_TENNIS_KEY }

    val tennisKeyExpired: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_TENNIS_EXPIRED] ?: false }

    val oddsBlazKey: Flow<String> = context.dataStore.data
        .map { it[KEY_ODDSBLAZ] ?: BuildConfig.ODDSBLAZ_KEY }

    val oddsApiKey: Flow<String> = context.dataStore.data
        .map { it[KEY_ODDS_API] ?: BuildConfig.ODDS_API_KEY }

    val oddsQuotaRemaining: Flow<Int?> = context.dataStore.data
        .map { prefs -> prefs[KEY_ODDS_QUOTA_REMAINING] }

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

    suspend fun setOddsApiKey(key: String) = context.dataStore.edit {
        it[KEY_ODDS_API] = key
    }

    suspend fun setOddsQuotaRemaining(remaining: Int) = context.dataStore.edit {
        it[KEY_ODDS_QUOTA_REMAINING] = remaining
    }

    val oddsLastSyncDate: Flow<String> = context.dataStore.data.map { it[KEY_ODDS_LAST_SYNC_DATE] ?: "" }

    suspend fun setOddsLastSyncDate(date: String) = context.dataStore.edit {
        it[KEY_ODDS_LAST_SYNC_DATE] = date
    }

    val predictionsJson: Flow<String> = context.dataStore.data.map { it[KEY_PREDICTIONS_JSON] ?: "" }
    val predictionsDate: Flow<String> = context.dataStore.data.map { it[KEY_PREDICTIONS_DATE] ?: "" }

    suspend fun savePredictions(json: String, date: String) = context.dataStore.edit {
        it[KEY_PREDICTIONS_JSON] = json
        it[KEY_PREDICTIONS_DATE] = date
    }
}
