package moe.emi.finite.components.settings.ui.backup

import moe.emi.finite.core.model.Period
import moe.emi.finite.core.model.Reminder
import moe.emi.finite.core.model.Subscription

fun SubscriptionBackup.toSubscription() =
	Subscription(
		id, name, description, color, price, currency, startedOn,
		Period(period.length, period.unit),
		paymentMethod, notes, active
	)

fun ReminderBackup.toReminder(): Reminder =
	Reminder(
		id,
		subscriptionId,
		period,
		hours, minutes
	)

fun Reminder.toBackup(): ReminderBackup =
	ReminderBackup(
		id,
		subscriptionId,
		remindInAdvance,
		hours, minutes
	)