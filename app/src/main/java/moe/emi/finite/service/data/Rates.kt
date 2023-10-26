package moe.emi.finite.service.data

import java.util.Date

data class Rates(
	val lastUpdated: Date,
	val rates: List<Rate>
)
