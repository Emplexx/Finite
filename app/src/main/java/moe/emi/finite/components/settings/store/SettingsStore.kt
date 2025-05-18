package moe.emi.finite.components.settings.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import moe.emi.finite.dump.jsonSerializer

private val Context.settingsDataStore: DataStore<AppSettings> by dataStore(
	"settings.json",
	jsonSerializer(AppSettings())
)

fun Context.newSettingsStore(scope: CoroutineScope) = SettingsStore(settingsDataStore, scope)

class SettingsStore(
	private val store: DataStore<AppSettings>,
	scope: CoroutineScope
) : DataStore<AppSettings> by store {
	
	private val flow = data.stateIn(scope, SharingStarted.Eagerly, AppSettings())
	val value get() = flow.value
	
}