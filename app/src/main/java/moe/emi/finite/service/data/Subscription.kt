package moe.emi.finite.service.data

import moe.emi.finite.service.db.SubscriptionEntity
import java.io.Serializable
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

@kotlinx.serialization.Serializable
data class Subscription(
	
	val name: String = "",
	val description: String = "",
	val color: Int? = null,
	
	val price: Double = 0.0,
	val currency: Currency,
	
	val startedOn: FullDate? = null,
	val period: BillingPeriod = BillingPeriod(1, Timespan.Month),
	
	val paymentMethod: String = "",
	
	val notes: String = "",
	
	val active: Boolean = true,
	
	val id: Int = 0,
) : Serializable {
	
	constructor(entity: SubscriptionEntity) : this(
		id = entity.id,
		
		name = entity.name,
		description = entity.description,
		color = entity.color,
		
		price = entity.amount,
		currency = entity.currency,
		
		startedOn = entity.startedOn?.let { FullDate.from(it) },
		period = BillingPeriod(entity.periodAmount, entity.periodTimespan),
		paymentMethod = entity.paymentMethod,
		notes = entity.notes,
		active = entity.active,
	)
	
	companion object {
		fun LocalDate.plus(period: BillingPeriod): LocalDate {
			val r = when (period.unit) {
				Timespan.Day -> this.plusDays(period.every.toLong())
				Timespan.Week -> this.plusWeeks(period.every.toLong())
				Timespan.Month -> this.plusMonths(period.every.toLong())
				Timespan.Year -> this.plusYears(period.every.toLong())
			}
//			Log.d("LocalDate.plus", "local date $this")
//			Log.d("LocalDate.plus", "period $period")
//			Log.d("LocalDate.plus", "result $r")
			return r
		}
		
		tailrec fun LocalDate.findNextPayment(period: BillingPeriod): LocalDate =
			if (this > LocalDate.now()) this else (this.plus(period)).findNextPayment(period)
	
		val dateComparator = compareBy<Subscription> {
			it.daysUntilNextPayment
		}
		val alphabeticalComparator = compareBy<Subscription> {
			it.name.lowercase()
		}
		fun priceComparator(from: (Currency) -> Rate, to: Rate) = compareBy<Subscription> {
			convert(it.price, from(it.currency), to)
		}
	}
	
	fun findNextPayment(): LocalDate? {
		startedOn ?: return null
		val date = LocalDate.of(startedOn.year, startedOn.month, startedOn.day)
		
//		val now = LocalDate.now()
//		var workDate = date
//		while (workDate < now) {
//			workDate = workDate.plus(period)
//		}
		
		return date.findNextPayment(period)
		
	}
	
	fun periodUntilNextPayment(): Period? =
		findNextPayment()?.let { Period.between(LocalDate.now(), it) }
	
	val daysUntilNextPayment: Long?
		get() = findNextPayment()
			?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
	
}
