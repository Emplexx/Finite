package moe.emi.finite.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface Encoded<T, K> {
	val backing: Preferences.Key<K>
	fun encode(from: T): K
	fun decode(from: K?): T
}

inline fun <T, K> Preferences.Key<K>.encoded(
	crossinline encode: (T) -> K,
	crossinline decode: (K?) -> T,
) = object : Encoded<T, K> {
	override val backing: Preferences.Key<K> = this@encoded
	override fun decode(from: K?): T = decode(from)
	override fun encode(from: T): K = encode(from)
}

private typealias DS = DataStore<Preferences>

operator fun <T> DS.get(key: Preferences.Key<T>): Flow<T?> {
	return data.map { it[key] }
}

operator fun <T, K> DS.get(key: Encoded<T, K>): Flow<T> {
	return data.map {
		it[key.backing].let { key.decode(it) }
	}
}

suspend fun <T, K> DS.set(key: Encoded<T, K>, value: T) {
	this.edit {
		it[key.backing] = key.encode(value)
	}
}