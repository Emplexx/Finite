package moe.emi.finite.service.data

import androidx.annotation.IntRange
import kotlinx.serialization.Serializable
import moe.emi.finite.IOSerializable
import moe.emi.finite.service.db.NotificationEntity
import moe.emi.finite.ui.details.NotificationPeriod
import moe.emi.finite.ui.settings.backup.ReminderBackup

@Serializable
data class Reminder(
	val id: Int = 0,
	val subscriptionId: Int = 0,
	
	/** Period of time to notify in advance before the subscription day. If null, the reminder is on the same day as the billing date*/
	val period: NotificationPeriod?,
	@IntRange(0, 23) val hours: Int,
	@IntRange(0, 59) val minutes: Int,
) : IOSerializable {
	
	init {
		require(hours in 0..23)
		require(minutes in 0..59)
	}
	
	constructor(n: NotificationEntity): this(
		n.id, n.subscriptionId, n.period, n.hours, n.minutes
	)
	
	companion object {
		
		fun from(it: ReminderBackup) = it.run {
			Reminder(
				id,
				subscriptionId,
				period?.let {
					NotificationPeriod(period.length, period.unit)
				},
				hours, minutes
			)
		}
	}
	
}