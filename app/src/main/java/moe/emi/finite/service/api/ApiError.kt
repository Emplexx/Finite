package moe.emi.finite.service.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiError(val code: Int, val message: String)