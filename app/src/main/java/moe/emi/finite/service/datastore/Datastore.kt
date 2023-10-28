package moe.emi.finite.service.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import moe.emi.finite.FiniteApp
import moe.emi.finite.dump.DataStoreExt.read
import moe.emi.finite.dump.DataStoreExt.write
import moe.emi.finite.service.data.Subscription

val Context.storeGeneral: DataStore<Preferences> by preferencesDataStore("General")

object Keys {
	// App stuff
	val RatesLastUpdated = longPreferencesKey("ratesLastUpdated")
	
	val Draft = stringPreferencesKey("draft")
}

suspend fun setDraft(draft: Subscription) {
	FiniteApp.instance.storeGeneral.write(Keys.Draft, Json.encodeToString(draft))
}

fun getDraft(): Flow<Subscription?> {
	return FiniteApp.instance.storeGeneral.read(Keys.Draft).map { encoded ->
		encoded?.let { Json.decodeFromString(it) }
	}
}

suspend fun clearDraft() {
	FiniteApp.instance.storeGeneral.edit { it.remove(Keys.Draft) }
}
