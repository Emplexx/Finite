package moe.emi.finite.ui.details

import moe.emi.finite.components.settings.store.ColorOptions
import moe.emi.finite.core.model.Currency
import moe.emi.finite.core.model.Subscription

data class SubscriptionDetailUiModel(
	val model: Subscription,
	val preferredCurrency: Currency,
	val convertedAmount: Double?,
	val colorOptions: ColorOptions,
)
