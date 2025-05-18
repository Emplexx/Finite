package moe.emi.finite.components.list

import kotlinx.coroutines.flow.combine
import moe.emi.finite.components.list.domain.Sort
import moe.emi.finite.components.settings.store.AppSettings
import moe.emi.finite.components.settings.store.SettingsStore
import moe.emi.finite.core.db.SubscriptionDao
import moe.emi.finite.core.db.toSubscription
import moe.emi.finite.core.model.Currency
import moe.emi.finite.core.model.Subscription
import moe.emi.finite.core.rates.RatesRepo
import moe.emi.finite.core.rates.model.FetchedRates

fun getFilteredSubscriptions(
	dao: SubscriptionDao,
	settings: SettingsStore,
	ratesRepo: RatesRepo,
) = combine(
	dao.getAllObservable(),
	settings.data,
	ratesRepo.fetchedRates
) { subscriptions, appSettings, rates ->
	subscriptions
		.map { it.toSubscription() }
		.let { filteredSubscriptions(it, appSettings) }
		.let { sortedSubscriptions(it, appSettings, rates) }
}

private fun sortedSubscriptions(
	subscriptions: List<Subscription>,
	appSettings: AppSettings,
	rates: FetchedRates?,
) =
	when (appSettings.sort) {
		Sort.Date -> dateComparator
		Sort.Alphabetical -> alphabeticalComparator
		// TODO relative price comparator
		Sort.Price -> priceComparator(
			rates,
			appSettings.preferredCurrency
		)
	}
		.let { comparator ->
			if (appSettings.sortIsAscending) comparator else comparator.reversed()
		}
		.let(subscriptions::sortedWith)

private fun filteredSubscriptions(
	subscriptions: List<Subscription>,
	appSettings: AppSettings
) =
	if (appSettings.selectedPaymentMethods.isEmpty()) subscriptions
	else {
		val methods = appSettings.selectedPaymentMethods.map { it.trim().lowercase() }
		subscriptions.filter { s -> s.paymentMethod.trim().lowercase() in methods }
	}

// Comparators

val dateComparator = compareBy<Subscription> {
	it.daysUntilNextPayment
}

val alphabeticalComparator = compareBy<Subscription> {
	it.name.lowercase()
}

fun priceComparator(source: FetchedRates?, preferredCurrency: Currency) =
	compareBy<Subscription> {
		source?.convert(it.price, it.currency, preferredCurrency)
	}