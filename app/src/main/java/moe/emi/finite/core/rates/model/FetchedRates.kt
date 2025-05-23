package moe.emi.finite.core.rates.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.emi.finite.core.model.Currency

@Serializable
data class FetchedRates(
	val base: Currency,
	@SerialName("timestamp")
	val refreshedAt: Long,
	val rates: List<Rate>
) {
	
	fun convert(price: Double, from: Currency, to: Currency): Double? {
		
		val rateFrom = rates[from] ?: return null
		val rateTo = rates[to] ?: return null
		
		return convert(price, rateFrom, rateTo)
	}
	
	private operator fun List<Rate>.get(currency: Currency) =
		this.find { it.currency == currency }
	
	private fun convert(price: Double, from: Rate, to: Rate): Double =
		price / from.value * to.value

	companion object {
		fun empty() = FetchedRates(Currency.USD, 0, emptyList())
	}
	
}