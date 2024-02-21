package moe.emi.finite.service.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.serialization.Serializable
import moe.emi.finite.dump.jsonSerializer
import moe.emi.finite.service.api.ApiProvider
import moe.emi.finite.service.api.FetchedRates

val Context.ratesCache: DataStore<RatesCache> by dataStore("rates.json", RatesCache)

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
