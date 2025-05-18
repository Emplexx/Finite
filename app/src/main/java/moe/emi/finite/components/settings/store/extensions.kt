package moe.emi.finite.components.settings.store

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import moe.emi.finite.FiniteApp

/**
 * Get a flow of AppSettings in a context
 */
val Context.appSettings: Flow<AppSettings> get() = (applicationContext as FiniteApp).container.settingsStore.data

/**
 * Get a flow of AppSettings in a context, shorthand for fragments
 */
val Fragment.appSettings: Flow<AppSettings> get() = requireContext().appSettings

/**
 * Shorthand for opening a new scope in a [LifecycleOwner], getting the settings data store,
 * and and updating its data.
 */
fun LifecycleOwner.editSettings(block: (AppSettings) -> AppSettings) {
	lifecycleScope.launch {
		FiniteApp.instance.container.settingsStore.updateData(block)
	}
}