package moe.emi.finite.service.datastore

import kotlinx.serialization.Serializable
import moe.emi.finite.dump.Storable
import moe.emi.finite.dump.storableKey
import moe.emi.finite.service.data.Currency
import moe.emi.finite.ui.home.model.Sort

@Serializable
data class AppSettings(
	val penis: Int = 0,
	
	// View options
	val sort: Sort = Sort.Date,
	val sortIsAscending: Boolean = true,
	val showTimeLeft: Boolean = true,
	val showRoughlySign: Boolean = true,
	val selectedPaymentMethods: Set<String> = emptySet(),
	
	// Settings
	val preferredCurrency: Currency = Currency.EUR,
	val appTheme: AppTheme = AppTheme.Unspecified,
	val harmonizeColors: Boolean = true,
	val normalizeColors: Boolean = false,
	val normalizeFactor: Int = 0,
) {
	companion object : Storable<AppSettings> by storableKey("AppSettings")
}

enum class AppTheme {
	Unspecified, Light, Dark
}