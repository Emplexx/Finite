package moe.emi.finite.service.api

import arrow.core.Either

enum class ApiProvider(
	val key: Int
) {
	InforEuro(0);
	
	
	interface Impl {
		val name: String
		suspend fun getRates(): Either<Failure, ExchangeRates>
		fun shouldRefresh(lastRefreshed: Long): Boolean
	}

}