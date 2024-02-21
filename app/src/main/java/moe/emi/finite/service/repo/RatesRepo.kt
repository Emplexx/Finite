package moe.emi.finite.service.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import moe.emi.finite.FiniteApp
import moe.emi.finite.di.NetworkModule
import moe.emi.finite.dump.DataStoreExt.read
import moe.emi.finite.dump.DataStoreExt.write
import moe.emi.finite.dump.Response
import moe.emi.finite.service.data.Rate
import moe.emi.finite.service.datastore.Keys
import moe.emi.finite.service.datastore.storeGeneral
import moe.emi.finite.service.db.RateEntity

object RatesRepo {
	
	private const val TAG = "RatesRepo"
	private val dao by lazy { FiniteApp.db.rateDao() }
	private val ratesApi by lazy { NetworkModule.getRatesApi() }
	
	val rates = getLocalRates()
		.stateIn(FiniteApp.scope, SharingStarted.Eagerly, emptyList())
	
	// Makes a request to the API, refreshes the rates in the local DB if successful
	suspend fun refreshRates(): Response<Nothing?> =
		ratesApi.getRates()
			.onRight { data ->
				FiniteApp.instance.storeGeneral.write(Keys.RatesLastUpdated, data.timestamp)
				dao.insertAll(
					rates = data.rates
						.map { RateEntity(it.currency.iso4217Alpha, it.value) }
						.toTypedArray()
				)
			}
			.fold(
				{ Response.Failure(Exception("$it")) },
				{ Response.Success(null) }
			)
	
	
	fun getLocalRates(): Flow<List<Rate>> {
		return dao
			.getAllObservable()
			.map { list ->
				list.map { Rate(it.code, it.rate) }
			}
	}
	
	
	suspend fun shouldRefreshRates(): Boolean {
		if (getLocalRates().first().isEmpty()) return true
		return FiniteApp.instance.storeGeneral
			.read(Keys.RatesLastUpdated, 0L).first()
			.let { ratesApi.shouldRefresh(it) }
//			.let { if (!it) return@launch }
	}
}