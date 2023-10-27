package moe.emi.finite.service.datastore

import kotlinx.serialization.Serializable
import moe.emi.finite.dump.Storable
import moe.emi.finite.dump.storableKey
import moe.emi.finite.service.data.Currency
import moe.emi.finite.ui.home.Sort

@Serializable
data class AppSettings(
	val penis: Int = 0,
	
	// View options
	val sort: Sort = Sort.Date,
	val sortIsAscending: Boolean = true,
	
	// Settings
	val preferredCurrency: Currency = Currency.EUR,
	val harmonizeColors: Boolean = true,
	val normalizeColors: Boolean = false,
	val normalizeFactor: Int = 0,
) {
	companion object : Storable<AppSettings> by storableKey("AppSettings")
}