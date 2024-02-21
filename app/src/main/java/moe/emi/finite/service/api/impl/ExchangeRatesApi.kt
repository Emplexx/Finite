package moe.emi.finite.service.api.impl

import android.util.Log
import arrow.core.Either
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.RaiseDSL
import arrow.core.raise.either
import arrow.core.right
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonObject
import moe.emi.finite.BuildConfig
import moe.emi.finite.dump.systemTimeMillis
import moe.emi.finite.jsonApi
import moe.emi.finite.service.api.ApiProvider
import moe.emi.finite.service.api.Deserialization
import moe.emi.finite.service.api.ExchangeRates
import moe.emi.finite.service.api.Failure
import moe.emi.finite.service.api.Rate
import moe.emi.finite.service.api.Unknown
import moe.emi.finite.service.api.decode
import moe.emi.finite.service.api.fuelGet
import moe.emi.finite.service.data.Currency
import kotlin.time.Duration.Companion.days

class ExchangeRatesApi : ApiProvider.Impl {
	
	private val baseUrl = "http://api.exchangeratesapi.io/v1/"
	private val baseCurrency = Currency.EUR
	
	private val accessKey = BuildConfig.API_KEY
	
	override val name: String = "exchangerates"
	
	override suspend fun getRates(): Either<Failure, ExchangeRates> = either {
		
		val response = fuelGet("$baseUrl/latest?access_key=$accessKey").bind()
		
		if (response.statusCode != 200) jsonApi
			.decode(ErrorBody.serializer(), response.body)
			.also { raiseCode(it.error.code) }
		
		val body = jsonApi.decode(Output.serializer(), response.body)
		val rates = body.listRates.bind()
		
		ExchangeRates(baseCurrency, body.timestamp, rates)
			.also { Log.i(name, "<-- 200 | $it") }
	}
	
	override fun shouldRefresh(lastRefreshed: Long): Boolean {
		// TODO? let the user set this
		return systemTimeMillis - lastRefreshed > 1.days.inWholeMilliseconds
	}
	
	
	@Serializable
	private data class ErrorBody(
		val error: Error,
	) {
		@Serializable
		data class Error(
			val code: Int,
			val info: String,
		)
	}
	
	@Serializable
	data class Output(
		val timestamp: Long,
		private val rates: JsonObject
	) {
		val listRates get() = runCatching {
			rates
				.mapNotNull { (code, value) ->
					Currency.ofIsoA3Code(code)?.let {
						Rate(it, jsonApi.decodeFromJsonElement(Double.serializer(), value))
					}
				}
				.right()
			
		}.getOrElse { Deserialization.left() }
	}
	
	companion object {
		
		private data object NoApiKey : Failure
		private data object MonthlyRequestLimitReached : Failure
		private data object NoResults : Failure
		
		context (Raise<Failure>)
		@RaiseDSL
		private fun raiseCode(code: Int): Nothing {
			
			raise(
				when (code) {
					101 -> NoApiKey
					104 -> MonthlyRequestLimitReached
					106 -> NoResults
					else -> Unknown.also { Log.e("Rates provider", "Rate provider exchangerates returned error $code") }
				}
			)
		}
	}
	
}