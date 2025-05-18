package moe.emi.finite.core.rates.api.impl

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
import moe.emi.finite.core.model.Currency
import moe.emi.finite.core.rates.api.ApiProvider
import moe.emi.finite.core.rates.api.ApiProvider.Companion.jsonApi
import moe.emi.finite.core.rates.api.Deserialization
import moe.emi.finite.core.rates.api.Failure
import moe.emi.finite.core.rates.api.Unknown
import moe.emi.finite.core.rates.api.decode
import moe.emi.finite.core.rates.api.fuelGet
import moe.emi.finite.core.rates.model.FetchedRates
import moe.emi.finite.core.rates.model.Rate
import kotlin.time.Duration.Companion.days

@Deprecated("")
class ExchangeRatesApi : ApiProvider.Impl {
	
	override val type = ApiProvider.ExchangeRatesApi
	val name = type.providerName
	
	private val baseUrl = "http://api.exchangeratesapi.io/v1/"
	private val baseCurrency = Currency.EUR
	private val accessKey = BuildConfig.API_KEY
	
	
	override suspend fun getRates(): Either<Failure, FetchedRates> = either {
		
		val timestamp = systemTimeMillis
		val response = fuelGet("$baseUrl/latest?access_key=$accessKey").bind()
		
		if (response.statusCode != 200) jsonApi
			.decode(ErrorBody.serializer(), response.body)
			.also { raiseCode(it.error.code) }
		
		val body = jsonApi.decode(Output.serializer(), response.body)
		val rates = body.listRates.bind()
		
		FetchedRates(baseCurrency, timestamp, rates)
			.also {
				Log.i(name, "<-- 200 | Got rates for ${it.rates.size} currencies from $name")
				Log.i(name, "$it")
			}
	}
	
	override suspend fun shouldRefresh(lastRefreshed: Long): Boolean {
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