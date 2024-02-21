package moe.emi.finite.service.api.impl

import android.util.Log
import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.RaiseDSL
import arrow.core.raise.either
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import moe.emi.finite.dump.systemTimeMillis
import moe.emi.finite.jsonApi
import moe.emi.finite.service.api.ApiProvider
import moe.emi.finite.service.api.ExchangeRates
import moe.emi.finite.service.api.Failure
import moe.emi.finite.service.api.Rate
import moe.emi.finite.service.api.Unknown
import moe.emi.finite.service.api.decode
import moe.emi.finite.service.api.fuelGet
import moe.emi.finite.service.data.Currency
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class InforEuro : ApiProvider.Impl {
	
	private val baseUrl = "https://ec.europa.eu/budg/inforeuro/api/public"
	private val baseCurrency = Currency.EUR
	
	
	override val name: String = "InforEuro"
	
	override suspend fun getRates(): Either<Failure, ExchangeRates> = either {
		
		val timestamp = systemTimeMillis
		val response = fuelGet("$baseUrl/monthly-rates").bind()
		
		if (response.statusCode != 200) jsonApi
			.decode(ErrorBody.serializer(), response.body)
			.also { raiseCode(it.code) }
		
		val body = jsonApi.decode(ListSerializer(SuccessObject.serializer()), response.body)
			.mapNotNull { (code, value) ->
				Currency.ofIsoA3Code(code)?.let {
					Rate(it, value)
				}
			}
		
		ExchangeRates(baseCurrency, timestamp, body)
			.also { Log.i(name, "<-- 200 | $it") }
	}
	
	override fun shouldRefresh(lastRefreshed: Long): Boolean {
		val last = Instant.ofEpochMilli(lastRefreshed).atZone(ZoneId.systemDefault()).toLocalDate()
		val now = LocalDate.now()
		
		// InforEuro rates update at the end of every month, so we just need
		// to check if the current month is different from the last. Since 'now'
		// is always guaranteed to be more recent than the last update,
		// a simple equality check is enough.
		return last.monthValue != now.monthValue || last.year != now.year
	}
	
	
	@Serializable
	private data class SuccessObject(
		val isoA3Code: String,
		val value: Double
	)
	
	@Serializable
	private data class ErrorBody(
		val code: Int,
		val message: String,
	)
	
	companion object {
		private data object InvalidLanguageChoice : Failure
		private data object NoRatesForPeriod : Failure
		
		context (Raise<Failure>)
		@RaiseDSL
		private fun raiseCode(code: Int): Nothing {
			raise(
				when (code) {
					2 -> InvalidLanguageChoice
					5 -> NoRatesForPeriod
					else -> Unknown
				}
			)
		}
	}
	
}