package moe.emi.finite.core.model

import kotlinx.serialization.Serializable
import moe.emi.finite.core.findNextPayment
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.io.Serializable as JavaSerializable
import java.time.Period as JavaPeriod

@Serializable
data class Subscription(
	
	val id: Int = 0,
	
	val name: String = "",
	val description: String = "",
	val color: Int? = null,
	
	val price: Double = 0.0,
	val currency: Currency,
	
	// TODO make this field non-null, as this is an invalid domain model
	val startedOn: SimpleDate? = null,
//	val period: BillingPeriod = BillingPeriod(1, Timespan.Month),
	val period: Period = Period(1, Timespan.Month),
	
	val paymentMethod: String = "",
	
	val notes: String = "",
	
	val isActive: Boolean = true
	
) : JavaSerializable {
	
	fun findNextPayment(): LocalDate? {
		startedOn ?: return null
		val date = LocalDate.of(startedOn.year, startedOn.month, startedOn.day)
		return date.findNextPayment(period)
	}
	
	fun periodUntilNextPayment(): JavaPeriod? =
		findNextPayment()?.let { JavaPeriod.between(LocalDate.now(), it) }
	
	val daysUntilNextPayment: Long?
		get() = findNextPayment()?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
	
	val isDraft = id == 0
	
}

