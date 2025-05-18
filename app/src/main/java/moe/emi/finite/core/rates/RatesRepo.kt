package moe.emi.finite.core.rates

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import arrow.core.raise.either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import moe.emi.finite.core.rates.api.Failure
import moe.emi.finite.core.rates.model.RatesCache

class RatesRepo(
	remoteRates: RemoteRates,
	context: Context,
) {
	
	private val localCache = context.ratesCache
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
	
	private val remoteProvider = remoteRates.provider
		.shareIn(scope, SharingStarted.Lazily, 1)
	
	val fetchedRates = getLocalRates()
		.stateIn(scope, SharingStarted.Eagerly, null)
	
	/**
	 * Makes a request to the remote rates API, refreshes the locally stored rates if successful
	 */
	suspend fun refreshRates() = either<Failure, Unit> {
		val data = remoteProvider.first().getRates().bind()
		localCache.updateData { it.updated(remoteProvider.first().type, data) }
	}
	
	suspend fun shouldRefreshRates(): Boolean {
		
		val cachedRates = getLocalRates().first() ?: return true
		if (cachedRates.rates.isEmpty()) return true
		
		return remoteProvider.first().shouldRefresh(lastRefreshed = cachedRates.refreshedAt)
	}
	
	suspend fun clearRates() {
		localCache.updateData { RatesCache(emptyMap()) }
	}
	
	// This will need to be changed if provider choice is implemented in the future
	private fun getLocalRates() =
		localCache.data.map { it[remoteProvider.first().type] }
	
	companion object {
		private val Context.ratesCache: DataStore<RatesCache> by dataStore("rates.json", RatesCache)
	}
	
}