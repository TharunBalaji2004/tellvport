package com.ldihackos.tellvport.models

data class AmenityResponse(
    val data: AmenityData,
    val message: String,
    val success: Boolean
)