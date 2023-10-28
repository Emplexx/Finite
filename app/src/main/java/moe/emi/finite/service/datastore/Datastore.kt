package moe.emi.finite.service.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.storeGeneral: DataStore<Preferences> by preferencesDataStore("General")

object Keys {
	// App stuff
	val RatesLastUpdated = longPreferencesKey("ratesLastUpdated")
}

