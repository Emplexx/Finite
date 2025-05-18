package moe.emi.finite.components.list.domain

import moe.emi.finite.components.settings.store.ColorOptions
import moe.emi.finite.core.model.Currency

data class SubscriptionListUiModel(
	val active: List<SubscriptionItemUiModel>,
	val inactive: List<SubscriptionItemUiModel>,
	val timeframe: TotalView,
	val timeframeAmount: Double,
	val defaultCurrency: Currency,
	val colorOptions: ColorOptions
) {
	val isEmpty get() = active.isEmpty() && inactive.isEmpty()
}