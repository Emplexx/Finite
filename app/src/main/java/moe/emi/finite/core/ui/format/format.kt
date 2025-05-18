package moe.emi.finite.core.ui.format

import moe.emi.finite.R
import moe.emi.finite.core.model.Period
import moe.emi.finite.core.model.Timespan
import java.math.RoundingMode
import java.text.DecimalFormat

fun formatPrice(price: Double): String =
	DecimalFormat("0.00")
		.apply { roundingMode = RoundingMode.CEILING }
		.format(price)

val Period.formatStringId: Int? get() = when (unit) {
	Timespan.Week ->
		when (length) {
			1 -> R.string.period_weekly
			2 -> R.string.period_2weeks
			else -> null
		}
	Timespan.Month ->
		when (length) {
			1 -> R.string.period_monthly
			3 -> R.string.period_quarterly
			6 -> R.string.period_6months
			else -> null
		}
	Timespan.Year ->
		when (length) {
			1 -> R.string.period_yearly
			else -> null
		}
	else -> null
}