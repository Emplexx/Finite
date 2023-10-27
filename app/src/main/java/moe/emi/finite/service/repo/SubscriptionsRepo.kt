package moe.emi.finite.service.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import moe.emi.finite.FiniteApp
import moe.emi.finite.service.data.Subscription

object SubscriptionsRepo {
	
	private val dao by lazy { FiniteApp.db.subscriptionDao() }
	
	suspend fun pauseSubscription(id: Int, pause: Boolean) {
		dao.getObservable(id).first()?.let {
			dao.insertAll(it.copy(active = !pause))
		}
	}
	
	fun getSubscriptions(): Flow<List<Subscription>> =
		dao
			.getAllObservable()
			.map { list ->
				list.map { Subscription(it) }
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