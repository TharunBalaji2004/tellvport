package com.ldihackos.tellvport.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ldihackos.tellvport.repository.ApiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApiViewModel @Inject constructor(private val apiRepository: ApiRepository): ViewModel() {

    val platformsLiveData = apiRepository.platformsLiveData
    val statusLiveData = apiRepository.statusLiveData

    private fun getPlatforms() = viewModelScope.launch {
        apiRepository.getPlatforms()
    }

    init {
        getPlatforms()
    }
}