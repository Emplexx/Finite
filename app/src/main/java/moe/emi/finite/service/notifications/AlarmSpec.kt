package moe.emi.finite.service.notifications

import moe.emi.finite.service.model.BillingPeriod
import moe.emi.finite.service.model.FullDate
import moe.emi.finite.service.model.Reminder

data class AlarmSpec(
	val reminder: Reminder,
	val subStartedOn: FullDate,
	val subBillingPeriod: BillingPeriod,
)