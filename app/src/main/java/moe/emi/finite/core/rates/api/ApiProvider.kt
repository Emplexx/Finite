package moe.emi.finite.core.rates.api

import arrow.core.Either
import kotlinx.serialization.json.Json
import moe.emi.finite.core.rates.api.impl.ExchangeRatesApi
import moe.emi.finite.core.rates.api.impl.InforEuro
import moe.emi.finite.core.rates.model.FetchedRates

enum class ApiProvider(
	val id: Int,
	val providerName: String
) {
	// 151 currencies, updates at the end of each month
	InforEuro(0, "InforEuro"),
	
	// https://exchangeratesapi.io/
	// 168 currencies, free plan updates at most 100 times per month, will not be in the final release
	ExchangeRatesApi(1, "exchangerates"),
	
	// https://openexchangerates.org/
	// ? currencies, free plan updates at most 1000 times per month
	OpenExchangeRates(2, "Open Exchange Rates"),
	;
	
	fun createClient() = when (this) {
		InforEuro -> InforEuro()
		ExchangeRatesApi -> ExchangeRatesApi()
		OpenExchangeRates -> TODO()
	}
	
	interface Impl {
		val type: ApiProvider
		suspend fun getRates(): Either<Failure, FetchedRates>
		suspend fun shouldRefresh(lastRefreshed: Long): Boolean
	}
	
	companion object {
		val default = InforEuro
		
		val jsonApi = Json {
			ignoreUnknownKeys = true
		}
	}

}