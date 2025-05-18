package moe.emi.finite.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable
import java.time.Period as JavaPeriod

@Serializable
data class Period(
	@SerialName("length")
	val length: Int,
	@SerialName("unit")
	val unit: Timespan
) : JavaSerializable {
	
	companion object {
		val validBillingPeriods = listOf(
			Period(1, Timespan.Week),
			Period(2, Timespan.Week),
			Period(1, Timespan.Month),
			Period(3, Timespan.Month),
			Period(6, Timespan.Month),
			Period(1, Timespan.Year)
		)
	}
	
	init { require(length > 0) }

	fun toJavaPeriod(): JavaPeriod =
		when (unit) {
			Timespan.Day -> JavaPeriod.ofDays(length)
			Timespan.Week -> JavaPeriod.ofWeeks(length)
			Timespan.Month -> JavaPeriod.ofMonths(length)
			Timespan.Year -> JavaPeriod.ofYears(length)
		}
	
	operator fun compareTo(other: Period): Int =
		(length * unit.approximateDays).compareTo(other.length * other.unit.approximateDays)
}