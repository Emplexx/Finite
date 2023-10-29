package moe.emi.finite.service.notifications

import moe.emi.finite.service.data.BillingPeriod
import moe.emi.finite.service.data.FullDate
import moe.emi.finite.service.data.Reminder

data class AlarmSpec(
	val reminder: Reminder,
	val subStartedOn: FullDate,
	val subBillingPeriod: BillingPeriod,
)