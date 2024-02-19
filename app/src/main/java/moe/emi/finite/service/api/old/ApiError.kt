package moe.emi.finite.service.api.old

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiError(val code: Int, val message: String)