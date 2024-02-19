package moe.emi.finite.service.api

import moe.emi.finite.service.data.Currency

data class Rate(
	val currency: Currency,
	val value: Double,
)