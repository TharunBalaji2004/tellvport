package com.ldihackos.tellvport.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ldihackos.tellvport.api.API
import com.ldihackos.tellvport.models.PlatformData
import com.ldihackos.tellvport.models.PlatformsResponse
import com.ldihackos.tellvport.utils.NetworkResult
import org.json.JSONObject
import retrofit2.Response
import javax.inject.Inject

class ApiRepository @Inject constructor(private val api: API) {

    private val _platformsLiveData = MutableLiveData<NetworkResult<List<PlatformData>>>()
    val platformsLiveData: LiveData<NetworkResult<List<PlatformData>>> = _platformsLiveData

    private val _statusLiveData = MutableLiveData<NetworkResult<String>>()
    val statusLiveData: LiveData<NetworkResult<String>> = _statusLiveData

    suspend fun getPlatforms() {
        _platformsLiveData.postValue(NetworkResult.Loading())
        val response = api.getPlatforms()
        if (response.isSuccessful && response.body() != null) {
            _platformsLiveData.postValue(NetworkResult.Success(response.body()!!.data))
        } else if (response.errorBody() != null) {
            val errorObj = JSONObject(response.errorBody()!!.charStream().readText())
            _platformsLiveData.postValue(NetworkResult.Error(errorObj.getString("message")))
        } else {
            _platformsLiveData.postValue(NetworkResult.Error("Something went wrong"))
        }
    }

    private fun handleResponse(response: Response<PlatformsResponse>) {
        if (response.isSuccessful && response.body() != null) {
            _statusLiveData.postValue(NetworkResult.Success(response.body()!!.message))
        } else {
            _statusLiveData.postValue(NetworkResult.Error("Something went wrong"))
        }
    }
}