package moe.emi.finite.dump

import androidx.lifecycle.SavedStateHandle
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@DslMarker
annotation class SavedStateExtension

@SavedStateExtension
operator fun <T> SavedStateHandle.invoke(default: T) = object : ReadWriteProperty<Any, T> {
	
	override fun getValue(thisRef: Any, property: KProperty<*>): T {
		return this@invoke[property.name] ?: default
	}
	
	override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
		this@invoke[property.name] = value
	}
	
}