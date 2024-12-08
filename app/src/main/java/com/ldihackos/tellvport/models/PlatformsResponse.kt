package com.ldihackos.tellvport.models

data class PlatformsResponse(
    val `data`: List<PlatformData>,
    val message: String,
    val success: Boolean
)