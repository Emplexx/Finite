package moe.emi.finite

import kotlinx.serialization.json.Json

typealias IOSerializable = java.io.Serializable

val jsonApi = Json {
	ignoreUnknownKeys = true
}