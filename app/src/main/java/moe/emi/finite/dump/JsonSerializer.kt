package moe.emi.finite.dump

import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

inline fun <reified T : Any?> jsonSerializer(default: T) = object : Serializer<T> {
	
	override val defaultValue: T get() = default
	
	private val json = Json
	
	override suspend fun readFrom(input: InputStream): T {
		return try {
			withContext(Dispatchers.IO) {
				json.decodeFromString(input.readBytes().decodeToString())
			}
		}
		catch (e: SerializationException) {
			e.printStackTrace()
			defaultValue
		}
	}
	
	override suspend fun writeTo(t: T, output: OutputStream) {
		withContext(Dispatchers.IO) {
			output.write(
				json.encodeToString(t).encodeToByteArray()
			)
		}
	}
	
}