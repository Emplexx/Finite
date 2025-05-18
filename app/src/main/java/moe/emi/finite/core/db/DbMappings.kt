package moe.emi.finite.core.db

import moe.emi.finite.core.model.Period
import moe.emi.finite.core.model.Reminder
import moe.emi.finite.core.model.SimpleDate
import moe.emi.finite.core.model.Subscription

fun NotificationEntity.toReminder(): Reminder =
	Reminder(id, subscriptionId, run {
		if (unitsInAdvance == null || timespanInAdvance == null) null
		else Period(unitsInAdvance, timespanInAdvance)
	}, hours, minutes)

fun Reminder.toEntity(): NotificationEntity =
	NotificationEntity(
		id,
		subscriptionId,
		remindInAdvance?.length,
		remindInAdvance?.unit,
		hours,
		minutes
	)

fun SubscriptionEntity.toSubscription() =
	Subscription(
		id,
		name,
		description,
		color,
		amount,
		currency,
		startedOn?.let { SimpleDate.fromUtcMilliseconds(it) },
		Period(periodAmount, periodTimespan),
		paymentMethod,
		notes,
		active
	)