package moe.emi.finite.service.api

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import moe.emi.finite.service.data.Rates
import java.util.AbstractMap
import java.util.Date

object Latest {
	
	@JsonClass(generateAdapter = true)
	data class Output(
		val base: String,
		val timestamp: Long,
		private val rates: Any?
	) {
		val listRates: List<Rate>
			get() = kotlin.runCatching {
				rates
					.let { it as AbstractMap<*, *> }
					.map {
						Rate(it.key.toString(), it.value as Double)
					}
			}.getOrDefault(emptyList())
	}
	
	data class Rate(
		val code: String,
		val rate: Double,
	)
	
}

data class RateList(
	val rates: List<Latest.Rate>
)

//object RateListAdapter : JsonAdapter<RateList>() {
//
//	override fun fromJson(reader: JsonReader): RateList? {
//		reader.nextString()
//	}
//
//
//	@FromJson
//	fun fromJson(json: String): RateList {
//		val moshi = Moshi.Builder().build()
//		val jsonObject = JSONObject(json)
//		val type = jsonObject.getString("type")
//		val adapter = when (type) {
//			"artist" -> {
//				moshi.adapter(ArtistAttributes::class.java)
//			}
//			"cover_art" -> {
//				moshi.adapter(CoverArtAttributes::class.java)
//			}
//			else -> throw IllegalArgumentException("unhandled type")
//		}
//		return adapter.fromJson(json)
//	}
//
//	override fun toJson(writer: JsonWriter, value: RateList?) {
//		TODO("Not yet implemented")
//	}
//}