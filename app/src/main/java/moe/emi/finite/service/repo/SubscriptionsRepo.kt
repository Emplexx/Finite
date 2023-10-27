package moe.emi.finite.service.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import moe.emi.finite.FiniteApp
import moe.emi.finite.service.data.Rate.Companion.get
import moe.emi.finite.service.data.Subscription
import moe.emi.finite.service.datastore.appSettings
import moe.emi.finite.ui.home.Sort

object SubscriptionsRepo {
	
	private val dao by lazy { FiniteApp.db.subscriptionDao() }
	
	suspend fun pauseSubscription(id: Int, pause: Boolean) {
		dao.getObservable(id).first()?.let {
			dao.insertAll(it.copy(active = !pause))
		}
	}
	
	fun getSubscriptions(): Flow<List<Subscription>> =
		combine(
			dao.getAllObservable(),
			RatesRepo.getLocalRates(),
			FiniteApp.instance.appSettings
		) { list, rates, settings ->
			list.map { Subscription(it) }
				.sortedWith(
					when (settings.sort) {
						Sort.Date -> Subscription.dateComparator
						Sort.Alphabetical -> Subscription.alphabeticalComparator
						Sort.Price -> Subscription.priceComparator(
							from = { rates.get(it)!! },
							to = rates.get(settings.preferredCurrency)!!
						)
					}
				)
				.let { if (settings.sortIsAscending) it else it.reversed() }
		}
	
	fun getSubscription(id: Int): Flow<Subscription?> =
		dao
			.getObservable(id)
			.map { it?.let { Subscription(it) } }
	
	suspend fun deleteSubscription(id: Int): Result<Nothing?> =
		dao
			.delete(id)
			.let {
				when (it) {
					0 -> Result.failure(Error())
					1 -> Result.success(null)
					else -> Result.failure(IllegalStateException())
				}
			}
}