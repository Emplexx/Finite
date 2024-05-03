package moe.emi.finite.ui.settings.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.emi.finite.service.model.Timespan
import moe.emi.finite.ui.details.NotificationPeriod

@Serializable
data class Period(
	@SerialName("length")
	val length: Int,
	@SerialName("unit")
	val unit: Timespan
) {
//	init { require(length > 0) }

	fun toNotificationPeriod() = NotificationPeriod(length, unit)
	
}