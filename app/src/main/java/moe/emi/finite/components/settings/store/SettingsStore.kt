package moe.emi.finite.components.settings.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

fun SettingsStore.editor(owner: LifecycleOwner) = { transform: (AppSettings) -> AppSettings ->
	owner.lifecycleScope.launch {
		this@editor.updateData(transform)
	}
}