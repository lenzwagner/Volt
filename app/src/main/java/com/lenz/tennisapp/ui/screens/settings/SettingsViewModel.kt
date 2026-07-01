package com.lenz.tennisapp.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenz.tennisapp.data.api.TennisApiService
import com.lenz.tennisapp.data.datastore.ApiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class SettingsUiState(
    val tennisKey: String = "",
    val tennisKeyExpired: Boolean = false,
    val isTestingTennis: Boolean = false,
    val tennisTestResult: String? = null,
    val oddsApiKey: String = "",
    val oddsQuotaRemaining: Int? = null,
    val showTabGradient: Boolean = true,
    val bgGradientHeight: Float = 1.0f,
    val bgGradientColor: Long = 0xFFBBDEFB,
    val bgGradientDynamic: Boolean = false,
    val bannerAlpha: Float = 0.65f,
    val tabBarColor: Long = 0xFF918EF4L
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val keyStore: ApiKeyStore,
    private val tennisApi: TennisApiService
) : ViewModel() {

    private val _isTestingTennis = MutableStateFlow(false)
    private val _tennisTestResult = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        keyStore.tennisApiKey,
        keyStore.tennisKeyExpired,
        _isTestingTennis,
        _tennisTestResult,
        combine(
            keyStore.oddsApiKey, 
            keyStore.oddsQuotaRemaining, 
            keyStore.showTabGradient,
            keyStore.bgGradientHeight,
            keyStore.bgGradientColor,
            keyStore.bgGradientDynamic,
            keyStore.bannerAlpha,
            keyStore.tabBarColor
        ) { values ->
            values
        }
    ) { tennisKey, expired, testing, result, extra ->
        val oddsKey = extra[0] as String
        val quota = extra[1] as Int?
        val tabGradient = extra[2] as Boolean
        val bgHeight = extra[3] as Float
        val bgColor = extra[4] as Long
        val bgDynamic = extra[5] as Boolean
        val bAlpha = extra[6] as Float
        val tabColor = extra[7] as Long

        SettingsUiState(
            tennisKey = tennisKey,
            tennisKeyExpired = expired,
            isTestingTennis = testing,
            tennisTestResult = result,
            oddsApiKey = oddsKey,
            oddsQuotaRemaining = quota,
            showTabGradient = tabGradient,
            bgGradientHeight = bgHeight,
            bgGradientColor = bgColor,
            bgGradientDynamic = bgDynamic,
            bannerAlpha = bAlpha,
            tabBarColor = tabColor
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun saveTennisKey(key: String) {
        viewModelScope.launch {
            keyStore.setTennisKey(key.trim())
            _tennisTestResult.value = null
        }
    }

    fun saveOddsApiKey(key: String) {
        viewModelScope.launch { keyStore.setOddsApiKey(key.trim()) }
    }

    fun setShowTabGradient(show: Boolean) {
        viewModelScope.launch { keyStore.setShowTabGradient(show) }
    }

    fun updateBgGradientSettings(height: Float, color: Long, dynamic: Boolean) {
        viewModelScope.launch {
            keyStore.setBgGradientSettings(height, color, dynamic)
        }
    }

    fun setBannerAlpha(alpha: Float) {
        viewModelScope.launch {
            keyStore.setBannerAlpha(alpha)
        }
    }

    fun setTabBarColor(color: Long) {
        viewModelScope.launch {
            keyStore.setTabBarColor(color)
        }
    }

    fun testTennisKey(key: String) {
        if (key.isBlank()) {
            _tennisTestResult.value = "Fehler: Key ist leer"
            return
        }
        viewModelScope.launch {
            _isTestingTennis.value = true
            _tennisTestResult.value = "Teste..."
            try {
                val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val resp = tennisApi.getFixtures(eventType = 1, dateStart = date, dateStop = date, apiKey = key.trim())
                if (resp.success == "1") {
                    _tennisTestResult.value = "Erfolg: Key ist gültig!"
                    keyStore.setTennisKeyExpired(false)
                } else {
                    _tennisTestResult.value = "Fehler: ${resp.error ?: "Ungültiger Key"}"
                    keyStore.setTennisKeyExpired(true)
                }
            } catch (e: Exception) {
                _tennisTestResult.value = "Fehler: ${e.message ?: "Netzwerkfehler"}"
            } finally {
                _isTestingTennis.value = false
            }
        }
    }
}
