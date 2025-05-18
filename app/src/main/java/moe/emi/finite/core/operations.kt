package moe.emi.finite.core

import moe.emi.finite.core.model.Period
import moe.emi.finite.core.model.Timespan
import java.time.LocalDate

fun LocalDate.plus(period: Period): LocalDate {
	
	val r = when (period.unit) {
		Timespan.Day -> this.plusDays(period.length.toLong())
		Timespan.Week -> this.plusWeeks(period.length.toLong())
		Timespan.Month -> this.plusMonths(period.length.toLong())
		Timespan.Year -> this.plusYears(period.length.toLong())
	}
	return r
}

tailrec fun LocalDate.findNextPayment(period: Period): LocalDate =
	if (this > LocalDate.now()) this else (this.plus(period)).findNextPayment(period)

tailrec fun LocalDate.findNextPaymentInclusive(period: Period): LocalDate =
	if (this >= LocalDate.now()) this
	else (this.plus(period)).findNextPaymentInclusive(
		period
	)