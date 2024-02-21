package moe.emi.finite.service.api

import arrow.core.Either
import moe.emi.finite.service.api.impl.ExchangeRatesApi
import moe.emi.finite.service.api.impl.InforEuro

enum class ApiProvider(
	val key: Int
) {
	InforEuro(0), // 151 currencies, updates at the end of each month
	ExchangeRatesApi(1), // 168 currencies, updates at most 100 times per month
	
	;
	
	fun createClient() = when (this) {
		InforEuro -> InforEuro()
		ExchangeRatesApi -> ExchangeRatesApi()
	}
	
	interface Impl {
		val name: String
		suspend fun getRates(): Either<Failure, FetchedRates>
		fun shouldRefresh(lastRefreshed: Long): Boolean
	}

}