package moe.emi.finite.service.repo

import android.util.Log
import com.slack.eithernet.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import moe.emi.finite.FiniteApp
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
	
	suspend fun refreshRates(): Response<Nothing?> =
		apiClient.getLatestRates()
			.also { Log.d("TAG", "$it")
				if (it is ApiResult.Failure.UnknownFailure) {
					Log.d("RatesRepository", "", it.error)
				}
			}
			.toResponse()
			.ifSuccess { data ->
				FiniteApp.instance.storeGeneral.write(Keys.RatesLastUpdated, data.timestamp)
				FiniteApp.db.rateDao().insertAll(
					*
					data.listRates
						.map { RateEntity(it.code, it.rate) }
						.toTypedArray()
				)
			}
			.map { data ->
				Rates(
					lastUpdated = Date(data.timestamp * 1000L),
					rates = data.listRates.map { Rate(it.code, it.rate) }
				)
				null
			}
	
	@Deprecated("")
	suspend fun updateRates(): Response<Rates> {
		
		// Get local rates from DB and last updated timestamp
		var rates = FiniteApp.db.rateDao().getAll()
			.map {
				Rate(it.code, it.rate)
			}
		val lastUpdated = FiniteApp.instance.storeGeneral.read(Keys.RatesLastUpdated).first() ?: 0L
		
		// Make a response from local data
		var response: Response<Rates> = Response.Success(Rates(rates = rates, lastUpdated = Date(lastUpdated * 1000L)))
		
		// If no rates in DB, get them from the api
		// TODO also add check if the lastUpdated was longer than week/month/etc ago
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
	
	fun getLocalRates(): Flow<List<Rate>> {
		return FiniteApp.db.rateDao()
			.getAllObservable()
			.map { list ->
				list.map { Rate(it.code, it.rate) }
			}
	}
}