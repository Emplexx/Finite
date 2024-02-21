package moe.emi.finite.service.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.emi.finite.FiniteApp
import moe.emi.finite.dump.jsonSerializer


val Context.storedSettings: DataStore<AppSettings> by dataStore("settings.json", jsonSerializer(AppSettings()))

/**
 * Get a flow of AppSettings
 */
val Context.appSettings: Flow<AppSettings> get() = storedSettings.data

/**
 * Get a flow of AppSettings. Shorthand for fragments
 */
val Fragment.appSettings: Flow<AppSettings> get() = requireContext().appSettings

/**
 * Writes the settings object to DataStore through the global context. Async
 */
fun AppSettings.set() {
	FiniteApp.scope.launch {
		FiniteApp.instance.storedSettings.updateData { this@set }
	}
}

/**
 * Shorthand for opening a new scope in a LifecycleOwner, getting the settings object,
 * and calling set on it. If you are already in a CoroutineContext, prefer calling
 * appSettings.first().copy(...).set() directly
 */
fun LifecycleOwner.editSettings(block: (AppSettings) -> AppSettings) {
	lifecycleScope.launch { block(FiniteApp.instance.appSettings.first()).set() }
}