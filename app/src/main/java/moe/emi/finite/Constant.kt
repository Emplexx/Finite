package moe.emi.finite

import kotlinx.serialization.json.Json

typealias IOSerializable = java.io.Serializable

object Constant {
	const val RatesUpdateInterval = 2_628_002L // 1 Month
}

val jsonApi = Json {
	ignoreUnknownKeys = true
}