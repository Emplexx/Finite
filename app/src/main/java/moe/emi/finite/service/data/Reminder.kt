package moe.emi.finite.service.data

import androidx.annotation.IntRange
import kotlinx.serialization.Serializable
import moe.emi.finite.JavaSerializable
import moe.emi.finite.service.db.NotificationEntity
import moe.emi.finite.ui.details.NotificationPeriod

@Serializable
data class Reminder(
	val id: Int = 0,
	val subscriptionId: Int = 0,
	val period: NotificationPeriod?,
	@IntRange(0, 23) val hours: Int,
	@IntRange(0, 59) val minutes: Int,
) : JavaSerializable {
	
	constructor(n: NotificationEntity): this(
		n.id, n.subscriptionId, n.period, n.hours, n.minutes
	)
	
}