package moe.emi.finite.service.repo

import arrow.core.raise.either
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import moe.emi.finite.FiniteApp
import moe.emi.finite.di.NetworkModule
import moe.emi.finite.service.api.Failure
import moe.emi.finite.service.api.FetchedRates
import moe.emi.finite.service.datastore.RatesCache
import moe.emi.finite.service.datastore.ratesCache

object RatesRepo {
	
	private const val TAG = "RatesRepo"
	
	private val local = FiniteApp.instance.ratesCache
	private val ratesApi by lazy { NetworkModule.getRatesApi() }
	
	val fetchedRates = getLocalRates()
		.stateIn(FiniteApp.scope, SharingStarted.Eagerly, null)
	
	// Makes a request to the API, refreshes the rates in the local DB if successful
	suspend fun refreshRates() = either<Failure, Unit> {
		val data = ratesApi.getRates().bind()
		
		local.updateData {
			it.updated(NetworkModule.selectedApi, data)
		}
	}
	
	suspend fun shouldRefreshRates(): Boolean {
		
		val actualRates = getLocalRates().first() ?: return true
		if (actualRates.rates.isEmpty()) return true
		
		return ratesApi.shouldRefresh(actualRates.timestamp)
	}
	
	suspend fun clearRates() {
		local.updateData { RatesCache(emptyMap()) }
	}
	
	
	
	private fun getLocalRates(): Flow<FetchedRates?> {
		return local.data.map { it[NetworkModule.selectedApi] }
	}
}