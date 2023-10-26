package moe.emi.finite.service.data

import kotlinx.coroutines.flow.first
import moe.emi.finite.dump.safe
import moe.emi.finite.service.data.Rate.Companion.get
import moe.emi.finite.service.repo.RatesRepo

fun convert(price: Double, from: Rate, to: Rate): Double =
	price / from.rate * to.rate

suspend fun convert(price: Double, from: Currency, to: Currency): Double? {
	val rates = RatesRepo.getLocalRates().first()
	return safe(rates.get(from), rates.get(to)) { rateFrom, rateTo ->
		convert(price, rateFrom, rateTo)
	}
}