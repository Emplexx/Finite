package moe.emi.finite.core.rates.model

import androidx.datastore.core.Serializer
import kotlinx.serialization.Serializable
import moe.emi.finite.dump.jsonSerializer
import moe.emi.finite.core.rates.api.ApiProvider

@Serializable
data class RatesCache(
	private val map: Map<ApiProvider, FetchedRates> = emptyMap()
) : Map<ApiProvider, FetchedRates> by map {
	
	companion object : Serializer<RatesCache> by jsonSerializer(RatesCache())
	
	fun updated(key: ApiProvider, value: FetchedRates): RatesCache {
		return this.copy(
			map = map.toMutableMap().apply {
				this[key] = value
			}
		)
	}
	
}