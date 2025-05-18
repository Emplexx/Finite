package moe.emi.finite.components.settings.store

import kotlinx.serialization.Serializable
import moe.emi.finite.components.list.domain.Sort
import moe.emi.finite.core.model.Currency

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
)

data class ColorOptions(
	val harmonizeColors: Boolean,
	val normalizeColors: Boolean,
	val normalizeFactor: Int,
)

val AppSettings.colorOptions get() = ColorOptions(harmonizeColors, normalizeColors, normalizeFactor)
