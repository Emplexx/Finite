package moe.emi.finite.service.data

import kotlinx.serialization.Serializable
import moe.emi.finite.service.api.FetchedRates
import moe.emi.finite.service.db.SubscriptionEntity
import moe.emi.finite.ui.details.NotificationPeriod
import moe.emi.finite.ui.settings.backup.SubscriptionBackup
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import java.io.Serializable as IOSerializable

@Serializable
data class Subscription(
	
	val id: Int = 0,
	
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
	
) : IOSerializable {
	
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
			val r = when (period.timespan) {
				Timespan.Day -> this.plusDays(period.count.toLong())
				Timespan.Week -> this.plusWeeks(period.count.toLong())
				Timespan.Month -> this.plusMonths(period.count.toLong())
				Timespan.Year -> this.plusYears(period.count.toLong())
			}
//			Log.d("LocalDate.plus", "local date $this")
//			Log.d("LocalDate.plus", "period $period")
//			Log.d("LocalDate.plus", "result $r")
			return r
		}
		
		fun LocalDate.minus(period: NotificationPeriod): LocalDate =
			when (period.timespan) {
				Timespan.Day -> this.minusDays(period.count.toLong())
				Timespan.Week -> this.minusWeeks(period.count.toLong())
				Timespan.Month -> this.minusMonths(period.count.toLong())
				Timespan.Year -> this.minusYears(period.count.toLong())
			}
		
		tailrec fun LocalDate.findNextPayment(period: BillingPeriod): LocalDate =
			if (this > LocalDate.now()) this else (this.plus(period)).findNextPayment(period)
		
		tailrec fun LocalDate.findNextPaymentInclusive(period: BillingPeriod): LocalDate =
			if (this >= LocalDate.now()) this else (this.plus(period)).findNextPaymentInclusive(period)
	
		val dateComparator = compareBy<Subscription> {
			it.daysUntilNextPayment
		}
		val alphabeticalComparator = compareBy<Subscription> {
			it.name.lowercase()
		}
		fun priceComparator(source: FetchedRates?, preferredCurrency: Currency) = compareBy<Subscription> {
			source?.convert(it.price, it.currency, preferredCurrency)
		}
		
		
		fun from(it: SubscriptionBackup) = it.run {
			Subscription(
				id, name, description, color, price, currency, startedOn,
				BillingPeriod(period.length, period.unit),
				paymentMethod, notes, active
			)
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
