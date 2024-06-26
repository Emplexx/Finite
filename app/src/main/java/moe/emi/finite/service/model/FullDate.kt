package moe.emi.finite.service.model

import kotlinx.serialization.SerialName
import java.io.Serializable
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

@kotlinx.serialization.Serializable
data class FullDate(
	@SerialName("year")
	val year: Int,
	
	@SerialName("month")
	val month: Int,
	
	@SerialName("day")
	val day: Int,
) : Serializable, Comparable<FullDate> {
	
	companion object {
		
		fun from(
			year: Int?,
			month: Int?,
			day: Int?,
		): FullDate? {
			if (
				year == null ||
				month == null ||
				day == null
			) return null
			
			return FullDate(
				year,
				month,
				day
			)
		}
		
		fun from(millis: Long): FullDate {
			val calendar = GregorianCalendar(TimeZone.getTimeZone(UTC))
			calendar.clear()
			calendar.timeInMillis = millis
			
			return FullDate(
				calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH) + 1,
				calendar.get(Calendar.DAY_OF_MONTH),
			)
		}
		
		
	}
	
	fun toLong(): Long {
		val calendar = GregorianCalendar(TimeZone.getTimeZone(UTC))
		calendar.clear()
		calendar.set(this.year, this.month-1, this.day)
		return calendar.timeInMillis
	}
	
	fun toLocalDate(): LocalDate = LocalDate.of(this.year, this.month, this.day)
	
	override fun compareTo(other: FullDate): Int {
		
		return if (this.year > other.year) 1
		else if (this.year < other.year) -1
		else {
			
			if (this.month > other.month) 1
			else if (this.month < other.month) -1
			else {
				
				if (this.day > other.day) 1
				else if (this.day < other.day) -1
				else 0
			}
		}
	}
	
	
	
}
