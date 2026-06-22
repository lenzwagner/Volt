package com.lenz.tennisapp.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenz.tennisapp.data.repository.UpdateInfo
import com.lenz.tennisapp.data.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository
) : ViewModel() {

    private val _available = MutableStateFlow<UpdateInfo?>(null)
    val available: StateFlow<UpdateInfo?> = _available.asStateFlow()

    private val _downloading = MutableStateFlow(false)
    val downloading: StateFlow<Boolean> = _downloading.asStateFlow()

    private var checked = false

    fun checkOnce() {
        if (checked) return
        checked = true
        viewModelScope.launch {
            _available.value = updateRepository.checkForUpdate()
        }
    }

    fun startUpdate() {
        val info = _available.value ?: return
        _downloading.value = true
        updateRepository.downloadAndInstall(info)
    }

    fun dismiss() { _available.value = null }
}
