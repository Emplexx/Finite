package moe.emi.finite.service.data

data class Rate(
	val code: String,
	val rate: Double
) {
	companion object {
		val EUR = Rate("EUR", 1.0)
		
		fun List<Rate>.get(currency: Currency) =
			this.find { it.code == currency.iso4217Alpha }
	}
}
