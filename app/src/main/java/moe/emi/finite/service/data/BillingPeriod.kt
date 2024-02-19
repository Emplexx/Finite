package moe.emi.finite.service.data

import kotlinx.serialization.Serializable
import moe.emi.finite.R
import java.time.Period
import java.io.Serializable as IOSerializable

@Serializable
@Deprecated("Use [Period] instead")
data class BillingPeriod(
	val count: Int,
	val timespan: Timespan
) : IOSerializable {
	
	val stringId: Int? =
		when (timespan) {
			Timespan.Week ->
				when (count) {
					1 -> R.string.period_weekly
					2 -> R.string.period_2weeks
					else -> null
				}
			Timespan.Month ->
				when (count) {
					1 -> R.string.period_monthly
					3 -> R.string.period_quarterly
					6 -> R.string.period_6months
					else -> null
				}
			Timespan.Year ->
				when (count) {
					1 -> R.string.period_yearly
					else -> null
				}
			else -> null
		}
	
	fun priceEveryYear(price: Double): Double =
		when (this.timespan) {
			Timespan.Year -> price / this.count
			Timespan.Month -> price * (12 / this.count)
			Timespan.Week -> price * (52.14 / this.count)
			Timespan.Day -> price * (365 / this.count)
		}
	
	fun priceEveryMonth(price: Double): Double =
		when (this.timespan) {
			Timespan.Year -> price / this.count / 12
			Timespan.Month -> price / this.count
			Timespan.Week -> price * (4.35 / this.count)
			Timespan.Day -> price * (30.42 / this.count)
		}
	
	fun priceEveryWeek(price: Double): Double =
		when (this.timespan) {
			Timespan.Year -> price / this.count / 52.14
			Timespan.Month -> price / this.count / 4.35
			Timespan.Week -> price / this.count
			Timespan.Day -> price * (7 / this.count)
		}
	
	
	fun toJavaPeriod() =
		when (timespan) {
			Timespan.Day -> Period.ofDays(count)
			Timespan.Week -> Period.ofWeeks(count)
			Timespan.Month -> Period.ofMonths(count)
			Timespan.Year -> Period.ofYears(count)
		}
	
	fun toPeriod() = moe.emi.finite.ui.settings.backup.Period(count, timespan)
	
	private val approximateLength: Int
		get() = this.timespan.approximateMultiplier * this.count
	
	operator fun compareTo(other: BillingPeriod): Int {
		return this.approximateLength - other.approximateLength
	}
}
