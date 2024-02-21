package moe.emi.finite.ui.details.model

import moe.emi.finite.service.data.Currency
import moe.emi.finite.service.data.Subscription

data class SubscriptionDetailUiModel(
	val model: Subscription,
	val preferredCurrency: Currency,
	val convertedAmount: Double?,
)
