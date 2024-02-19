package moe.emi.finite.service.repo

import android.util.Log
import com.slack.eithernet.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import moe.emi.finite.FiniteApp
import moe.emi.finite.di.NetworkModule
import moe.emi.finite.dump.DataStoreExt.read
import moe.emi.finite.dump.DataStoreExt.write
import moe.emi.finite.dump.Response
import moe.emi.finite.dump.Response.Companion.toResponse
import moe.emi.finite.service.api.apiClient
import moe.emi.finite.service.data.Rate
import moe.emi.finite.service.data.Rates
import moe.emi.finite.service.datastore.Keys
import moe.emi.finite.service.datastore.storeGeneral
import moe.emi.finite.service.db.RateEntity
import java.util.Date

object RatesRepo {
	
	private const val TAG = "RatesRepo"
	private val dao by lazy { FiniteApp.db.rateDao() }
	private val ratesApi by lazy { NetworkModule.getRatesApi() }
	
	
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
	
	suspend fun _refreshRates(): Response<Nothing?> =
		apiClient.getLatestRates()
			.also {
				Log.d(TAG, "$it")
				if (it is ApiResult.Failure.UnknownFailure) {
					Log.d(TAG, "", it.error)
				}
			}
			.toResponse()
			.ifSuccess { data ->
				FiniteApp.instance.storeGeneral.write(Keys.RatesLastUpdated, data.timestamp)
				dao.insertAll(
					rates = data.listRates
						.map { RateEntity(it.code, it.rate) }
						.toTypedArray()
				)
			}
			.map { null }
	
	
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
	
	
	@Deprecated("Keeping it for keepsake purposes")
	// NO LONGER USED!
	suspend fun updateRates(): Response<Rates> {
		
		// Get local rates from DB and last updated timestamp
		val rates = dao.getAll()
			.map {
				Rate(it.code, it.rate)
			}
		val lastUpdated = FiniteApp.instance.storeGeneral.read(Keys.RatesLastUpdated).first() ?: 0L
		
		// Make a response from local data
		var response: Response<Rates> = Response.Success(Rates(rates = rates, lastUpdated = Date(lastUpdated * 1000L)))
		
		// If no rates in DB, get them from the api
		if (rates.isEmpty()) {
			response = apiClient.getLatestRates()
				.also { Log.d("TAG", "$it")
					
					if (it is ApiResult.Failure.UnknownFailure) {
						Log.d("Tag", "Fuck", it.error)
					}
				}
				.toResponse()
				.also { Log.d("TAG", "$it") }
				.ifSuccess { data ->
					Log.d("TAG", "ifSuccess")
					FiniteApp.instance.storeGeneral.write(Keys.RatesLastUpdated, data.timestamp)
					FiniteApp.db.rateDao().insertAll(
						*data.listRates.map { RateEntity(it.code, it.rate) }.toTypedArray()
					)
				}
				.map { data ->
					Log.d("TAG", "map")
					Rates(
						lastUpdated = Date(data.timestamp * 1000L),
						rates = data.listRates.map { Rate(it.code, it.rate) }
					)
				}
			
		}
		
		return response
	}
}