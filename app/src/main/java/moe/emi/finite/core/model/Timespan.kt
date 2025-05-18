package moe.emi.finite.core.model

import java.time.temporal.ChronoUnit

enum class Timespan {
	Day, Week, Month, Year;
	
	/** Gets approximate count of days of this timespan */
	val approximateDays: Int
		get() = when (this) {
			Day -> 1
			Week -> 7
			Month -> 30
			Year -> 365
		}
	
	fun toChronoUnit() = when (this) {
		Day -> ChronoUnit.DAYS
		Week -> ChronoUnit.WEEKS
		Month -> ChronoUnit.MONTHS
		Year -> ChronoUnit.YEARS
	}
}