package moe.emi.finite.ui.home.model

import moe.emi.finite.service.data.Currency
import moe.emi.finite.service.data.Subscription

data class SubscriptionListUiModel(
	val model: Subscription,
	val preferredCurrency: Currency,
	val convertedAmount: ConvertedAmount,
	val showTimeLeft: Boolean
)
