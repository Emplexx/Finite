package moe.emi.finite.dump

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import moe.emi.finite.dump.DataStoreExt.read
import moe.emi.finite.dump.DataStoreExt.write
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.createInstance

// JSON Storable

interface Storable<T> {
	val key: String
	val default: T?
}

fun <T> storableKey(key: String, default: T? = null) = object : Storable<T> {
	override val default = default
	override val key = key
}

val Storable<*>.dataStoreKey: Preferences.Key<String>
	get() = stringPreferencesKey(this.key)

inline fun <reified T : Any> DataStore<Preferences>.getStorable(): Flow<T> {
	
	
	val obj = T::class.companionObjectInstance
	
	if (obj !is Storable<*>) error("${T::class.simpleName} must have a companion object that implements Storable<${T::class.simpleName}>")
	
	return this.read(obj.dataStoreKey).map {
		
		if (it == null) (obj.default ?: T::class.createInstance()) as T
		else Json.decodeFromString(it)
	}
}

suspend inline fun <reified T> DataStore<Preferences>.setStorable(value: T) /*where T : SerializationStrategy<T>*/ {
	val obj = T::class.companionObjectInstance
	
	if (obj !is Storable<*>) error("${T::class.simpleName} must have a companion object that implements Storable<${T::class.simpleName}>")
	
	this.write(obj.dataStoreKey, Json.encodeToString(value))
}



