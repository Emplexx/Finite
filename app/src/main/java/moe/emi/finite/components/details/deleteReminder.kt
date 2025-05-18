package moe.emi.finite.components.details

import arrow.core.raise.either
import moe.emi.finite.core.alarms.ReminderScheduler
import moe.emi.finite.core.db.NotificationDao
import moe.emi.finite.core.db.deleteReminder
import moe.emi.finite.core.db.validateWrite

context(NotificationDao, ReminderScheduler)
suspend fun deleteReminder(id: Int) = either {
	this@NotificationDao.deleteReminder(id).validateWrite(1)
	cancelReminders(id)
}