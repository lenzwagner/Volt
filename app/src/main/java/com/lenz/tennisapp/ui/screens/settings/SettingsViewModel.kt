package com.lenz.tennisapp.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenz.tennisapp.data.api.OddsApiService
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
    val oddsKey: String = "",
    val tennisKeyExpired: Boolean = false,
    val oddsKeyExpired: Boolean = false,
    val oddsRequestsRemaining: Int = 500,
    val isTestingTennis: Boolean = false,
    val isTestingOdds: Boolean = false,
    val tennisTestResult: String? = null,
    val oddsTestResult: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val keyStore: ApiKeyStore,
    private val tennisApi: TennisApiService,
    private val oddsApi: OddsApiService
) : ViewModel() {

    private val _isTestingTennis = MutableStateFlow(false)
    private val _isTestingOdds = MutableStateFlow(false)
    private val _tennisTestResult = MutableStateFlow<String?>(null)
    private val _oddsTestResult = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        keyStore.tennisApiKey,
        keyStore.oddsApiKey,
        keyStore.tennisKeyExpired,
        keyStore.oddsKeyExpired,
        keyStore.oddsRequestsRemaining,
        _isTestingTennis,
        _isTestingOdds,
        _tennisTestResult,
        _oddsTestResult
    ) { values ->
        SettingsUiState(
            tennisKey = values[0] as String,
            oddsKey = values[1] as String,
            tennisKeyExpired = values[2] as Boolean,
            oddsKeyExpired = values[3] as Boolean,
            oddsRequestsRemaining = values[4] as Int,
            isTestingTennis = values[5] as Boolean,
            isTestingOdds = values[6] as Boolean,
            tennisTestResult = values[7] as? String,
            oddsTestResult = values[8] as? String
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun saveTennisKey(key: String) {
        viewModelScope.launch { 
            keyStore.setTennisKey(key.trim())
            _tennisTestResult.value = null
        }
    }

    fun saveOddsKey(key: String) {
        viewModelScope.launch { 
            keyStore.setOddsKey(key.trim())
            _oddsTestResult.value = null
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
                // Simple request to verify key
                val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val resp = tennisApi.getFixtures(apiKey = key.trim(), dateStart = date, dateStop = date)
                if (resp.success == 1) {
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

    fun testOddsKey(key: String) {
        if (key.isBlank()) {
            _oddsTestResult.value = "Fehler: Key ist leer"
            return
        }
        viewModelScope.launch {
            _isTestingOdds.value = true
            _oddsTestResult.value = "Teste..."
            try {
                // Request tennis odds to verify key
                val resp = oddsApi.getOdds(sport = "tennis_atp", apiKey = key.trim())
                // If it doesn't throw an exception (401/403), it's likely working
                _oddsTestResult.value = "Erfolg: Key ist gültig!"
                keyStore.setOddsKeyExpired(false)
            } catch (e: Exception) {
                _oddsTestResult.value = "Fehler: ${e.message ?: "Ungültiger Key oder Limit erreicht"}"
                keyStore.setOddsKeyExpired(true)
            } finally {
                _isTestingOdds.value = false
            }
        }
    }
}
