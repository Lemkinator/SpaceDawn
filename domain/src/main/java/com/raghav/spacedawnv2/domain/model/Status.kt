package com.raghav.spacedawnv2.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Status(
    val abbrev: String?,
    val description: String?,
    val id: Int?,
    val name: String?
)
