package moe.emi.finite.ui.details.model

import moe.emi.finite.service.model.Currency
import moe.emi.finite.service.model.Subscription

data class SubscriptionDetailUiModel(
	val model: Subscription,
	val preferredCurrency: Currency,
	val convertedAmount: Double?,
)
