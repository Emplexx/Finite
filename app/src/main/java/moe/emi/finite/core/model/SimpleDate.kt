package moe.emi.finite.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import java.io.Serializable as JavaSerializable

@Serializable
data class SimpleDate(
	@SerialName("year")
	val year: Int,
	
	@SerialName("month")
	val month: Int,
	
	@SerialName("day")
	val day: Int,
) : JavaSerializable {
	
	companion object {
		
		fun fromUtcMilliseconds(millis: Long): SimpleDate {
			val calendar = GregorianCalendar(TimeZone.getTimeZone(UTC))
			calendar.clear()
			calendar.timeInMillis = millis
			
			return SimpleDate(
				calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH) + 1,
				calendar.get(Calendar.DAY_OF_MONTH),
			)
		}
		
	}
	
	fun toUtcMilliseconds(): Long {
		val calendar = GregorianCalendar(TimeZone.getTimeZone(UTC))
		calendar.clear()
		calendar.set(this.year, this.month-1, this.day)
		return calendar.timeInMillis
	}
	
	fun toLocalDate(): LocalDate = LocalDate.of(this.year, this.month, this.day)

}
