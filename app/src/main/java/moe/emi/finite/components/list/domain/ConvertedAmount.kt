package moe.emi.finite.components.list.domain

data class ConvertedAmount(
	val timeframe: TotalView,
	val amountMatchedToTimeframe: Double,
	val amountOriginal: Double,
	val isConverted: Boolean,
) {
	val isMatchedToTimeframe = amountOriginal != amountMatchedToTimeframe
}