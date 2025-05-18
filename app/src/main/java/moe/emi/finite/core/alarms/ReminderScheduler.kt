package moe.emi.finite.core.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import moe.emi.finite.core.db.NotificationDao
import moe.emi.finite.core.db.toReminder
import moe.emi.finite.core.db.toSubscription
import moe.emi.finite.core.findNextPaymentInclusive
import moe.emi.finite.core.model.Period
import moe.emi.finite.core.model.Reminder
import moe.emi.finite.core.model.SimpleDate
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

interface ReminderScheduler {
	
	/**
	 * Schedule reminders with [reminderIds]
	 */
	suspend fun scheduleReminders(vararg reminderIds: Int)
	
	/**
	 * Cancel reminders with [reminderIds]
	 */
	suspend fun cancelReminders(vararg reminderIds: Int)
	
	suspend fun scheduleRemindersForSubscription(subscriptionId: Int)
	suspend fun cancelRemindersForSubscription(subscriptionId: Int)
	
	/**
	 * Cancel all reminders and reschedule them
	 */
	suspend fun invalidateAllReminders()
	
	suspend fun cancelAllReminders()
	
}

fun newReminderScheduler(
	context: Context,
	dao: NotificationDao
) = object : ReminderScheduler {
	
	val alarmManager = context.getSystemService<AlarmManager>()!!
	
	override suspend fun scheduleReminders(vararg reminderIds: Int) = withContext(Dispatchers.IO) {
		
		// TODO test if this works when reminders aren't cancelled
//		reminderIds.forEach { context.cancelReminder(it) }
		
		for ((r, s) in dao.getRemindersWithSubs(*reminderIds)) {
			val (startedOn, period) = s.toSubscription().let { it.startedOn to it.period }
			scheduleReminder(r.toReminder(), startedOn ?: continue, period)
		}
	}
	
	override suspend fun cancelReminders(vararg reminderIds: Int) {
		reminderIds.forEach { context.cancelReminder(it) }
	}
	
	override suspend fun scheduleRemindersForSubscription(subscriptionId: Int) {
		dao.getEntitiesBySubscriptionId(subscriptionId).first()
			.map { it.id }
			.let { scheduleReminders(*it.toIntArray()) }
	}
	
	override suspend fun cancelRemindersForSubscription(subscriptionId: Int) {
		dao.getEntitiesBySubscriptionId(subscriptionId).first()
			.map { it.id }
			.let { cancelReminders(*it.toIntArray()) }
	}
	
	override suspend fun invalidateAllReminders() = withContext(Dispatchers.IO) {
		
		dao.getAll().first().forEach { context.cancelReminder(it.id) }
		
		for ((s, reminders) in dao.getActiveSubscriptionsWithReminders()) {
			val (startedOn, period) = s.toSubscription().let { it.startedOn to it.period }
			if (startedOn == null) continue
			
			reminders.forEach { scheduleReminder(it.toReminder(), startedOn , period) }
		}
	}
	
	override suspend fun cancelAllReminders() {
		dao.getAll().first().forEach { context.cancelReminder(it.id) }
	}
	
	/**
	 * The most important method. This schedules a single reminder based on the provided information.
	 */
	private fun scheduleReminder(
		reminder: Reminder,
		startedOn: SimpleDate,
		billingPeriod: Period,
	) {
		if (!alarmManager.canScheduleExactAlarms()) return
		
		val alarmTime = findNextReminderTime(reminder, startedOn, billingPeriod).toEpochMilli()
		
		alarmManager.setExactAndAllowWhileIdle(
			AlarmManager.RTC_WAKEUP,
			alarmTime,
			context.pendingIntentForReminder(reminder.id)
		)
	}
	
}

private fun Context.cancelReminder(reminderId: Int) {
	getSystemService<AlarmManager>()?.cancel(pendingIntentForReminder(reminderId))
}

private fun Context.pendingIntentForReminder(reminderId: Int): PendingIntent {
	
	val receiverIntent = Intent(this, ReminderAlarmReceiver::class.java).apply {
		putExtra("ID", reminderId)
	}
	
	return PendingIntent.getBroadcast(
		this,
		reminderId,
		receiverIntent,
		PendingIntent.FLAG_IMMUTABLE
	)
	// PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
}

private fun findNextReminderTime(
	reminder: Reminder,
	startedOn: SimpleDate,
	billingPeriod: Period,
): Instant = startedOn.toLocalDate()
	
	.findNextPaymentInclusive(billingPeriod)
	.let {
		if (reminder.remindInAdvance == null) it
		else it.minus(reminder.remindInAdvance.toJavaPeriod())
	}
	
	.atTime(reminder.hours, reminder.minutes)
	.atZone(ZoneId.systemDefault())
	
	.let {
		
		var reminderDateTime = it
		val now = ZonedDateTime.now()
		
		// if alarm time is in the past it's likely because it gets rescheduled on the same
		// day it went off, so we add billing periods to it until it's in the future
		while (reminderDateTime.isBefore(now)) {
			reminderDateTime = reminderDateTime.plus(billingPeriod.toJavaPeriod())
		}
		
		reminderDateTime.toInstant()
	}