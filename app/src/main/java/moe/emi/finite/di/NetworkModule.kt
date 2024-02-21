package moe.emi.finite.di

import moe.emi.finite.service.api.ApiProvider

object NetworkModule {
	
	val selectedApi = ApiProvider.ExchangeRatesApi
	
	private val def by lazy {
		selectedApi.createClient()
	}
	
	fun getRatesApi(): ApiProvider.Impl = def
}