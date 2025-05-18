package moe.emi.finite.components.list

import kotlinx.coroutines.flow.map
import moe.emi.finite.core.db.SubscriptionDao

fun getAllPaymentMethods(
	repo: SubscriptionDao
) = repo.getAllObservable().map { list ->
	list.map { it.paymentMethod.trim() }
		.filter { it.isNotBlank() }
		.associateBy { it.lowercase() }
		.values
		.toSet()
}