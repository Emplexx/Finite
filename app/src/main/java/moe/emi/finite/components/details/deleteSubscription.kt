package moe.emi.finite.components.details

import arrow.core.raise.either
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.emi.finite.core.alarms.ReminderScheduler
import moe.emi.finite.core.db.NotificationDao
import moe.emi.finite.core.db.SubscriptionDao
import moe.emi.finite.core.db.validateWrite

context(SubscriptionDao, NotificationDao, ReminderScheduler)
suspend fun deleteSubscription(id: Int) = withContext(Dispatchers.IO) {
	
	val subscriptionDao = this@SubscriptionDao
	val notificationDao = this@NotificationDao
	
	either {
		subscriptionDao.delete(id).validateWrite(1)
		cancelRemindersForSubscription(id)
		notificationDao.deleteBySubscriptionId(id)
	}
}