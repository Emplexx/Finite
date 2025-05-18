package moe.emi.finite.components.editor

import arrow.core.raise.either
import arrow.core.raise.ensure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.emi.finite.core.alarms.ReminderScheduler
import moe.emi.finite.core.db.SubscriptionDao
import moe.emi.finite.core.db.SubscriptionEntity
import moe.emi.finite.core.model.Subscription

context(SubscriptionDao, ReminderScheduler, DraftStore)
suspend fun saveSubscription(subscription: Subscription) = withContext(Dispatchers.IO) {
	either {
		
		val dao = this@SubscriptionDao
		val draft = this@DraftStore
		val scheduler = this@ReminderScheduler
		
		val savedId = dao
			.insertAll(SubscriptionEntity(subscription))
			.also { ensure(it.size == 1) { "Subscription could not be saved" } }
			.first()
		
		when (subscription.isDraft) {
			true -> draft.clear()
			false -> {
				scheduler.cancelRemindersForSubscription(subscription.id)
				scheduler.scheduleRemindersForSubscription(subscription.id)
			}
		}
		
		savedId
	}
}