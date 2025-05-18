package moe.emi.finite.core.rates.model

import kotlinx.serialization.Serializable
import moe.emi.finite.core.model.Currency

/**
 * @param value The ratio of this currency's value to the [FetchedRates.base] currency in [FetchedRates]
 */
@Serializable
data class Rate(
	val currency: Currency,
	val value: Double,
)