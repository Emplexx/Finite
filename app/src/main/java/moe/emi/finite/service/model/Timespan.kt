package moe.emi.finite.service.model

enum class Timespan {
	Day, Week, Month, Year;
	
	/** Gets approximate count of days of this timespan */
	val approximateMultiplier: Int
		get() = when (this) {
			Day -> 1
			Week -> 7
			Month -> 30
			Year -> 12 * 30
		}
}