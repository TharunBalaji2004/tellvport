package com.ldihackos.tellvport.models

data class PlatformData(
    val platform_id: String,
    val platform_lastest_train_name: String,
    val platform_lastest_train_number: String,
    val platform_location_corianates: String,
    val platform_location_description: String,
    val platform_name: String,
    val type: String
)