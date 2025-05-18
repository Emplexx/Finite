package moe.emi.finite.core.preferences

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import moe.emi.finite.core.model.Currency
import moe.emi.finite.core.rates.api.ApiProvider

object Pref {
	val currencyHistory = stringSetPreferencesKey("currencyHistory").encoded(
		encode = {
			it.map(Currency::iso4217Alpha).take(5).toSet()
		},
		decode = {
			val set = it ?: setOf("EUR", "USD")
			set.mapNotNull(Currency.Companion::ofIsoA3Code).take(5)
		}
	)
	
	val preferredRatesProvider = intPreferencesKey("apiProvider").encoded(
		encode = { it.id },
		decode = { id -> ApiProvider.entries.find { it.id == id } ?: ApiProvider.default }
	)
	val openExchangeRatesAppId = stringPreferencesKey("openExchangeRatesAppId")
	
	val proLastSeenAt = longPreferencesKey("proLastSeenAt").encoded(
		encode = { it.toEpochMilli() },
		decode = { java.time.Instant.ofEpochMilli(it ?: 0L)!! }
	)
}