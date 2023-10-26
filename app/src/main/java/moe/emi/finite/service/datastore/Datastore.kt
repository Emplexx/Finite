package moe.emi.finite.service.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import moe.emi.finite.dump.getStorable
import moe.emi.finite.service.data.Currency

typealias Prefs = DataStore<Preferences>

val Context.storeGeneral: Prefs by preferencesDataStore("General")

val Context.appSettings: Flow<AppSettings>
	get() = this.storeGeneral.getStorable<AppSettings>()

val Context.preferredCurrency: Flow<Currency>
	get() = appSettings.map { it.preferredCurrency }

object Keys {
	// App stuff
	val RatesLastUpdated = longPreferencesKey("ratesLastUpdated")
	
	// User preferences
	
}

