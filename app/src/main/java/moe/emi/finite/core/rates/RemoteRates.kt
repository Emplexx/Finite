package moe.emi.finite.core.rates

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.msoul.datastore.getEnum
import moe.emi.finite.core.rates.api.ApiProvider
import moe.emi.finite.core.rates.api.impl.ExchangeRatesApi
import moe.emi.finite.core.rates.api.impl.InforEuro
import moe.emi.finite.core.rates.api.impl.OpenExchangeRates
import moe.emi.finite.core.preferences.Pref.openExchangeRatesAppId
import moe.emi.finite.core.preferences.Pref.preferredRatesProvider
import moe.emi.finite.core.preferences.get

class RemoteRates(private val dataStore: DataStore<Preferences>) {
	
	val provider = dataStore[preferredRatesProvider].map { it.create() }
	
	private suspend fun ApiProvider.create(): ApiProvider.Impl = with (dataStore) {
		
		when (this@create) {
			ApiProvider.InforEuro -> InforEuro()
			ApiProvider.ExchangeRatesApi -> ExchangeRatesApi()
			
			ApiProvider.OpenExchangeRates -> object : OpenExchangeRates.Options {
				override suspend fun getAppId(): String? {
					return this@with[openExchangeRatesAppId].first()
				}
				
				override suspend fun getUpdateFrequency(): OpenExchangeRates.UpdateFrequency {
					return getEnum<OpenExchangeRates.UpdateFrequency>().first()
				}
				
			}.let { OpenExchangeRates(it) }
		}
	}
}