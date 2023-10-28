package moe.emi.finite.service.datastore

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import moe.emi.finite.FiniteApp
import moe.emi.finite.dump.getStorable
import moe.emi.finite.dump.setStorable

/**
 * Get a flow of AppSettings
 */
val Context.appSettings: Flow<AppSettings>
	get() = this.storeGeneral.getStorable()

/**
 * Get a flow of AppSettings. Shorthand for fragments
 */
val Fragment.appSettings: Flow<AppSettings>
	get() = requireContext().appSettings

/**
 * Shorthand for getting the latest state of settings through the global context.
 * Async
 */
suspend fun getSettings() = FiniteApp.instance.appSettings.first()

/**
 * Writes the settings object to DataStore through the global context. Async
 */
suspend fun AppSettings.set() {
	FiniteApp.instance.storeGeneral.setStorable(this)
}

context(LifecycleOwner)
fun AppSettings.setAsync() {
	lifecycleScope.launch(Dispatchers.IO) { FiniteApp.instance.storeGeneral.setStorable(this@setAsync) }
}

/**
 * Shorthand for opening a new scope in a LifecycleOwner, getting the settings object,
 * and calling set on it. If you are already in a CoroutineContext, prefer calling
 * appSettings.first().copy(...).set() directly
 */
fun LifecycleOwner.editSettings(block: (AppSettings) -> AppSettings) {
	lifecycleScope.launch { block(FiniteApp.instance.appSettings.first()).set() }
}

/**
 * Shorthand for getting the latest state of settings through the global context.
 * Sync (Note: See deprecation reason)
 */
@Deprecated("Results in fairly noticeable app hang")
val settingsSync: AppSettings
	get() = runBlocking { FiniteApp.instance.appSettings.first() }