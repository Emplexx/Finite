package moe.emi.finite.service.api

import kotlinx.serialization.Serializable
import moe.emi.finite.service.model.Currency

@Serializable
data class Rate(
	val currency: Currency,
	val value: Double,
)