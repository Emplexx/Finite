package moe.emi.finite.service.db

import androidx.annotation.IntRange
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import moe.emi.finite.service.model.Reminder
import moe.emi.finite.ui.details.NotificationPeriod

@Entity(tableName = "notifications")
data class NotificationEntity(
	@PrimaryKey(autoGenerate = true)
	val id: Int = 0,
	val subscriptionId: Int = 0,
	
	@Embedded
	val period: NotificationPeriod? = null,
	
	@IntRange(0, 23)
	val hours: Int,
	
	@IntRange(0, 59)
	val minutes: Int,
) {
	
	constructor(n: Reminder): this(
		n.id, n.subscriptionId, n.period, n.hours, n.minutes
	)
	
}
