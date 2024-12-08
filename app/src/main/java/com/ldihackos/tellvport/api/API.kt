package com.ldihackos.tellvport.api

import com.ldihackos.tellvport.models.PlatformsResponse
import retrofit2.Response
import retrofit2.http.GET

interface API {
    @GET("platform")
    suspend fun getPlatforms(): Response<PlatformsResponse>
}