package moe.emi.finite.service.data

import moe.emi.finite.R
import java.io.Serializable

data class BillingPeriod(
	val every: Int,
	val unit: Timespan
) : Serializable {
	val stringId: Int? =
		when (unit) {
			Timespan.Week ->
				when (every) {
					1 -> R.string.period_weekly
					2 -> R.string.period_2weeks
					else -> null
				}
			Timespan.Month ->
				when (every) {
					1 -> R.string.period_monthly
					3 -> R.string.period_quarterly
					6 -> R.string.period_6months
					else -> null
				}
			Timespan.Year ->
				when (every) {
					1 -> R.string.period_yearly
					else -> null
				}
			else -> null
		}
	
	fun priceEveryYear(price: Double): Double =
		when (this.unit) {
			Timespan.Year -> price / this.every
			Timespan.Month -> price * (12 / this.every)
			Timespan.Week -> price * (52.14 / this.every)
			Timespan.Day -> price * (365 / this.every)
		}
	
	fun priceEveryMonth(price: Double): Double =
		when (this.unit) {
			Timespan.Year -> price / this.every / 12
			Timespan.Month -> price / this.every
			Timespan.Week -> price * (4.35 / this.every)
			Timespan.Day -> price * (30.42 / this.every)
		}
	
	fun priceEveryWeek(price: Double): Double =
		when (this.unit) {
			Timespan.Year -> price / this.every / 52.14
			Timespan.Month -> price / this.every / 4.35
			Timespan.Week -> price / this.every
			Timespan.Day -> price * (7 / this.every)
		}
}
