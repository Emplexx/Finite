package moe.emi.finite.ui.home.model

import moe.emi.finite.service.api.Rate

data class ConvertedAmount(
	val timeframe: TotalView,
	val amountMatchedToTimeframe: Double,
	val amountOriginal: Double,
	val from: Rate,
	val to: Rate
)