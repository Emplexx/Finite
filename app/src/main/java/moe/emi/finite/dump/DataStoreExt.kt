package moe.emi.finite.dump

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object DataStoreExt {

	
	/**
	 * Read key as [Flow] from given Preferences Data Store
	 * @param key Key of preference to read
	 * @param default Value to return if the value of key is null
	 * @return [Flow] with value of key or default value provided if null found. This function is not type safe
	 */
	fun <T> DataStore<Preferences>.read(key: Preferences.Key<T>, default: T): Flow<T> {
		return this.data
			.map {
				it[key] ?: default
			}
	}
	
	/**
	 * Read key as [Flow] from given Preferences Data Store
	 * @param key Key of preference to read
	 * @return [Flow] with value of key or null if not found. This function is not type safe
	 */
	fun <T> DataStore<Preferences>.read(key: Preferences.Key<T>): Flow<T?> {
		return this.data
			.map {
				it[key]
			}
	}
	
	/**
	 * Read key from given Preferences Data Store
	 * @param key Key of preference to read
	 * @return Value of key or null if not found. This function is not type safe
	 */
	suspend fun <T> DataStore<Preferences>.readFirst(key: Preferences.Key<T>): T? {
		return this.data.first()[key]
	}
	
	/**
	 * Read key from given Preferences Data Store
	 * @param key Key of preference to read
	 * @return Value of key or null if not found. This function is not type safe
	 */
	suspend fun <T> DataStore<Preferences>.readFirst(key: Preferences.Key<T>, default: T): T {
		return this.data.first()[key] ?: default
	}
	
	/**
	 * Write value to DataStore
	 * @param key Key of preference to write
	 * @param value Value to write
	 */
	suspend fun <T> DataStore<Preferences>.write(key: Preferences.Key<T>, value: T) {
		this.edit {
			it[key] = value
		}
	}
	
	/**
	 * Clear DataStore
	 */
	suspend fun DataStore<Preferences>.clear() {
		this.edit {
			it.clear()
		}
	}
	
}