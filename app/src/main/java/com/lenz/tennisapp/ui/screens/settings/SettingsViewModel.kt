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
    val oddsBlazKey: String = ""
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
        keyStore.oddsBlazKey
    ) { tennisKey, expired, testing, result, oddsKey ->
        SettingsUiState(
            tennisKey = tennisKey,
            tennisKeyExpired = expired,
            isTestingTennis = testing,
            tennisTestResult = result,
            oddsBlazKey = oddsKey
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun saveTennisKey(key: String) {
        viewModelScope.launch {
            keyStore.setTennisKey(key.trim())
            _tennisTestResult.value = null
        }
    }

    fun saveOddsBlazKey(key: String) {
        viewModelScope.launch { keyStore.setOddsBlazKey(key.trim()) }
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
