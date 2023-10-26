package moe.emi.finite.service.datastore

import kotlinx.serialization.Serializable
import moe.emi.finite.dump.Storable
import moe.emi.finite.dump.storableKey
import moe.emi.finite.service.data.Currency

@Serializable
data class AppSettings(
	val penis: Int = 0,
	val preferredCurrency: Currency = Currency.EUR,
	val harmonizeColors: Boolean = true,
	val normalizeColors: Boolean = false,
	val normalizeFactor: Int = 0,
) {
	companion object : Storable<AppSettings> by storableKey("AppSettings")
}