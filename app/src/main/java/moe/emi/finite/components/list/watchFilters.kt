package moe.emi.finite.components.list

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import moe.emi.finite.components.settings.store.SettingsStore
import moe.emi.finite.core.db.SubscriptionDao

suspend fun watchFilters(
	settingsStore: SettingsStore,
	dao: SubscriptionDao,
) = combine(
	getAllPaymentMethods(dao).map { it.map(String::lowercase) },
	settingsStore.data
) { paymentMethods, settings ->
	val deleted = settings.selectedPaymentMethods.filter { it.lowercase() !in paymentMethods }
	if (deleted.isEmpty()) return@combine
	
	settingsStore.updateData {
		it.copy(selectedPaymentMethods = settings.selectedPaymentMethods - deleted.toSet())
	}
}.collect()