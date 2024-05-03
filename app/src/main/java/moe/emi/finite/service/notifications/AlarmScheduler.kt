package moe.emi.finite.service.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.getSystemService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import moe.emi.finite.service.model.Reminder
import moe.emi.finite.service.model.Subscription
import moe.emi.finite.service.model.Subscription.Companion.findNextPaymentInclusive
import moe.emi.finite.service.model.Subscription.Companion.minus
import moe.emi.finite.service.repo.ReminderRepo
import java.time.Period
import java.time.ZoneId

class AlarmScheduler(
	private val context: Context
) {
	
	val alarmManager = context.getSystemService<AlarmManager>()!!
	
	/**
	 * [X] user opens the app(?)	 			-> invalidateAlarms
	 * [?] user reboots the phone 				-> invalidateAlarms
	 * [X] user pauses a subscription 			-> removeAlarms
	 * [X] user resumes a subscription 			-> scheduleAlarms
	 * [X] user creates a new reminder 			-> scheduleAlarms
	 * [ ] user receives a subscription
	 * 	   notification and notifications for
	 * 	   the next billing cycle should be
	 * 	   scheduled							-> scheduleAlarms
	 */
	
	// cancels all notifications *that exist in the db* and reschedules them
	suspend fun invalidateAllAlarms() {
		Log.e("", "AlarmScheduler Invalidate")
		withContext(Dispatchers.IO) {
			
			ReminderRepo.dao.getAll().first()
				.forEach { cancel(it.id) }

//			SubscriptionsRepo.getAllSubscriptions().first()
//				.filter { it.active }
//				.map { it.id }
//				.let {
//					NotificationRepo.dao.getBySubscriptionId(*it.toIntArray()).first()
//				}
//				.forEach { schedule(SubscriptionNotification(it)) }

//			val active = SubscriptionsRepo.getAllSubscriptions().first().filter { it.active }
//
//			NotificationRepo.dao.getBySubscriptionId(*active.map { it.id }.toIntArray())
//				.first()
//				.map { SubscriptionNotification(it) }
//				.mapNotNull map@ { notification ->
//					active
//						.find { it.id == notification.subscriptionId }
//						?.let {
//							AlarmSpec(notification, it.startedOn ?: return@map null, it.period)
//						}
//				}
//				.forEach { schedule(it) }
			
			// TODO fix this mess
			ReminderRepo.dao.getSubcriptionsWithReminders()
				.filterKeys { it.active }
				
				.flatMap { (subscription, reminders) ->
					val s = Subscription(subscription)
					reminders.mapNotNull {
						AlarmSpec(Reminder(it), s.startedOn ?: return@mapNotNull null, s.period)
					}
				}
				.forEach { schedule(it) }
		}
	}
	
	// cancels all alarms from given IDs and reschedules them
	suspend fun scheduleAlarms(vararg ids: Int) {
		withContext(Dispatchers.IO) {
			
			Log.d("TAG", "scheduleAlarms $ids")
			
			ids.forEach {
				Log.d("TAG", "scheduleAlarms cancel $it")
				cancel(it)
			}

//			NotificationRepo.dao.getById(*ids).first()
//				.forEach { schedule(SubscriptionNotification(it)) }
			
			// TODO fix this mess
			ReminderRepo.dao.getTest(*ids)
				.also {
					Log.d("TAG", "fucking hell $it")
				}
				.mapNotNull map@ { (nEnt, sEnt) ->
					val s = Subscription(sEnt)
					AlarmSpec(Reminder(nEnt), s.startedOn ?: return@map null, s.period)
				}
				.forEach {
					Log.d("TAG", "scheduleAlarms schedule $it")
					schedule(it)
				}
		}
	}
	
	suspend fun scheduleAlarmsForSubscription(id: Int) {
		ReminderRepo.getBySubscriptionId(id).first()
			.map { it.id }
			.let { scheduleAlarms(*it.toIntArray()) }
	}
	
	fun removeAlarms(vararg ids: Int) {
		ids.forEach { cancel(it) }
	}
	
	suspend fun removeAlarmsForSubscription(id: Int) {
		ReminderRepo.getBySubscriptionId(id).first()
			.map { it.id }
			.let { removeAlarms(*it.toIntArray()) }
	}
	
	
	// TODO get rid of AlarmSpec and just fucking pass an ID and time in millis normally
	
	private fun schedule(alarm: AlarmSpec) {
		
		val intent = Intent(context, AlarmReceiver::class.java).apply {
			putExtra("ID", alarm.reminder.id)
			Intent.FLAG_RECEIVER_NO_ABORT
		}
		
		val alarmTimeMillis = alarm.subStartedOn.toLocalDate()
			
			// Find next payment date since the subscription was started
			.findNextPaymentInclusive(alarm.subBillingPeriod)
			
			// Subtract a period from it if it's set, else the reminder is "same day"
			.minus(alarm.reminder.period?.toJavaPeriod() ?: Period.ZERO)
			
			.atTime(alarm.reminder.hours, alarm.reminder.minutes)
			.atZone(ZoneId.systemDefault())
			
			.let {
				if (it.toEpochSecond() * 1000L > System.currentTimeMillis()) it
				// if alarm time is in the past it's likely because it gets rescheduled on the same
				// day it went off, so we add another billing period to it
				else it.plus(alarm.subBillingPeriod.toJavaPeriod())
			}
			.toEpochSecond() * 1000L
		
		val alarmTime = alarm.subStartedOn.toLocalDate()
			.findNextPaymentInclusive(alarm.subBillingPeriod)
			.let {
				if (alarm.reminder.period == null) it
				else it.minus(alarm.reminder.period)
			}
			.atTime(alarm.reminder.hours, alarm.reminder.minutes)
			.atZone(ZoneId.systemDefault())
			.toEpochSecond() * 1000L
		
		Log.d("TAG", "alarmTime   $alarmTimeMillis")
		Log.d("TAG", "currentTime ${System.currentTimeMillis()}")
		
		if (alarmTime <= System.currentTimeMillis()) return
		
		if (alarmManager.canScheduleExactAlarms()) {
			
			val pendingIntent = PendingIntent.getBroadcast(
				context,
				alarm.reminder.id,
				intent,
				PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
			)
//			alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime2, pendingIntent)
			
			alarmManager.setExactAndAllowWhileIdle(
				AlarmManager.RTC_WAKEUP,
				alarmTimeMillis,
				pendingIntent
			)
		}
	}
	
	private fun cancel(notificationId: Int) {
		alarmManager.cancel(
			PendingIntent.getBroadcast(
				context,
				notificationId,
				Intent(context, AlarmReceiver::class.java),
				PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
			)
		)
	}
}