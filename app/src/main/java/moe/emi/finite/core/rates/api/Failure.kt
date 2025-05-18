package moe.emi.finite.core.rates.api

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import fuel.Fuel
import fuel.HttpResponse
import fuel.get
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import java.net.UnknownHostException

interface Failure

data object Unknown : Failure
data object Network : Failure
data object Deserialization : Failure

suspend fun fuelGet(url: String): Either<Failure, HttpResponse> = either {
	runCatching { Fuel.get(url) }
		.onFailure {
			it.printStackTrace()
			if (it is UnknownHostException) raise(Network)
		}
		.getOrElse { raise(Unknown) }
}

context (Raise<Failure>)
inline fun <reified T> Json.deserialize(string: String) =
	runCatching { this.decodeFromString<T>(string) }.getOrElse { raise(Deserialization) }

context (Raise<Failure>)
inline fun <reified T> Json.decode(serializer: DeserializationStrategy<T>, string: String) =
	runCatching { this.decodeFromString(serializer, string) }.getOrElse { raise(Deserialization) }