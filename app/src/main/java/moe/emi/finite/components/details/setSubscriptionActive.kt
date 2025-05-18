package moe.emi.finite.components.details

import arrow.core.raise.either
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import moe.emi.finite.core.alarms.ReminderScheduler
import moe.emi.finite.core.db.SubscriptionDao
import moe.emi.finite.core.db.validateWrite

context(SubscriptionDao, ReminderScheduler)
suspend fun setSubscriptionActive(
	id: Int,
	isActive: Boolean
) = withContext(Dispatchers.IO) {
	either {
		
		val sub = getSubscriptionEntity(id).first() ?: return@either
		if (sub.active == isActive) return@either
		
		insertAll(sub.copy(active = isActive)).validateWrite(1)
		
		if (isActive) cancelRemindersForSubscription(id)
		else scheduleRemindersForSubscription(id)
	}
}

