package moe.emi.finite.service.api

import moe.emi.finite.service.data.Currency

data class ExchangeRates(
	val base: Currency,
	val timestamp: Long,
	val rates: List<Rate>
)