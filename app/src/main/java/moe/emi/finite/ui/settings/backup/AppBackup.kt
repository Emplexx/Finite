package moe.emi.finite.ui.settings.backup

import androidx.annotation.IntRange
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.emi.finite.service.model.Currency
import moe.emi.finite.service.model.FullDate
import moe.emi.finite.service.model.Reminder
import moe.emi.finite.service.model.Subscription
import moe.emi.finite.service.model.Timespan

@Serializable
data class AppBackup(
	
	@SerialName("appVersion")
	val appVersion: Int,
	
	@SerialName("createdAt")
	val createdAt: Long,
	
	@SerialName("subscriptions")
	val subscriptions: List<SubscriptionBackup> = emptyList(),
	
	@SerialName("reminders")
	val reminders: List<ReminderBackup> = emptyList()
	
	// TODO backup settings?
	
) {
	fun serialize(): String = jsonBackup.encodeToString(serializer(), this)
}

@Serializable
data class SubscriptionBackup(
	@SerialName("id")
	val id: Int = 0,
	
	@SerialName("name")
	val name: String = "",
	
	@SerialName("description")
	val description: String = "",
	
	@SerialName("color")
	val color: Int? = null,
	
	@SerialName("price")
	val price: Double = 0.0,
	
	@SerialName("currency")
	val currency: Currency,
	
	@SerialName("startedOn")
	val startedOn: FullDate? = null,
	
	@SerialName("period")
	val period: Period = Period(1, Timespan.Month),
	
	@SerialName("paymentMethod")
	val paymentMethod: String = "",
	
	@SerialName("notes")
	val notes: String = "",
	
	@SerialName("active")
	val active: Boolean = true,
) {
	
	companion object {
		fun from(it: Subscription) = it.run {
			SubscriptionBackup(
				id,
				name,
				description,
				color,
				price,
				currency,
				startedOn,
				period.toPeriod(),
				paymentMethod, notes, active,
			)
		}
	}
	
}

@Serializable
data class ReminderBackup(
	@SerialName("id")
	val id: Int = 0,
	
	@SerialName("subscriptionId")
	val subscriptionId: Int = 0,
	
	@SerialName("period")
	val period: Period? = null,
	
	@SerialName("hours")
	@IntRange(0, 23) val hours: Int,
	
	@SerialName("minutes")
	@IntRange(0, 59) val minutes: Int,
) {
	companion object {
		fun from(it: Reminder) = it.run {
			ReminderBackup(
				id, subscriptionId, period?.toPeriod(), hours, minutes
			)
		}
	}
}

