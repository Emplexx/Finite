package moe.emi.finite.dump.android

import androidx.lifecycle.SavedStateHandle
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@DslMarker
annotation class SavedStateExtension

@SavedStateExtension
operator fun <T> SavedStateHandle.invoke(default: T, key: String? = null) = object : ReadWriteProperty<Any, T> {
	
	override fun getValue(thisRef: Any, property: KProperty<*>): T {
		return this@invoke[key ?: property.name] ?: default
	}
	
	override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
		this@invoke[key ?: property.name] = value
	}
	
}