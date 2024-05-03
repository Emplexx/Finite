package moe.emi.finite.ui.home.model

import moe.emi.finite.service.model.Currency
import moe.emi.finite.service.model.Subscription

data class SubscriptionListUiModel(
	val model: Subscription,
	val preferredCurrency: Currency,
	val convertedAmount: ConvertedAmount,
	val showTimeLeft: Boolean
)
