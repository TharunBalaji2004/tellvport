package com.ldihackos.tellvport.models

data class AmenityData(
    val amenities: List<Amenity>,
    val createdAt: String,
    val description: String,
    val icon: String,
    val id: String,
    val name: String,
    val updatedAt: String
)