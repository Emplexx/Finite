package moe.emi.finite.service.api

import arrow.core.Either
import moe.emi.finite.service.api.impl.ExchangeRatesApi
import moe.emi.finite.service.api.impl.InforEuro

enum class ApiProvider(
	val key: Int
) {
	InforEuro(0),
	ExchangeRatesApi(1);
	
	fun createClient() = when (this) {
		InforEuro -> InforEuro()
		ExchangeRatesApi -> ExchangeRatesApi()
	}
	
	interface Impl {
		val name: String
		suspend fun getRates(): Either<Failure, ExchangeRates>
		fun shouldRefresh(lastRefreshed: Long): Boolean
	}

}