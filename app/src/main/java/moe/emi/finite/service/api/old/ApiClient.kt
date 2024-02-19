package moe.emi.finite.service.api.old

import com.slack.eithernet.ApiResult
import com.slack.eithernet.ApiResultCallAdapterFactory
import com.slack.eithernet.ApiResultConverterFactory
import com.slack.eithernet.DecodeErrorBody
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import moe.emi.finite.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Date

const val baseUrl = "http://api.exchangeratesapi.io/v1/"
const val apiKey = BuildConfig.API_KEY

private val moshi: Moshi = Moshi.Builder()
	.add(Date::class.java, Rfc3339DateJsonAdapter())
	.add(KotlinJsonAdapterFactory())
	.build()

private val logging = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
private val okHttpClient = OkHttpClient.Builder().addInterceptor(logging).build()

val apiClient = Retrofit.Builder()
	.baseUrl(baseUrl)
	.client(okHttpClient)
	.addConverterFactory(ApiResultConverterFactory)
	.addConverterFactory(MoshiConverterFactory.create(moshi))
	.addCallAdapterFactory(ApiResultCallAdapterFactory)
	.build()
	.create<ApiClient>()

interface ApiClient {

	@DecodeErrorBody
	@GET("latest")
	suspend fun getLatestRates(
		@Query("access_key") key: String = apiKey,
	): ApiResult<Latest.Output, ApiError>
	
}