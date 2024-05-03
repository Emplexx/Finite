package moe.emi.finite.service.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import moe.emi.finite.FiniteApp
import moe.emi.finite.service.db.NotificationEntity
import moe.emi.finite.service.model.Reminder

object ReminderRepo {
	
	val dao by lazy { FiniteApp.db.notificationDao() }
	
	suspend fun insertAll(vararg models: Reminder): List<Long> =
		dao.insertAll(*models.map { NotificationEntity(it) }.toTypedArray())
	
	fun getBySubscriptionId(vararg ids: Int): Flow<List<Reminder>> =
		dao.getBySubscriptionId(*ids).map { list ->
			list.map { Reminder(it) }
		}
	
	suspend fun delete(id: Int): Result<Nothing?> =
		dao.delete(id)
			.let {
				when (it) {
					0 -> Result.failure(Error())
					1 -> Result.success(null)
					else -> Result.failure(IllegalStateException())
				}
			}
}