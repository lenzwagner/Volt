package com.lenz.tennisapp.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenz.tennisapp.data.db.dao.MatchDao
import com.lenz.tennisapp.data.repository.TennisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private const val FOUR_HOURS_MS = 4 * 60 * 60 * 1000L

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val repository: TennisRepository,
    private val matchDao: MatchDao
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    init {
        viewModelScope.launch {
            try {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val lastCached = matchDao.getLatestCachedAt(today) ?: 0L
                val isStale = System.currentTimeMillis() - lastCached > FOUR_HOURS_MS

                if (isStale) {
                    Timber.d("Startup: match data stale (>${FOUR_HOURS_MS / 3600000}h), refreshing...")
                    repository.refreshMatches(LocalDate.now())
                } else {
                    Timber.d("Startup: match data fresh, skipping refresh")
                }
            } catch (e: Exception) {
                Timber.w(e, "Startup refresh failed — showing cached data")
            } finally {
                _isReady.value = true
            }
        }
    }
}
