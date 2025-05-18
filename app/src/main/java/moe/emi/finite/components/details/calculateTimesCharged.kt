package moe.emi.finite.components.details

import moe.emi.finite.core.model.Period

fun calculateTimesCharged(
	startedOn: java.time.LocalDate,
	period: Period
): Long =
	period.unit.toChronoUnit().between(startedOn, java.time.LocalDate.now())
		.div(period.length)
		.plus(1)