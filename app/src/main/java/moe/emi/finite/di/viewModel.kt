@file:Suppress("UNCHECKED_CAST")

package moe.emi.finite.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras

val CreationExtras.app: Application get() = this[APPLICATION_KEY]!!
val CreationExtras.ssh: SavedStateHandle get() = createSavedStateHandle()

typealias VMFactory = ViewModelProvider.Factory

fun <T> singleViewModel(
	create: CreationExtras.() -> T
): VMFactory {
	return object : VMFactory {
		override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
			return create(extras) as T
		}
	}
}