package moe.emi.finite.components.details

import arrow.core.raise.either
import moe.emi.finite.core.alarms.ReminderScheduler
import moe.emi.finite.core.db.NotificationDao
import moe.emi.finite.core.db.insertAll
import moe.emi.finite.core.db.validateWrite
import moe.emi.finite.core.model.Reminder

context(NotificationDao, ReminderScheduler)
suspend fun saveReminder(reminder: Reminder, isParentActive: Boolean) = either {
	val reminderId = insertAll(reminder).validateWrite(1).first().toInt()
	if (isParentActive) scheduleReminders(reminderId)
}