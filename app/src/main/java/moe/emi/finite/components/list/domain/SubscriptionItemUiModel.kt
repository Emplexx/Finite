package moe.emi.finite.components.list.domain

import moe.emi.finite.core.model.Currency
import moe.emi.finite.core.model.Subscription

data class SubscriptionItemUiModel(
	val model: Subscription,
	val preferredCurrency: Currency,
	val convertedAmount: ConvertedAmount,
	val showTimeLeft: Boolean
)
