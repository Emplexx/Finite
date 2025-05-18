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
import me.msoul.datastore.EnumPreference
import me.msoul.datastore.key
import moe.emi.finite.dump.systemTimeMillis
import moe.emi.finite.core.rates.api.ApiProvider
import moe.emi.finite.core.rates.api.Deserialization
import moe.emi.finite.core.rates.api.Failure
import moe.emi.finite.core.rates.model.FetchedRates
import moe.emi.finite.core.rates.model.Rate
import moe.emi.finite.core.rates.api.Unknown
import moe.emi.finite.core.rates.api.decode
import moe.emi.finite.core.rates.api.fuelGet
import moe.emi.finite.core.model.Currency
import moe.emi.finite.core.rates.api.ApiProvider.Companion.jsonApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class OpenExchangeRates(
	private val options: Options,
) : ApiProvider.Impl {

	override val type: ApiProvider = ApiProvider.OpenExchangeRates// "Open Exchange Rates"
	val name = type.providerName
	
	private val baseUrl = "https://openexchangerates.org/api/"
	private val baseCurrency = Currency.USD
	
	override suspend fun getRates(): Either<Failure, FetchedRates> = either {
		
		val appId = options.getAppId() ?: raise(Errors.InvalidAppID)
		val response = fuelGet("$baseUrl/latest.json?app_id=$appId").bind()
		
		if (response.statusCode != 200) jsonApi
			.decode(ErrorBody.serializer(), response.body)
			.also { raiseCode(it.status) }
		
		val body = jsonApi.decode(Output.serializer(), response.body)
		
		FetchedRates(baseCurrency, body.timestamp, body.listRates.bind())
			.also {
				Log.i(name, "<-- 200 | Got rates for ${it.rates.size} currencies from $name")
				Log.i(name, "$it")
			}
	}
	
	override suspend fun shouldRefresh(lastRefreshed: Long): Boolean {
		val frequency = options.getUpdateFrequency().duration.inWholeMilliseconds
		return systemTimeMillis - lastRefreshed > frequency
	}
	
	interface Options {
		suspend fun getAppId(): String?
		suspend fun getUpdateFrequency(): UpdateFrequency
	}
	
	enum class UpdateFrequency(val duration: Duration) : EnumPreference by key("updateFrequency") {
		Hourly(1.hours),
		Daily(1.days) { override val isDefault = true },
		Weekly(7.days),
	}
	
	
	@Serializable
	private data class ErrorBody(
		val error: Boolean,
		val status: Int,
		val message: String,
		val description: String,
	)
	
	@Serializable
	private data class Output(
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
		
		enum class Errors(val code: Int) : Failure {
			NotFound(404),
			InvalidAppID(401),
			NotAllowed(429),
			AccessRestricted(403),
			InvalidBase(400)
		}
		
		context (Raise<Failure>)
		@RaiseDSL
		private fun raiseCode(code: Int): Nothing {
			
			val error = Errors.entries
				.find { it.code == code }
				?: Unknown.also {
					Log.e(
						"Rates provider",
						"Rate provider OpenExchangeRates returned unknown error code: $code"
					)
				}
			
			raise(error)
		}
	}
}