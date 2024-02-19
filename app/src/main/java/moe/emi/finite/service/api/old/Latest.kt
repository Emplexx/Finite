package moe.emi.finite.service.api.old

import com.squareup.moshi.JsonClass
import java.util.AbstractMap

// http://api.exchangeratesapi.io/v1/latest
object Latest {
	
	@JsonClass(generateAdapter = true)
	data class Output(
		val base: String,
		val timestamp: Long,
		private val rates: Any?
	) {
		val listRates: List<Rate>
			get() = kotlin.runCatching {
				rates
					.let { it as AbstractMap<*, *> }
					.map {
						Rate(it.key.toString(), it.value as Double)
					}
			}.getOrDefault(emptyList())
	}
	
	data class Rate(
		val code: String,
		val rate: Double,
	)
	
}

